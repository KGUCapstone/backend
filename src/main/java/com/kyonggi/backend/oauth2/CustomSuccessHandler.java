package com.kyonggi.backend.oauth2;

import com.kyonggi.backend.jwt.JWTUtil;
import com.kyonggi.backend.jwt.refresh.RefreshEntity;
import com.kyonggi.backend.model.member.repository.RefreshRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

@Component
@RequiredArgsConstructor
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;

    @Value("${spring.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        //OAuth2User
        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();

        String username = customUserDetails.getUsername();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        String name = customUserDetails.getName();

        // token 생성
        String access = jwtUtil.createJwt("access", username, role,name, 60 * 60L); // 1시간
        String refresh = jwtUtil.createJwt("refresh", username, role,name, 24 * 60 * 60L); // 24시간

        //Refresh 토큰 저장
        addRefreshEntity(username, refresh, 86400000L);

        // 응답 생성
        response.setHeader("Authorization", "Bearer " + access);
        response.addCookie(createCookie("refresh", refresh, 24 * 60 * 60));

        //System.out.println("access = " + access);
        response.setStatus(HttpStatus.OK.value());


        // 프론트엔드로 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, frontendUrl+"/home");

    }

    private Cookie createCookie(String key, String value, int maxAge) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(false); // HTTPS에서만 전송 (개발 환경에서는 false로 변경)
        cookie.setPath("/");
        cookie.setHttpOnly(true); // JavaScript에서 접근 불가
        cookie.setAttribute("SameSite", "Lax"); // CORS 환경에서는 Lax가 더 적합
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