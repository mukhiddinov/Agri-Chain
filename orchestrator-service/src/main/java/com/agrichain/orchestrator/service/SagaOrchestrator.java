package com.agrichain.orchestrator.service;

import com.agrichain.inventory.grpc.InventoryItem;
import com.agrichain.inventory.grpc.InventoryServiceGrpc;
import com.agrichain.inventory.grpc.ReserveInventoryRequest;
import com.agrichain.inventory.grpc.ReserveInventoryResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SagaOrchestrator {

    @GrpcClient("inventory-service")
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryService;

    public void startOrderSaga(String orderId, Map<String, Object> orderEvent) {
        log.info("Starting order saga for order: {}", orderId);
        
        try {
            // Step 1: Reserve inventory
            boolean inventoryReserved = reserveInventory(orderId, orderEvent);
            
            if (inventoryReserved) {
                log.info("Inventory reserved successfully for order: {}", orderId);
                // TODO: Step 2: Process payment
                // TODO: Step 3: Update order status
            } else {
                log.warn("Failed to reserve inventory for order: {}", orderId);
                // TODO: Implement compensation logic
            }
            
        } catch (Exception e) {
            log.error("Error in order saga for order: {}", orderId, e);
            // TODO: Implement compensation/rollback
        }
    }

    private boolean reserveInventory(String orderId, Map<String, Object> orderEvent) {
        try {
            // TODO: Parse items from orderEvent and build inventory items
            // For now, create a stub request
            ReserveInventoryRequest request = ReserveInventoryRequest.newBuilder()
                    .setOrderId(orderId)
                    .build();
            
            ReserveInventoryResponse response = inventoryService.reserveInventory(request);
            return response.getSuccess();
            
        } catch (Exception e) {
            log.error("Failed to call inventory service", e);
            return false;
        }
    }
}
