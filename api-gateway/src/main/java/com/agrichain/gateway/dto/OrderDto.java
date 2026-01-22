package com.agrichain.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private String id;
    private String customerId;
    private List<OrderItemDto> items;
    private String status;
    private Double totalAmount;
    private String createdAt;
}
