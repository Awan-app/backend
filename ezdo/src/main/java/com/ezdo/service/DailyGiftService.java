package com.ezdo.service;

import com.ezdo.config.WheelSegment;
import com.ezdo.dto.gamification.WheelClaimResponse;
import com.ezdo.dto.gamification.WheelConfigResponse;
import com.ezdo.dto.gamification.WheelSegmentResponse;
import com.ezdo.dto.gamification.WheelSpinResponse;
import com.ezdo.dto.store.ItemResponse;
import com.ezdo.entity.DailyGiftClaim;
import com.ezdo.entity.User;
import com.ezdo.exception.DailyGiftAlreadyClaimedException;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.mapper.StoreMapper;
import com.ezdo.repository.DailyGiftClaimRepository;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Daily gift (spin-the-wheel) orchestration: exposes the wheel config and
 * handles a single spin per user per day. Idempotency is enforced both by a
 * pre-check and by the {@code (user_id, claim_date)} unique constraint, which
 * catches the race between two concurrent spins.
 */
@Service
@RequiredArgsConstructor
public class DailyGiftService {

    private final UserRepository userRepository;
    private final DailyGiftClaimRepository dailyGiftClaimRepository;
    private final WheelSpinService wheelSpinService;
    private final UserClockService userClockService;
    private final StoreMapper storeMapper;

    @Transactional(readOnly = true)
    public WheelConfigResponse getConfig(UUID userId) {
        User user = findUser(userId);
        LocalDate today = userClockService.today(user);

        List<WheelSegmentResponse> segments = Arrays.stream(WheelSegment.values())
            .map(segment -> new WheelSegmentResponse(
                segment.name(),
                segment.coins(),
                segment.payoutType()))
            .toList();

        boolean claimedToday = dailyGiftClaimRepository.existsByUserIdAndClaimDate(userId, today);

        WheelClaimResponse lastClaim = dailyGiftClaimRepository
            .findTopByUserIdOrderByClaimDateDesc(userId)
            .map(this::toClaimResponse)
            .orElse(null);

        return new WheelConfigResponse(segments, claimedToday, lastClaim);
    }

    @Transactional
    public WheelSpinResponse spin(UUID userId) {
        User user = findUser(userId);
        LocalDate today = userClockService.today(user);

        if (dailyGiftClaimRepository.existsByUserIdAndClaimDate(userId, today)) {
            throw new DailyGiftAlreadyClaimedException(today);
        }

        WheelSpinService.SpinOutcome outcome = wheelSpinService.resolve(user);

        DailyGiftClaim claim = DailyGiftClaim.builder()
            .user(user)
            .claimDate(today)
            .segmentId(outcome.segment().name())
            .coinsAwarded(outcome.coins())
            .itemId(outcome.item() != null ? outcome.item().getId() : null)
            .itemName(outcome.item() != null ? outcome.item().getName() : null)
            .claimedAt(Instant.now())
            .build();

        // The claim is inserted and flushed before any payout is applied, so this
        // flush can only fail on uk_daily_gift_user_date — a concurrent spin that
        // claimed (userId, today) between the pre-check and here.
        try {
            dailyGiftClaimRepository.saveAndFlush(claim);
        } catch (DataIntegrityViolationException e) {
            throw new DailyGiftAlreadyClaimedException(today);
        }

        long newBalance = wheelSpinService.applyPayout(user, outcome);

        ItemResponse item = outcome.item() != null
            ? storeMapper.toItemResponse(outcome.item())
            : null;

        return new WheelSpinResponse(
            outcome.segment().name(),
            outcome.payoutType(),
            outcome.coins(),
            newBalance,
            item
        );
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private WheelClaimResponse toClaimResponse(DailyGiftClaim claim) {
        return new WheelClaimResponse(
            claim.getSegmentId(),
            claim.getCoinsAwarded(),
            claim.getItemId(),
            claim.getItemName(),
            claim.getClaimDate(),
            claim.getClaimedAt()
        );
    }
}
