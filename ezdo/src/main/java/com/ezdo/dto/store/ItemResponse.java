package com.ezdo.dto.store;

import com.ezdo.entity.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemResponse {
    private UUID id;
    private String name;
    private String description;
    private String image;
    private String info;
    private Integer price;
    private String version;
    private ItemType type;
}
