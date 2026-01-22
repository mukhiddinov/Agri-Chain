package com.agrichain.inventory.repository;

import com.agrichain.inventory.entity.InventoryItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItemEntity, Long> {

    Optional<InventoryItemEntity> findByProductId(String productId);
}
