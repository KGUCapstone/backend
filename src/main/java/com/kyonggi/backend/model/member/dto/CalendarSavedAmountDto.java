package com.kyonggi.backend.model.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class CalendarSavedAmountDto {
    private String date;     // yyyy-MM-dd
    private Integer amount;
}