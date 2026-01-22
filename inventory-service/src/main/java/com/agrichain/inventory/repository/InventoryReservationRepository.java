package com.agrichain.inventory.repository;

import com.agrichain.inventory.entity.InventoryReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservationEntity, String> {
}
