package com.kyonggi.backend.model.member.controller;

import com.kyonggi.backend.jwt.JWTUtil;
import com.kyonggi.backend.model.member.dto.MypageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class MypageController {

    private final JWTUtil jwtUtil;

    @GetMapping("/api/mypage")
    public MypageResponseDto mypage(@RequestHeader("Authorization") String token) {
        //System.out.println("받은 토큰 = " + token);

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);  // "Bearer " 제거
        }

        try {
            String username = jwtUtil.getUsername(token);
            System.out.println("토큰에서 추출한 사용자: " + username);
            String name = jwtUtil.getName(token);

            return new MypageResponseDto(username,name);
        } catch (Exception e) {
            System.out.println("JWT 검증 실패: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid token");
        }
    }
}
