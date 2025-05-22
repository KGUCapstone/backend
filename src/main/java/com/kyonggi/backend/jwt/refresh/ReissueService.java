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
            log.warn("ReissueService: refresh token null from request");
            return new ResponseEntity<>("refresh token null", HttpStatus.BAD_REQUEST);
        }

        // 1. 리프레시 토큰 만료 여부 확인
        try {
            if (jwtUtil.isExpired(refresh)) { // isExpired가 true를 반환하면 만료된 것
                // DB에서 만료된 refreshToken 삭제 (만료된 경우에만 삭제)
                refreshRepository.deleteByRefresh(refresh);
                log.error("ReissueService: refresh token expired. Deleted from DB.");
                return new ResponseEntity<>("refresh token expired", HttpStatus.FORBIDDEN);
            }
        } catch (ExpiredJwtException e) {
            // 이 블록은 JWTUtil.isExpired에서 이미 처리되므로 사실상 도달하지 않을 수 있습니다.
            // 안전을 위해 한번 더 로깅 및 삭제 로직을 넣습니다.
            refreshRepository.deleteByRefresh(refresh);
            log.error("ReissueService: ExpiredJwtException caught for refresh token. Deleted from DB.");
            return new ResponseEntity<>("refresh token expired (exception caught)", HttpStatus.FORBIDDEN);
        } catch (Exception e) { // 서명 오류 등 유효하지 않은 토큰 처리
            log.error("ReissueService: Invalid refresh token format or signature. Error: {}", e.getMessage());
            return new ResponseEntity<>("invalid refresh token format", HttpStatus.BAD_REQUEST);
        }

        // 2. DB에서 리프레시 토큰 조회 및 동시성 제어
        // synchronized 블록으로 해당 리프레시 토큰에 대한 재발급 요청의 동시성을 제어
        // 이는 Refresh Token Rotation 시, 한 번 사용된 토큰이 여러 번 사용되는 것을 방지합니다.
        // 실제 운영 환경에서는 Redisson(분산 락)과 같은 외부 락 시스템을 고려해야 합니다.
        synchronized (this) { // this 대신 특정 토큰의 해시값이나 DB 엔티티 ID를 락 대상으로 사용할 수도 있습니다.
            Optional<RefreshEntity> refreshEntityOptional = refreshRepository.findByRefresh(refresh);
            if (refreshEntityOptional.isEmpty()) {
                log.error("ReissueService: refresh token not found in DB. Already used or invalid.");
                // 이미 DB에서 삭제되었을 경우 (예: 다른 동시 요청에 의해 이미 재발급됨)
                return new ResponseEntity<>("refresh token not found or already used", HttpStatus.FORBIDDEN);
            }

            // 3. 토큰 카테고리 검증
            String category = jwtUtil.getCategory(refresh);
            if (category == null || !"refresh".equals(category)) { // category가 null인 경우도 포함
                log.error("ReissueService: invalid refresh token category: {}", category);
                // 유효하지 않은 카테고리의 리프레시 토큰은 DB에서도 삭제 (보안 강화)
                refreshRepository.deleteByRefresh(refresh);
                return new ResponseEntity<>("invalid refresh token category", HttpStatus.BAD_REQUEST);
            }

            // 토큰 정보 추출
            String username = jwtUtil.getUsername(refresh);
            String role = jwtUtil.getRole(refresh);
            String name = jwtUtil.getName(refresh);

            // 4. 새로운 Access Token과 Refresh Token 생성
            String newAccess = jwtUtil.createJwt("access", username, role, name, 60 * 60L * 1000L); // 1시간 (초)
            String newRefresh = jwtUtil.createJwt("refresh", username, role, name, 60 * 60L * 1000L * 24); // 24시간 (초)

            // 5. 기존 Refresh Token 삭제 및 새 Refresh Token 저장 (Refresh Token Rotation)
            RefreshEntity existingRefreshEntity = refreshEntityOptional.get();
            refreshRepository.delete(existingRefreshEntity); // 기존 엔티티 삭제
            log.info("ReissueService: Deleted old refresh token: {}", existingRefreshEntity.getRefresh());

            addRefreshEntity(username, newRefresh, 60 * 60 * 24 * 1000L); // 새 리프레시 토큰 저장 (밀리초)
            log.info("ReissueService: Saved new refresh token for user: {}", username);

            // 6. 응답 헤더 설정
            response.setHeader("Authorization", "Bearer " + newAccess);
            response.addCookie(createCookie("refresh", newRefresh, 24 * 60 * 60 * 1000, true, "Lax")); // refresh 쿠키 설정

            log.info("ReissueService: Token reissue successful for user: {}", username);
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            log.debug("ReissueService: No cookies found in request.");
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("refresh".equals(cookie.getName())) {
                log.debug("ReissueService: 'refresh' cookie found.");
                return cookie.getValue();
            }
        }
        log.debug("ReissueService: 'refresh' cookie not found among available cookies.");
        return null;
    }

    // createCookie 메서드 시그니처 및 내부 로직 변경
    private Cookie createCookie(String key, String value, int maxAge, boolean httpOnly, String sameSite) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(false); // 개발 환경에서는 false, HTTPS 배포 시 true로 변경
        cookie.setPath("/");
        cookie.setHttpOnly(httpOnly);
        cookie.setAttribute("SameSite", sameSite);
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