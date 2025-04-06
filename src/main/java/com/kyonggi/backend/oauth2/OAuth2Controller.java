package com.kyonggi.backend.oauth2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
public class OAuth2Controller {
    @Value("${spring.backend.url}")
    private String backendUrl;

    @GetMapping("/naver")
    public ResponseEntity<?> getNaverLoginUrl() {
        String naverAuthUrl =
                backendUrl+"/oauth2/authorization/naver";

        return ResponseEntity.ok(naverAuthUrl);
    }
    @GetMapping("/google")
    public ResponseEntity<?> getGoogleLoginUrl() {
        String googleAuthUrl =
                backendUrl+"/oauth2/authorization/google";

        return ResponseEntity.ok(googleAuthUrl);
    }


    @GetMapping("/token")
    public ResponseEntity<?> getAccessToken(@CookieValue(value = "access_token", required = false) String accessToken) {
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access token not found");
        }
        return ResponseEntity.ok(Collections.singletonMap("accessToken", accessToken));
    }
}
