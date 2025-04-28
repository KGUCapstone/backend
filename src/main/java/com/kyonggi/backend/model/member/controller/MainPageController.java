package com.kyonggi.backend.model.member.controller;

import com.kyonggi.backend.jwt.JWTUtil;
import com.kyonggi.backend.model.member.dto.MainPageStatsDto;
import com.kyonggi.backend.model.member.entity.DailySavedAmount;
import com.kyonggi.backend.model.member.entity.Member;
import com.kyonggi.backend.model.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mainpage")
@RequiredArgsConstructor
public class MainPageController {
    private final JWTUtil jwtUtil;
    private final MemberRepository memberRepository;

    @GetMapping("/stats")
    public MainPageStatsDto getMainPageStats(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String username = jwtUtil.getUsername(token);
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        int totalSaved = member.getMonthlySavedAmounts().stream()
                .mapToInt(DailySavedAmount::getSavedAmount)
                .sum();

        int goalAmount = 100_000; // 기본 절약 목표 10만원

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1); // 이번주 월요일
        LocalDate endOfWeek = startOfWeek.plusDays(6); // 이번주 일요일

        List<DailySavedAmount> weeklyRecords = member.getMonthlySavedAmounts().stream()
                .filter(d -> {
                    LocalDate recordDate = LocalDate.of(d.getYear(), d.getMonth(), d.getDay());
                    return !recordDate.isBefore(startOfWeek) && !recordDate.isAfter(endOfWeek);
                })
                .toList();

        int weekSaved = weeklyRecords.stream()
                .mapToInt(DailySavedAmount::getSavedAmount)
                .sum();

        int weekSpent = weeklyRecords.stream()
                .mapToInt(DailySavedAmount::getConsumedAmount)
                .sum();

        return new MainPageStatsDto(
                totalSaved,
                goalAmount,
                today.format(DateTimeFormatter.ISO_DATE),
                weekSpent,
                weekSaved
        );
    }

    @PostMapping("/calendar")
    public Map<String, Integer> getCalendarData(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String username = jwtUtil.getUsername(token);
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();

        return member.getMonthlySavedAmounts().stream()
                .filter(d -> d.getYear() == currentYear && d.getMonth() == currentMonth)
                .collect(Collectors.toMap(
                        d -> String.format("%04d-%02d-%02d", d.getYear(), d.getMonth(), d.getDay()),
                        DailySavedAmount::getSavedAmount
                ));
    }

}
