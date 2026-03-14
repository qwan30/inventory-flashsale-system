package com.codex.flashsale.controller;

import com.codex.flashsale.api.InventoryResponse;
import com.codex.flashsale.inventory.InventoryItem;
import com.codex.flashsale.inventory.InventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{sku}")
    public InventoryResponse getInventory(@PathVariable String sku) {
        InventoryItem inventoryItem = inventoryService.getRequiredInventory(sku);
        return new InventoryResponse(
                inventoryItem.getSku(),
                inventoryItem.getAvailableQty(),
                inventoryItem.getReservedQty(),
                inventoryItem.getSoldQty(),
                inventoryItem.getVersion()
        );
    }
}

