package com.kyonggi.backend.jwt;

import com.kyonggi.backend.model.member.repository.RefreshRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@RequiredArgsConstructor
public class CustomLogoutFilter extends GenericFilterBean {
    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        doFilter((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, filterChain);
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        if (!request.getRequestURI().matches("^\\/api/logout$") || !request.getMethod().equals("POST")) {
            filterChain.doFilter(request, response);
            return;
        }

        String refresh = null;
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("refresh")) {
                refresh = cookie.getValue();
            }
        }

        if (refresh == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            jwtUtil.isExpired(refresh);
        } catch (ExpiredJwtException e) {
            // 만료된 refresh 토큰으로 로그아웃 요청이 들어와도 성공 처리하는 것이 더 사용자 친화적일 수 있음.
            // 현재는 BAD_REQUEST를 반환하지만, 200 OK로 변경을 고려할 수 있습니다.
            // refreshRepository.deleteByRefresh(refresh); // 만료된 토큰은 이미 재발급 과정에서 삭제되거나 여기서 명시적으로 삭제.
            response.setStatus(HttpServletResponse.SC_OK); // 변경 고려
            clearCookies(response); // 쿠키만 삭제하고 성공 처리
            return;
        }

        String category = jwtUtil.getCategory(refresh);
        if (!category.equals("refresh")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // DB에 refresh 토큰이 존재하지 않는 경우 (이미 삭제되었거나 위변조된 경우)
        if (!refreshRepository.existsByRefresh(refresh)) {
            // 이 경우도 로그아웃 성공으로 간주하고 쿠키를 삭제하는 것이 사용자 경험에 좋습니다.
            response.setStatus(HttpServletResponse.SC_OK); // 변경 고려
            clearCookies(response); // 쿠키만 삭제하고 성공 처리
            return;
        }

        // 유효하고 DB에 존재하는 refresh 토큰인 경우 삭제
        refreshRepository.deleteByRefresh(refresh); //

        clearCookies(response); // 쿠키 삭제
        response.setStatus(HttpServletResponse.SC_OK);
    }

    // 쿠키를 삭제하는 헬퍼 메서드 추가 (중복 코드 방지 및 가독성 향상)
    private void clearCookies(HttpServletResponse response) {
        // refresh 쿠키 삭제
        Cookie refreshCookie = new Cookie("refresh", null);
        refreshCookie.setMaxAge(0);
        refreshCookie.setPath("/");
        refreshCookie.setHttpOnly(true); // ReissueService에서 HttpOnly=true로 설정했으므로 동일하게 설정
        refreshCookie.setAttribute("SameSite", "Lax"); // SameSite도 일관성 유지

        // access_token 쿠키 삭제
        Cookie accessTokenCookie = new Cookie("access_token", null);
        accessTokenCookie.setMaxAge(0);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setHttpOnly(false); // CustomSuccessHandler에서 HttpOnly=false로 설정했으므로 동일하게 설정
        accessTokenCookie.setAttribute("SameSite", "Lax"); // SameSite도 일관성 유지

        response.addCookie(accessTokenCookie);
        response.addCookie(refreshCookie);
    }
}