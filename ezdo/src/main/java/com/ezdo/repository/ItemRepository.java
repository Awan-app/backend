package com.ezdo.repository;

import com.ezdo.entity.Item;
import com.ezdo.entity.ItemType;
import com.ezdo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {
    List<Item> findByType(ItemType type);

    @Query("SELECT i FROM Item i WHERE i.id NOT IN (SELECT ui.item.id FROM UserItem ui WHERE ui.user = :user)")
    List<Item> findUnownedBy(@Param("user") User user);
}
