package com.kyonggi.backend.model.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompleteCartRequestDto {
    private List<CartItemDto> selectedItems;
    private int totalSavedAmount;
}
