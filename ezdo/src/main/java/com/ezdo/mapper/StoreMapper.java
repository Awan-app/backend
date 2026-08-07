package com.ezdo.mapper;

import com.ezdo.dto.store.ItemResponse;
import com.ezdo.dto.store.UserItemResponse;
import com.ezdo.entity.Item;
import com.ezdo.entity.UserItem;
import org.springframework.stereotype.Component;

@Component
public class StoreMapper {

    public ItemResponse toItemResponse(Item item) {
        if (item == null) {
            return null;
        }
        return ItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .image(item.getImage())
                .info(item.getInfo())
                .price(item.getPrice())
                .version(item.getVersion())
                .type(item.getType())
                .build();
    }

    public UserItemResponse toUserItemResponse(UserItem userItem) {
        if (userItem == null) {
            return null;
        }
        return UserItemResponse.builder()
                .id(userItem.getId())
                .item(toItemResponse(userItem.getItem()))
                .boughtAt(userItem.getBoughtAt())
                .build();
    }
}
