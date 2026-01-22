package com.agrichain.gateway.graphql;

import com.agrichain.gateway.dto.ProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
@Slf4j
public class ProductResolver {

    private final WebClient catalogServiceClient;

    public ProductResolver(@Qualifier("catalogServiceClient") WebClient catalogServiceClient) {
        this.catalogServiceClient = catalogServiceClient;
    }

    @QueryMapping
    public Mono<ProductDto> product(@Argument String id) {
        log.info("GraphQL query: product(id={})", id);
        return catalogServiceClient.get()
                .uri("/api/products/{id}", id)
                .retrieve()
                .bodyToMono(ProductDto.class)
                .doOnError(e -> log.error("Error fetching product {}", id, e))
                .onErrorReturn(ProductDto.builder().id(id).name("NOT_FOUND").build());
    }

    @QueryMapping
    public Mono<List<ProductDto>> products() {
        log.info("GraphQL query: products");
        return catalogServiceClient.get()
                .uri("/api/products")
                .retrieve()
                .bodyToFlux(ProductDto.class)
                .collectList()
                .doOnError(e -> log.error("Error fetching products", e))
                .onErrorReturn(List.of());
    }
}
