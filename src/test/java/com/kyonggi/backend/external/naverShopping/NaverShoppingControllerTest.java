package com.kyonggi.backend.external.naverShopping;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NaverShoppingControllerTest {


    @Autowired
    private NaverClient naverClient;

    @Test
    public void searchTest() {
        var search = new ShoppingRequest();
        search.setQuery("바나나 우유");

        var result = naverClient.search(search);
        System.out.println(result);
    }

}