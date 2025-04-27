package com.kyonggi.backend.model.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MonthlySavedAmountDto {
    private String month;  // "2025.4"
    private int amount;    // 18400
}
