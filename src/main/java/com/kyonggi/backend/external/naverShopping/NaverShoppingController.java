package com.kyonggi.backend.external.naverShopping;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shopping")
@RequiredArgsConstructor
public class NaverShoppingController {

    private final NaverClient naverClient;

    @GetMapping("/search/{query}")
    public ShoppingResponse search(@PathVariable(name = "query") String query) {
        ShoppingRequest shoppingRequest = new ShoppingRequest();
        shoppingRequest.setQuery(query);

        return naverClient.search(shoppingRequest);
    }
}
