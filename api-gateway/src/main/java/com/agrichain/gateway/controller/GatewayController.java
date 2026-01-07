package com.agrichain.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
public class GatewayController {

    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        log.info("Health check requested");
        return Mono.just(Map.of(
                "status", "UP",
                "service", "api-gateway"
        ));
    }

    @GetMapping("/info")
    public Mono<Map<String, String>> info() {
        return Mono.just(Map.of(
                "name", "AgriChain API Gateway",
                "version", "1.0.0-SNAPSHOT",
                "description", "API Gateway with WebFlux and GraphQL support (placeholder)"
        ));
    }
}
