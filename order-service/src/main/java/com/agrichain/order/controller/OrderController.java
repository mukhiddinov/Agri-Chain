package com.agrichain.order.controller;

import com.agrichain.order.model.CreateOrderRequest;
import com.agrichain.order.model.OrderResponse;
import com.agrichain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("Received create order request for customer: {}", request.getCustomerId());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        log.info("Fetching order: {}", orderId);
        return orderService.getOrder(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable String orderId, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Updating order status: {} -> {}", orderId, status);
        orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.noContent().build();
    }
}
