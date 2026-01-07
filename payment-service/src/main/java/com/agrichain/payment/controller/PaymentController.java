package com.agrichain.payment.controller;

import com.agrichain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<Map<String, String>> processPayment(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("orderId");
        Double amount = ((Number) request.get("amount")).doubleValue();
        
        String transactionHash = paymentService.processPayment(orderId, amount);
        
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "transactionHash", transactionHash,
                "status", "PENDING"
        ));
    }

    @GetMapping("/verify/{transactionHash}")
    public ResponseEntity<Map<String, Boolean>> verifyPayment(@PathVariable String transactionHash) {
        boolean verified = paymentService.verifyPayment(transactionHash);
        return ResponseEntity.ok(Map.of("verified", verified));
    }
}
