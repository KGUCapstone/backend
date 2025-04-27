package com.kyonggi.backend.external.naverShopping;


import lombok.Data;

import java.util.List;

@Data
public class CompareRequestDto {
    private List<NaverSearchRequestDto> conditions;
    private List<String> mallNames;
}
