package com.kyonggi.backend.model.cart.dto.notUse;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HistoryRequestDto {   //기록 요청
    private Long cartId;
    private Long memberId;
    private boolean isActive;
}
