package com.kyonggi.backend.model.cart.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Data
@Getter
@Setter
public class CartItemDto {  //장바구니에 들어있는 상품 정보

    private Long id;
    private String title;
    private Integer price;
    private String link;
    private String image;
    private String mallName;
    private String brand;
    private String volume;

    private LocalDateTime createdAt;
    private int quantity;
    private int compareItemPrice;
}


