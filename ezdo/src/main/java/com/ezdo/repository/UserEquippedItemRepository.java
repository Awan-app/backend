package com.ezdo.repository;

import com.ezdo.entity.ItemType;
import com.ezdo.entity.UserEquippedItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserEquippedItemRepository extends JpaRepository<UserEquippedItem, UUID> {

    @EntityGraph(attributePaths = "item")
    Optional<UserEquippedItem> findByUserIdAndItemType(UUID userId, ItemType itemType);

    @EntityGraph(attributePaths = "item")
    List<UserEquippedItem> findByUserIdOrderByItemType(UUID userId);
}
