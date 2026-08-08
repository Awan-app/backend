package com.ezdo.service;

import com.ezdo.config.PayoutType;
import com.ezdo.config.WheelSegment;
import com.ezdo.entity.Item;
import com.ezdo.entity.User;
import com.ezdo.entity.UserItem;
import com.ezdo.repository.ItemRepository;
import com.ezdo.repository.UserItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Resolves a single wheel spin into its payout, in two phases so the caller can
 * persist the idempotency record between them: {@link #resolve(User)} decides
 * the outcome without writing anything, {@link #applyPayout(User, SpinOutcome)}
 * commits it.
 *
 * <p>The {@link WheelSegment#SEG_ITEM} segment grants a random item the user
 * does not already own. When every item is owned the spin resolves to
 * {@link #FALLBACK_SEGMENT} instead, so the outcome is always a segment the
 * client was shown in the wheel config.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WheelSpinService {

    /** Substituted for the item segment when the user already owns every item. */
    private static final WheelSegment FALLBACK_SEGMENT = WheelSegment.SEG_4;

    private final WalletService walletService;
    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;

    @Transactional(readOnly = true)
    public SpinOutcome resolve(User user) {
        WheelSegment segment = WheelSegment.pick(ThreadLocalRandom.current());

        if (segment.payoutType() != PayoutType.ITEM) {
            return new SpinOutcome(segment, PayoutType.COINS, segment.coins(), null);
        }

        List<Item> unowned = itemRepository.findUnownedBy(user);
        if (unowned.isEmpty()) {
            log.info("User {} hit the item segment but owns every item; resolving to {}",
                user.getId(), FALLBACK_SEGMENT.name());
            return new SpinOutcome(FALLBACK_SEGMENT, PayoutType.COINS, FALLBACK_SEGMENT.coins(), null);
        }

        Item item = unowned.get(ThreadLocalRandom.current().nextInt(unowned.size()));
        return new SpinOutcome(segment, PayoutType.ITEM, 0, item);
    }

    @Transactional
    public long applyPayout(User user, SpinOutcome outcome) {
        if (outcome.payoutType() == PayoutType.ITEM) {
            userItemRepository.save(UserItem.builder()
                .user(user)
                .item(outcome.item())
                .boughtAt(Instant.now())
                .build());
            log.info("User {} won item {} on the daily wheel (segment {})",
                user.getId(), outcome.item().getId(), outcome.segment().name());
        } else {
            walletService.credit(user, outcome.coins());
            log.info("User {} won {} coins on the daily wheel (segment {})",
                user.getId(), outcome.coins(), outcome.segment().name());
        }

        return user.getWallet().getPoints();
    }

    /**
     * A decided but not yet applied spin. {@code payoutType} describes what is
     * actually paid out, which is not always {@code segment.payoutType()} — the
     * item segment resolves to coins when the user owns everything.
     */
    public record SpinOutcome(
        WheelSegment segment,
        PayoutType payoutType,
        int coins,
        Item item
    ) {}
}
