package com.agrichain.orchestrator.listener;

import com.agrichain.orchestrator.service.SagaOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final SagaOrchestrator sagaOrchestrator;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.created", groupId = "orchestrator-service")
    public void handleOrderCreated(String message) {
        try {
            log.info("Received order.created event: {}", message);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> orderEvent = objectMapper.readValue(message, Map.class);
            String orderId = (String) orderEvent.get("orderId");
            
            // Start saga orchestration
            sagaOrchestrator.startOrderSaga(orderId, orderEvent);
            
        } catch (Exception e) {
            log.error("Error processing order.created event", e);
        }
    }
}
