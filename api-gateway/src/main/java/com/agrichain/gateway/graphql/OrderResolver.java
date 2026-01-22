package com.agrichain.gateway.graphql;

import com.agrichain.gateway.dto.CreateOrderInput;
import com.agrichain.gateway.dto.OrderDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Controller
@Slf4j
public class OrderResolver {

    private final WebClient orderServiceClient;

    public OrderResolver(@Qualifier("orderServiceClient") WebClient orderServiceClient) {
        this.orderServiceClient = orderServiceClient;
    }

    @QueryMapping
    public Mono<OrderDto> order(@Argument String id) {
        log.info("GraphQL query: order(id={})", id);
        return orderServiceClient.get()
                .uri("/api/orders/{id}", id)
                .retrieve()
                .bodyToMono(OrderDto.class)
                .doOnError(e -> log.error("Error fetching order {}", id, e))
                .onErrorReturn(OrderDto.builder().id(id).status("NOT_FOUND").build());
    }

    @QueryMapping
    public Mono<List<OrderDto>> orders() {
        log.info("GraphQL query: orders");
        // order-service doesn't have a list endpoint yet, return empty
        return Mono.just(List.of());
    }

    @MutationMapping
    public Mono<OrderDto> createOrder(@Argument CreateOrderInput input) {
        log.info("GraphQL mutation: createOrder for customer {}", input.getCustomerId());

        Map<String, Object> requestBody = Map.of(
                "customerId", input.getCustomerId(),
                "items", input.getItems()
        );

        return orderServiceClient.post()
                .uri("/api/orders")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(OrderDto.class)
                .doOnError(e -> log.error("Error creating order", e));
    }
}
