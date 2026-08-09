package com.ezdo.service;

import com.ezdo.dto.store.EquippedItemResponse;
import com.ezdo.dto.store.ItemResponse;
import com.ezdo.dto.store.UserItemResponse;
import com.ezdo.entity.Item;
import com.ezdo.entity.ItemType;
import com.ezdo.entity.User;
import com.ezdo.entity.UserEquippedItem;
import com.ezdo.entity.UserItem;
import com.ezdo.entity.Wallet;
import com.ezdo.exception.InsufficientPointsException;
import com.ezdo.exception.ItemAlreadyOwnedException;
import com.ezdo.exception.ItemNotFoundException;
import com.ezdo.exception.ItemNotOwnedException;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.mapper.StoreMapper;
import com.ezdo.repository.ItemRepository;
import com.ezdo.repository.UserEquippedItemRepository;
import com.ezdo.repository.UserItemRepository;
import com.ezdo.repository.UserRepository;
import com.ezdo.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {

    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final UserEquippedItemRepository userEquippedItemRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final StoreMapper storeMapper;

    @Transactional(readOnly = true)
    public List<ItemResponse> getAllItems(com.ezdo.entity.ItemType type) {
        List<Item> items = (type != null) ? itemRepository.findByType(type) : itemRepository.findAll();
        return items.stream()
                .map(storeMapper::toItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserItemResponse> getUserInventory(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return userItemRepository.findByUser(user).stream()
                .map(storeMapper::toUserItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserItemResponse buyItem(UUID userId, UUID itemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        if (userItemRepository.existsByUserAndItem(user, item)) {
            throw new ItemAlreadyOwnedException(itemId);
        }

        Wallet wallet = user.getWallet();

        if (wallet.getPoints() < item.getPrice()) {
            throw new InsufficientPointsException((int) wallet.getPoints(), item.getPrice());
        }

        // Deduct points (optimistic locking applies via @Version on Wallet)
        wallet.setPoints(wallet.getPoints() - item.getPrice());
        walletRepository.save(wallet);

        // Add item to user's inventory
        UserItem userItem = UserItem.builder()
                .user(user)
                .item(item)
                .boughtAt(Instant.now())
                .build();
        
        userItem = userItemRepository.save(userItem);

        log.info("User {} bought item {} for {} points", userId, itemId, item.getPrice());

        return storeMapper.toUserItemResponse(userItem);
    }

    @Transactional(readOnly = true)
    public List<EquippedItemResponse> getEquipped(UUID userId) {
        return userEquippedItemRepository.findByUserIdOrderByItemType(userId).stream()
                .map(storeMapper::toEquippedItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EquippedItemResponse equip(UUID userId, UUID itemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        if (!userItemRepository.existsByUserAndItem(user, item)) {
            throw new ItemNotOwnedException(itemId);
        }

        ItemType type = item.getType();

        var existing = userEquippedItemRepository.findByUserIdAndItemType(userId, type);
        if (existing.isPresent()) {
            UserEquippedItem slot = existing.get();
            if (!slot.getItem().getId().equals(itemId)) {
                slot.setItem(item);
                slot.setEquippedAt(Instant.now());
                log.info("User {} equipped item {} (type {})", userId, itemId, type);
            }
            return storeMapper.toEquippedItemResponse(slot);
        }

        UserEquippedItem equipped = UserEquippedItem.builder()
                .user(user)
                .item(item)
                .itemType(type)
                .equippedAt(Instant.now())
                .build();

        UserEquippedItem saved = userEquippedItemRepository.save(equipped);
        log.info("User {} equipped item {} (type {})", userId, itemId, type);

        return storeMapper.toEquippedItemResponse(saved);
    }

    @Transactional
    public void unequip(UUID userId, ItemType type) {
        userEquippedItemRepository.findByUserIdAndItemType(userId, type).ifPresent(existing -> {
            userEquippedItemRepository.delete(existing);
            log.info("User {} unequipped item {} (type {})", userId, existing.getItem().getId(), type);
        });
    }
}
