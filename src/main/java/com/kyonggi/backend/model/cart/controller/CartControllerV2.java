package com.kyonggi.backend.model.cart.controller;


import com.kyonggi.backend.jwt.JWTUtil;
import com.kyonggi.backend.model.cart.dto.CartItemDto;
import com.kyonggi.backend.model.cart.dto.CartSummaryDto;
import com.kyonggi.backend.model.cart.entity.Cart;
import com.kyonggi.backend.model.cart.repository.CartRepository;
import com.kyonggi.backend.model.cart.service.CartServiceV2;
import com.kyonggi.backend.model.item.Item;
import com.kyonggi.backend.model.item.OnlineItem;
import com.kyonggi.backend.model.item.dto.OnlineItemDto;
import com.kyonggi.backend.model.member.entity.Member;
import com.kyonggi.backend.model.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")

public class CartControllerV2 {
    private final CartServiceV2 cartService;
    private final JWTUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final CartRepository cartRepository;

    @PostMapping("/add")
    public ResponseEntity<OnlineItemDto> add(@RequestHeader(value = "Authorization", required = true) String token, @RequestBody OnlineItemDto onlineItemDto) {
        Long memberId = extractMemberIdFromToken(token);
        return ResponseEntity.ok(cartService.addItemToCart(onlineItemDto, memberId));
    }

    @PostMapping("/show")
    public ResponseEntity<List<CartItemDto>> showCart(@RequestHeader(value = "Authorization", required = true) String token) {
        Long memberId = extractMemberIdFromToken(token);
        Member member = memberRepository.findById(memberId).get(); // 예외 처리 추가
        List<OnlineItem> items= cartRepository.findActiveCartByMember(member);

        List<CartItemDto> result = items.stream()
                .map(item -> {
                    CartItemDto dto = new CartItemDto();
                    dto.setId(item.getId());
                    dto.setTitle(item.getName());
                    dto.setPrice(item.getPrice());
                    dto.setLink(item.getLink());
                    dto.setImage(item.getImage());
                    dto.setMallName(item.getMallName());
                    dto.setBrand(item.getBrand());
                    dto.setVolume(item.getVolume());
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    // 장바구니 비활성화 (구매 완료 처리)
    @PostMapping("/complete")
    public ResponseEntity<Void> completeCart(
            @RequestHeader(value = "Authorization", required = true) String token,
            @RequestBody List<CartItemDto> selectedItems) {

        Long memberId = extractMemberIdFromToken(token);
        cartService.completeCart(selectedItems, memberId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/history")
    public ResponseEntity<List<CartSummaryDto>> showCompletedCarts(
            @RequestHeader(value = "Authorization", required = true) String token) {

        Long memberId = extractMemberIdFromToken(token);
        Member member = memberRepository.findById(memberId).orElseThrow();

        List<Cart> carts = member.getCartList().stream()
                .filter(cart -> !cart.isActive())
                .toList();

        List<CartSummaryDto> result = carts.stream()
                .map(cart -> new CartSummaryDto(cart.getId(), cart.getName()))
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/history/{cartId}")
    public ResponseEntity<List<CartItemDto>> showCartItemsById(@PathVariable(name="cartId") Long cartId) {
        Cart cart = cartRepository.findById(cartId).orElseThrow();

        List<Item> items = cart.getItemList();

        List<CartItemDto> result = items.stream()
                .filter(item -> item instanceof OnlineItem)
                .map(item -> {
                    OnlineItem online = (OnlineItem) item;
                    CartItemDto dto = new CartItemDto();
                    dto.setId(online.getId());
                    dto.setTitle(online.getName());
                    dto.setPrice(online.getPrice());
                    dto.setLink(online.getLink());
                    dto.setImage(online.getImage());
                    dto.setMallName(online.getMallName());
                    dto.setBrand(online.getBrand());
                    dto.setVolume(online.getVolume());
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(result);
    }





    // 토큰에서 memberId 추출
    private Long extractMemberIdFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        try {
            String username = jwtUtil.getUsername(token);
            Long memberId = memberRepository.findByUsername(username).get().getId();
            return memberId;

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid token");
        }
    }

    //// test 임의로 "qwer1234"로 memberId를 가져옴 >>
//    private Long extractMemberIdFromToken(String token) {
//
//        try {
//            return memberRepository.findByUsername("qwer1234").get().getId();
//        } catch (Exception e) {
//            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
//        }
//    }

}
