package com.kyonggi.backend.jwt.refresh;

import com.kyonggi.backend.jwt.JWTUtil;
import com.kyonggi.backend.model.member.repository.RefreshRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReissueService {

    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;

    @Transactional
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refresh = extractRefreshToken(request);

        if (refresh == null) {
            return new ResponseEntity<>("refresh token null", HttpStatus.BAD_REQUEST);
        }

        try {
            jwtUtil.isExpired(refresh);
        } catch (ExpiredJwtException e) {
            refreshRepository.deleteByRefresh(refresh); // 만료된 refreshToken 삭제
            log.error("refresh token expired");
            return new ResponseEntity<>("refresh token expired", HttpStatus.FORBIDDEN);
        }

        Optional<RefreshEntity> refreshEntity = refreshRepository.findByRefresh(refresh);
        if (refreshEntity.isEmpty()) {
            log.error("refresh token not found");
            return new ResponseEntity<>("refresh token not found", HttpStatus.FORBIDDEN);
        }

        // 토큰 검증
        String category = jwtUtil.getCategory(refresh);
        if (!"refresh".equals(category)) {
            log.error("invalid refresh token");
            return new ResponseEntity<>("invalid refresh token", HttpStatus.BAD_REQUEST);
        }

        String username = jwtUtil.getUsername(refresh);
        String role = jwtUtil.getRole(refresh);
        String name = jwtUtil.getName(refresh);

        String newAccess = jwtUtil.createJwt("access", username, role,name, 60 * 60L);
        String newRefresh = jwtUtil.createJwt("refresh", username, role,name, 60 * 60 * 24L);

        // 기존 Refresh Token 삭제 (존재 여부 확인 후 삭제)
        refreshEntity.ifPresent(entity -> refreshRepository.deleteByRefresh(entity.getRefresh()));

        // 새 Refresh Token 저장
        addRefreshEntity(username, newRefresh, 60 * 60 * 24L);

        response.setHeader("Authorization", "Bearer " + newAccess);
        response.addCookie(createCookie("refresh", newRefresh, 60 * 60 * 24));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if ("refresh".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private Cookie createCookie(String key, String value, int maxAge) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(false); // HTTPS에서만 전송 (개발 환경에서는 false로 변경)
        cookie.setPath("/");
        cookie.setHttpOnly(true); // JavaScript에서 접근 불가
        cookie.setAttribute("SameSite", "Strict"); // CORS 환경에서 필수
        return cookie;
    }

    private void addRefreshEntity(String username, String refresh, Long expiredMs) {
        Date date = new Date(System.currentTimeMillis() + expiredMs);

        RefreshEntity refreshEntity = new RefreshEntity();
        refreshEntity.setUsername(username);
        refreshEntity.setRefresh(refresh);
        refreshEntity.setExpiration(date.toString());

        refreshRepository.save(refreshEntity);
    }
}
