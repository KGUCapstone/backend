package com.kyonggi.backend.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyonggi.backend.jwt.refresh.RefreshEntity;
import com.kyonggi.backend.model.member.dto.CustomUserDetails;
import com.kyonggi.backend.model.member.dto.LoginDTO;
import com.kyonggi.backend.model.member.repository.RefreshRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;
    private Long accessTokenExpiration;

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, RefreshRepository refreshRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshRepository = refreshRepository;
        setFilterProcessesUrl("/api/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

//        String username = obtainUsername(request);
//        String password = obtainPassword(request);
//        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password, null);
//        return authenticationManager.authenticate(authToken);

        // form-data -> json 으로 변경
        LoginDTO loginDTO = new LoginDTO();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ServletInputStream inputStream = request.getInputStream();
            String messageBody = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            loginDTO = objectMapper.readValue(messageBody, LoginDTO.class);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();

        //System.out.println(username);
        System.out.println("attemptAuthentication");

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);

        return authenticationManager.authenticate(authToken);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authentication
    ) throws IOException {

        //System.out.println("successfulAuthentication");
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        //String username = authentication.getName();
        String username = customUserDetails.getUsername();
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        String name = customUserDetails.getName();

        // token 생성
        String access = jwtUtil.createJwt("access", username, role,  name,60 * 60L); // 1시간
        String refresh = jwtUtil.createJwt("refresh", username, role, name, 24 * 60 * 60L); // 24


        //Refresh 토큰 저장
        addRefreshEntity(username, refresh, 86400000L);


        // 응답 생성
        response.setHeader("Authorization", "Bearer " + access);
        response.addCookie(createCookie("refresh", refresh, 24 * 60 * 60));

        response.setStatus(HttpStatus.OK.value());
    }

    private Cookie createCookie(String key, String value, int maxAge) {

        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(false); // HTTPS에서만 전송 (개발 환경에서는 false로 변경)
        cookie.setPath("/");
        cookie.setHttpOnly(false); // JavaScript에서 접근 불가
        //cookie.setAttribute("SameSite", "None"); // CORS 환경에서 필수
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

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {

        response.setStatus(401);
    }
}
