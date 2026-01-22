package com.agrichain.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${gateway.services.order:http://localhost:8081}")
    private String orderServiceUrl;

    @Value("${gateway.services.catalog:http://localhost:8085}")
    private String catalogServiceUrl;

    @Value("${gateway.services.payment:http://localhost:8083}")
    private String paymentServiceUrl;

    @Bean
    public WebClient orderServiceClient() {
        return WebClient.builder()
                .baseUrl(orderServiceUrl)
                .build();
    }

    @Bean
    public WebClient catalogServiceClient() {
        return WebClient.builder()
                .baseUrl(catalogServiceUrl)
                .build();
    }

    @Bean
    public WebClient paymentServiceClient() {
        return WebClient.builder()
                .baseUrl(paymentServiceUrl)
                .build();
    }
}
