package com.kyonggi.backend.jwt;

import com.kyonggi.backend.model.member.dto.CustomUserDetails;
import com.kyonggi.backend.model.member.entity.Member;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    public JWTFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();

        // 필터링을 건너뛸 경로들
        if(requestUri.equals("/api/login") ||
                requestUri.equals("/api/join") ||
                requestUri.equals("/api/reissue") ||
                requestUri.equals("/api/cart/") ||
                requestUri.equals("/api/login/oauth2/code/naver") ||
                requestUri.startsWith("https://nid.naver.com/oauth2.0/authorize") // startsWith로 변경하여 정확히 매치
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        // OAuth2 관련 경로도 필터링 건너뛰기
        if (requestUri.matches("^\\/oauth2(?:\\/.*)?$")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.info("Authorization 헤더가 없거나 Bearer로 시작하지 않음: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = authorizationHeader.substring(7).trim();
        log.debug("Raw Authorization Header: '{}'", authorizationHeader); // 디버그 레벨로 변경
        log.debug("Parsed JWT Token: '{}'", accessToken); // 디버그 레벨로 변경

        try {
            jwtUtil.isExpired(accessToken);
        } catch (ExpiredJwtException e) {
            log.info("ExpiredJwtException - Access token expired for URI: {}", requestUri);
            PrintWriter writer = response.getWriter();
            writer.print("access token expired");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\", error_description=\"The access token expired\"");
            return;
        } catch (Exception e) { // 추가: 다른 JWT 파싱 오류도 여기서 처리
            log.warn("Invalid JWT token for URI: {}. Error: {}", requestUri, e.getMessage());
            PrintWriter writer = response.getWriter();
            writer.print("invalid access token format or signature");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String category = jwtUtil.getCategory(accessToken);

        if (category == null || !category.equals("access")) { // category가 null인 경우도 포함
            log.info("Invalid access token category for URI: {}. Category: {}", requestUri, category);
            PrintWriter writer = response.getWriter();
            writer.print("invalid access token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\", error_description=\"Invalid token type\"");
            return;
        }

        String username = jwtUtil.getUsername(accessToken);
        String role = jwtUtil.getRole(accessToken);

        Member userEntity = new Member();
        userEntity.setUsername(username);
        userEntity.setRole(role);
        // userEntity.setName(jwtUtil.getName(accessToken)); // 필요한 경우 name도 설정

        CustomUserDetails customUserDetails = new CustomUserDetails(userEntity);

        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}