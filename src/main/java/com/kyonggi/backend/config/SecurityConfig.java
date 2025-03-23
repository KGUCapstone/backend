package com.kyonggi.backend.config;


import com.kyonggi.backend.jwt.CustomLogoutFilter;
import com.kyonggi.backend.jwt.JWTFilter;
import com.kyonggi.backend.jwt.JWTUtil;
import com.kyonggi.backend.jwt.LoginFilter;
import com.kyonggi.backend.model.member.repository.RefreshRepository;
import com.kyonggi.backend.oauth2.CustomOAuth2UserService;
import com.kyonggi.backend.oauth2.CustomSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final AuthenticationConfiguration authenticationConfiguration;
    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomSuccessHandler customSuccessHandler;

    @Value("${spring.frontend.url}")
    private String frontendUrl;

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        //csrf disable
        http
                .csrf((auth) -> auth.disable())
                .formLogin((auth) -> auth.disable())
                .httpBasic((auth) -> auth.disable())

                //인증 설정
                .authorizeHttpRequests((auth) -> auth
                        //.requestMatchers("/join","/login","/logout" ).permitAll()
                        .requestMatchers("/api/join","/api/login","/api/logout" ,"/oauth2/**"
                        ,"/api/auth/**").permitAll()
                        .requestMatchers("/api/reissue").permitAll()
                        .requestMatchers("/admin").hasRole("ADMIN")
                        .anyRequest().authenticated())


                //filter 설정
                .addFilterBefore(new JWTFilter(jwtUtil), OAuth2LoginAuthenticationFilter.class)
                .addFilterBefore(new CustomLogoutFilter(jwtUtil, refreshRepository), LogoutFilter.class)
                .addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), jwtUtil, refreshRepository),
                        UsernamePasswordAuthenticationFilter.class)

                //세션 설정
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))


                .oauth2Login((oauth2) -> oauth2
                        .userInfoEndpoint((userInfoEndpointConfig) -> userInfoEndpointConfig
                                .userService(customOAuth2UserService))
                        .successHandler(customSuccessHandler)
                        .authorizationEndpoint(authorizationEndpointConfig ->
                                authorizationEndpointConfig.baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(redirectionEndpointConfig ->
                                redirectionEndpointConfig.baseUri("/login/oauth2/code/*"))
                )

                //cors 설정
                .cors((corsCustomizer -> corsCustomizer.configurationSource(new CorsConfigurationSource() {

                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {

                        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                        CorsConfiguration config = new CorsConfiguration();

                        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                        config.setAllowedOrigins(Collections.singletonList(frontendUrl));
                        config.setAllowCredentials(true);
                        config.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization"));
                        config.setExposedHeaders(Arrays.asList("access", "Authorization")); // 프론트엔드에서 access 헤더를 읽을 수 있도록 설정
                        config.setMaxAge(3600L);


                        source.registerCorsConfiguration("/**", config);

                        return config;
                    }
                })));

        return http.build();
    }
}
