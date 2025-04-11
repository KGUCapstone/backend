package com.kyonggi.backend.model.cart.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
public class CartResponseDto {  //장바구니 반환(기록 포함)

    private Long cartId;
    private String userName;
    private int count;
    private List<CartItemDto> items;
    private boolean isActive;
}
