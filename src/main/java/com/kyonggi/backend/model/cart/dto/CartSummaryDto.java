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
    private String thumbnailUrl;
    private int totalQuantity;
    private int totalPrice;

    public CartSummaryDto(Long cartId, String name, LocalDateTime createdAt,
                          String thumbnailUrl, int totalQuantity, int totalPrice) {
        this.cartId = cartId;
        this.name = name;
        this.createdAt = createdAt;
        this.thumbnailUrl = thumbnailUrl;
        this.totalQuantity = totalQuantity;
        this.totalPrice = totalPrice;
    }

}

