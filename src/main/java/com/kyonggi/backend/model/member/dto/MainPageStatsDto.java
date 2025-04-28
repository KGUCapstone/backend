package com.kyonggi.backend.model.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MainPageStatsDto {
    private int totalSaved;        // 전체 절약 금액
    private int goalAmount;        // 목표 금액 (10만원)
    private String today;          // 오늘 날짜
    private int weekSpent;         // 주간 소비 금액
    private int weekSaved;         // 주간 절약 금액
}
