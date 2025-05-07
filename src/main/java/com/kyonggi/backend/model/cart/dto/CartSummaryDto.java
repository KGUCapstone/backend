package com.kyonggi.backend.model.cart.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Getter
@Setter
public class CartSummaryDto {
    private Long cartId;
    private String name;
    private LocalDateTime createdAt;

    public CartSummaryDto(Long cartId, String name, LocalDateTime createdAt) {
        this.cartId = cartId;
        this.name = name;
        this.createdAt = createdAt;
    }
}

