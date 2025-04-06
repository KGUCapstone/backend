package com.kyonggi.backend.external.naverShopping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShoppingRequest {
    private String query = "";
    private Integer display = 100;
    private String sort = "sim"; // 정확도 기준
    //private String sort = "asc"; // 최저가 정렬


    public MultiValueMap<String, String> map() {
        LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("query", query);
        map.add("display", String.valueOf(display));
        map.add("sort", sort);
        return map;
    }
}
