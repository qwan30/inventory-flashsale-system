package com.codex.flashsale.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {
}

