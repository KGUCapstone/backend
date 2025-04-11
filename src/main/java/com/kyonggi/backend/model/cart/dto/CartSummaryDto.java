package com.kyonggi.backend.model.cart.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CartSummaryDto {
    private Long cartId;
    private String name;
    private LocalDateTime createdAt;

    public CartSummaryDto(Long cartId, String name) {
        this.cartId = cartId;
        this.name = name;
        this.createdAt = LocalDateTime.now().withSecond(0).withNano(0);
    }
}

