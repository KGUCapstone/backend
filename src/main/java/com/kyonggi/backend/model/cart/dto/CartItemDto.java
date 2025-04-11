package com.kyonggi.backend.model.cart.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Data
@Getter
@Setter
public class CartItemDto {  //장바구니에 들어있는 상품 정보

    private Long id;
    private String name;
    private Integer price;
    private String link;
    private String image;
    private String mallName;
    private String brand;
    private String volume;
}


