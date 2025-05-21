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
                // requestUri.equals("/api/refresh") // refresh 엔드포인트는 reissue와 동일하게 처리되거나 제거
                requestUri.equals("/api/cart/") || // 장바구니 API가 인증 없이 접근 가능해야 하는 경우
                requestUri.equals("/api/login/oauth2/code/naver") ||
                requestUri.equals("https://nid.naver.com/oauth2.0/authorize/**")
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
            log.info("Authorization 헤더가 없거나 Bearer로 시작하지 않음");
            filterChain.doFilter(request, response); // 토큰이 없거나 형식이 잘못된 경우 다음 필터로 넘김 (public 접근 허용을 위해)
            return;
        }

        String accessToken = authorizationHeader.substring(7).trim();
        log.info("Raw Authorization Header: '{}'", authorizationHeader);
        log.info("Parsed JWT Token: '{}'", accessToken);

        try {
            jwtUtil.isExpired(accessToken);
        } catch (ExpiredJwtException e) {
            log.info("ExpiredJwtException - Access token expired for URI: {}", requestUri); // 로그에 URI 추가
            PrintWriter writer = response.getWriter();
            writer.print("access token expired");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
            // 프론트엔드에서 401을 처리하여 토큰 갱신을 시도하도록 WWW-Authenticate 헤더 추가 고려
            response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\", error_description=\"The access token expired\"");
            return;
        }

        String category = jwtUtil.getCategory(accessToken);

        if (!category.equals("access")) {
            log.info("Invalid access token category for URI: {}", requestUri); // 로그에 URI 추가
            PrintWriter writer = response.getWriter();
            writer.print("invalid access token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
            response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\", error_description=\"Invalid token type\"");
            return;
        }

        String username = jwtUtil.getUsername(accessToken);
        String role = jwtUtil.getRole(accessToken);

        // Member 엔티티는 JPA에서 관리되므로, 필요한 필드만 설정하여 CustomUserDetails에 전달
        Member userEntity = new Member();
        userEntity.setUsername(username);
        userEntity.setRole(role);
        // userEntity.setId()나 다른 필드는 여기서 직접 DB 조회 없이 JWT에서만 사용하는 경우 설정하지 않아도 됨.
        // 하지만 서비스 로직에서 memberId가 필요하면, 여기서는 username으로 MemberRepository에서 조회하여
        // Member 객체를 가져와야 합니다. (현재 CartControllerV2에서 memberId를 추출하는 로직 참고)
        // ex: Member member = memberRepository.findByUsername(username).orElseThrow();
        // CustomUserDetails customUserDetails = new CustomUserDetails(member);
        CustomUserDetails customUserDetails = new CustomUserDetails(userEntity); //

        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities()); //
        SecurityContextHolder.getContext().setAuthentication(authToken); //

        filterChain.doFilter(request, response);
    }
}