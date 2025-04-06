package com.kyonggi.backend.external.naverShopping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NaverSearchRequestDto {
    private String title;
    private int price;
    private String volume;
    private String brand;
}
