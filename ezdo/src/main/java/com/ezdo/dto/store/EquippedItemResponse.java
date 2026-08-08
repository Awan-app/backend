package com.ezdo.dto.store;

import com.ezdo.entity.ItemType;

import java.time.Instant;

public record EquippedItemResponse(
    ItemType type,
    ItemResponse item,
    Instant equippedAt
) {}
