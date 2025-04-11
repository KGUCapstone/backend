package com.kyonggi.backend.model.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartAddItemResponseDto {       //상품 추가 반환
    private Long itemId;         // 추가된 아이템 ID
    private String itemName;     // 아이템 이름
    private int price;           // 가격
    private int totalItemCount;  // 현재 장바구니에 들어있는 전체 아이템 수
}
