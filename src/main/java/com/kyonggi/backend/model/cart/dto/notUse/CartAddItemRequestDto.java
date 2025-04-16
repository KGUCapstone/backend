package com.kyonggi.backend.model.cart.dto.notUse;

import com.kyonggi.backend.model.item.dto.OnlineItemDto;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CartAddItemRequestDto {    //상품 추가 요청

    private Long memberId;
    private OnlineItemDto item;
}
