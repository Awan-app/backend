package com.ezdo.controller;

import com.ezdo.dto.store.ItemResponse;
import com.ezdo.dto.store.UserItemResponse;
import com.ezdo.entity.ItemType;
import com.ezdo.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @GetMapping("/items")
    public ResponseEntity<List<ItemResponse>> getAllItems(
            @RequestParam(required = false) ItemType type
    ) {
        return ResponseEntity.ok(storeService.getAllItems(type));
    }

    @GetMapping("/inventory")
    public ResponseEntity<List<UserItemResponse>> getUserInventory(
            @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(storeService.getUserInventory(userId));
    }

    @PostMapping("/items/{itemId}/buy")
    public ResponseEntity<UserItemResponse> buyItem(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID itemId
    ) {
        return ResponseEntity.ok(storeService.buyItem(userId, itemId));
    }
}
