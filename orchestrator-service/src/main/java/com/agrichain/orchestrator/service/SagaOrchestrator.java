package com.agrichain.orchestrator.service;

import com.agrichain.inventory.grpc.InventoryItem;
import com.agrichain.inventory.grpc.InventoryServiceGrpc;
import com.agrichain.inventory.grpc.ReleaseInventoryRequest;
import com.agrichain.inventory.grpc.ReleaseInventoryResponse;
import com.agrichain.inventory.grpc.ReserveInventoryRequest;
import com.agrichain.inventory.grpc.ReserveInventoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SagaOrchestrator {

    private final RestTemplate restTemplate;

    @GrpcClient("inventory-service")
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryService;

    @Value("${orchestrator.services.order:http://localhost:8081}")
    private String orderServiceBaseUrl;

    @Value("${orchestrator.services.payment:http://localhost:8083}")
    private String paymentServiceBaseUrl;

    public void startOrderSaga(String orderId, Map<String, Object> orderEvent) {
        log.info("Starting order saga for order: {}", orderId);

        String reservationId = null;
        try {
            double totalAmount = extractTotalAmount(orderEvent);

            ReserveInventoryResponse reserveResponse = reserveInventory(orderId, orderEvent);
            if (!reserveResponse.getSuccess()) {
                log.warn("Failed to reserve inventory for order: {} - {}", orderId, reserveResponse.getMessage());
                updateOrderStatus(orderId, "FAILED");
                return;
            }

            reservationId = reserveResponse.getReservationId();
            log.info("Inventory reserved successfully for order: {}, reservationId={}", orderId, reservationId);
            updateOrderStatus(orderId, "INVENTORY_RESERVED");

            String transactionHash = processPayment(orderId, totalAmount);
            log.info("Payment processed for order: {}, txHash={}", orderId, transactionHash);

            updateOrderStatus(orderId, "PAID");

        } catch (Exception e) {
            log.error("Error in order saga for order: {}", orderId, e);
            if (reservationId != null) {
                compensateInventory(orderId, reservationId);
            }
            updateOrderStatus(orderId, "FAILED");
        }
    }

    private double extractTotalAmount(Map<String, Object> orderEvent) {
        Object value = orderEvent.get("totalAmount");
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalArgumentException("orderEvent.totalAmount is missing or invalid");
    }

    private ReserveInventoryResponse reserveInventory(String orderId, Map<String, Object> orderEvent) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsRaw = (List<Map<String, Object>>) orderEvent.getOrDefault("items", Collections.emptyList());

            List<InventoryItem> items = itemsRaw.stream()
                    .map(item -> InventoryItem.newBuilder()
                            .setProductId((String) item.get("productId"))
                            .setQuantity(((Number) item.get("quantity")).intValue())
                            .build())
                    .toList();

            ReserveInventoryRequest request = ReserveInventoryRequest.newBuilder()
                    .setOrderId(orderId)
                    .addAllItems(items)
                    .build();

            return inventoryService.reserveInventory(request);

        } catch (Exception e) {
            log.error("Failed to call inventory service", e);
            return ReserveInventoryResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error reserving inventory: " + e.getMessage())
                    .build();
        }
    }

    private void compensateInventory(String orderId, String reservationId) {
        try {
            log.info("Compensating inventory for order: {}, reservationId={}", orderId, reservationId);
            ReleaseInventoryRequest request = ReleaseInventoryRequest.newBuilder()
                    .setReservationId(reservationId)
                    .setOrderId(orderId)
                    .build();

            ReleaseInventoryResponse response = inventoryService.releaseInventory(request);
            if (!response.getSuccess()) {
                log.warn("Failed to release inventory during compensation for order {}: {}", orderId, response.getMessage());
            }
        } catch (Exception e) {
            log.error("Error during inventory compensation for order: {}", orderId, e);
        }
    }

    private String processPayment(String orderId, double amount) {
        String url = paymentServiceBaseUrl + "/api/payments/process";
        Map<String, Object> requestBody = Map.of(
                "orderId", orderId,
                "amount", amount
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Payment service returned non-success status: " + response.getStatusCode());
        }

        Object txHash = response.getBody().get("transactionHash");
        if (txHash == null) {
            throw new IllegalStateException("Payment service response missing transactionHash");
        }

        return txHash.toString();
    }

    private void updateOrderStatus(String orderId, String status) {
        try {
            String url = orderServiceBaseUrl + "/api/orders/" + orderId + "/status";
            Map<String, String> body = Map.of("status", status);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PATCH, entity, Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Failed to update order status for {} to {}: HTTP {}", orderId, status, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error updating order status for {} to {}", orderId, status, e);
        }
    }
}
