package com.ezdo.repository;

import com.ezdo.entity.Item;
import com.ezdo.entity.User;
import com.ezdo.entity.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, UUID> {
    List<UserItem> findByUser(User user);
    
    Optional<UserItem> findByUserAndItem(User user, Item item);
    
    boolean existsByUserAndItem(User user, Item item);
}
