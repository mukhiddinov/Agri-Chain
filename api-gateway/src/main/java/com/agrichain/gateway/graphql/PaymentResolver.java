package com.agrichain.gateway.graphql;

import com.agrichain.gateway.dto.PaymentResultDto;
import com.agrichain.gateway.dto.PaymentVerificationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
@Slf4j
public class PaymentResolver {

    private final WebClient paymentServiceClient;

    public PaymentResolver(@Qualifier("paymentServiceClient") WebClient paymentServiceClient) {
        this.paymentServiceClient = paymentServiceClient;
    }

    @MutationMapping
    public Mono<PaymentResultDto> processPayment(@Argument String orderId, @Argument Double amount) {
        log.info("GraphQL mutation: processPayment(orderId={}, amount={})", orderId, amount);

        Map<String, Object> requestBody = Map.of(
                "orderId", orderId,
                "amount", amount
        );

        return paymentServiceClient.post()
                .uri("/api/payments/process")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(PaymentResultDto.class)
                .doOnError(e -> log.error("Error processing payment for order {}", orderId, e));
    }

    @QueryMapping
    public Mono<PaymentVerificationDto> verifyPayment(@Argument String transactionHash) {
        log.info("GraphQL query: verifyPayment(transactionHash={})", transactionHash);

        return paymentServiceClient.get()
                .uri("/api/payments/verify/{hash}", transactionHash)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> PaymentVerificationDto.builder()
                        .transactionHash(transactionHash)
                        .verified((Boolean) response.get("verified"))
                        .build())
                .doOnError(e -> log.error("Error verifying payment {}", transactionHash, e))
                .onErrorReturn(PaymentVerificationDto.builder()
                        .transactionHash(transactionHash)
                        .verified(false)
                        .build());
    }
}
