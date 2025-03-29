package com.kyonggi.backend.external.naverShopping;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class NaverClient {
    private String naverUrl = "https://openapi.naver.com/v1/search/shop.json";
    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String clientSecret;

    public ShoppingResponse search(ShoppingRequest request) {
        URI uri = UriComponentsBuilder.fromUriString(naverUrl)
                .queryParams(request.map())
                .build()
                .encode()
                .toUri();


        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);

        HttpEntity httpEntity = new HttpEntity<>(headers);


        ResponseEntity<ShoppingResponse> entity = new RestTemplate().exchange(
                uri,
                HttpMethod.GET,
                httpEntity,
                ShoppingResponse.class
        );

        return entity.getBody();
    }
}