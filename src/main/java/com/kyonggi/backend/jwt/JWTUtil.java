package com.kyonggi.backend.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JWTUtil {

    private SecretKey secretKey;

    public JWTUtil(@Value("${spring.jwt.secret}")String secret) {
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    public String getName(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("name", String.class);
    }

    public String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("username", String.class);
    }

    public String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    public Boolean isExpired(String token) {
        try {
            // 토큰 파싱 시 만료 여부를 즉시 확인
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            // 만료된 토큰인 경우 true 반환
            return true;
        } catch (SignatureException | IllegalArgumentException e) { // 토큰이 유효하지 않은 경우 (서명 오류, 형식 오류 등)

            return true;
        }
    }

    // JWT 토큰에서 'category' 클레임을 추출하는 메서드
    public String getCategory(String token) {
        try {
            // 토큰을 파싱하여 클레임(payload)을 얻고, 'category' 클레임 값을 String 형태로 반환
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("category", String.class);
        } catch (ExpiredJwtException e) {

            return null; // 만료된 토큰은 카테고리 검증을 통과하지 못하게 null 반환 (ReissueService에서 BAD_REQUEST로 처리)
        } catch (SignatureException | IllegalArgumentException e) {
            // 토큰 서명이 유효하지 않거나 토큰 형식이 잘못된 경우
            return null; // 유효하지 않은 토큰은 카테고리 검증을 통과하지 못하게 null 반환
        }
    }

    public String createJwt(String category,String username, String role, String name, Long expiredMs) {
        return Jwts.builder()
                .claim("category", category)
                .claim("username", username)
                .claim("role", role)
                .claim("name", name)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey)
                .compact();
    }
}