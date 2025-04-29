package com.kyonggi.backend.model.member.controller;

import com.kyonggi.backend.jwt.JWTUtil;
import com.kyonggi.backend.model.member.dto.MonthlySavedAmountDto;
import com.kyonggi.backend.model.member.dto.MypageResponseDto;
import com.kyonggi.backend.model.member.entity.DailySavedAmount;
import com.kyonggi.backend.model.member.entity.Member;
import com.kyonggi.backend.model.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MypageController {

    private final JWTUtil jwtUtil;
    private final MemberRepository memberRepository;

    @GetMapping("/api/mypage")
    public MypageResponseDto mypage(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            String username = jwtUtil.getUsername(token);
            String name = jwtUtil.getName(token);

            Member member = memberRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

            int thisMonthSaved = calculateThisMonthSaved(member);

            return new MypageResponseDto(username, name, thisMonthSaved);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid token");
        }
    }

    private int calculateThisMonthSaved(Member member) {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        return member.getMonthlySavedAmounts().stream()
                .filter(m -> m.getYear() == year && m.getMonth() == month)
                .mapToInt(DailySavedAmount::getSavedAmount)
                .sum();
    }
//
//    @GetMapping("/api/mypage/saved-amounts")
//    public List<MonthlySavedAmountDto> getSavedAmounts(@RequestHeader("Authorization") String token) {
//        if (token.startsWith("Bearer ")) {
//            token = token.substring(7);
//        }
//        String username = jwtUtil.getUsername(token);
//
//        Member member = memberRepository.findByUsername(username)
//                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
//
//        return member.getMonthlySavedAmounts().stream()
//                .map(m -> new MonthlySavedAmountDto(
//                        m.getYear() + "." + m.getMonth(), // "2025.4" 형식
//                        m.getSavedAmount()
//                ))
//                .sorted(Comparator.comparing(MonthlySavedAmountDto::getMonth))
//                .toList();
//    }

    @GetMapping("/api/mypage/saved-amounts")
    public List<MonthlySavedAmountDto> getSavedAmounts(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String username = jwtUtil.getUsername(token);

        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 연.월 기준으로 그룹핑  누적 합산
        Map<String, Integer> monthlyMap = member.getMonthlySavedAmounts().stream()
                .collect(Collectors.groupingBy(
                        d -> d.getYear() + "." + d.getMonth(),
                        Collectors.summingInt(DailySavedAmount::getSavedAmount)
                ));

        return monthlyMap.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> new MonthlySavedAmountDto(entry.getKey(), entry.getValue()))
                .toList();
    }


}

