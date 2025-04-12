package com.kyonggi.backend.model.cart.dto.notUse;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CartRemoveItemRequestDto {     //상품 삭제 요청

    private Long memberId;
    private Long itemId;
}
