package com.kyonggi.backend.external.naverShopping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NaverSearchResponseDto {
    private List<ShoppingResponse.ShoppingItem> items;
}
