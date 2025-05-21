package com.kyonggi.backend.model.member.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MonthlySavedAmountDto {
    private String month;  // "2025.4"
    private int amount;    // 18400
}
