package com.kyonggi.backend.model.cart.controller;

import com.kyonggi.backend.jwt.JWTUtil;
import com.kyonggi.backend.model.cart.dto.*;
import com.kyonggi.backend.model.cart.service.CartService;
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
public class CartController {

    private final CartService cartService;
    private final JWTUtil jwtUtil;
    private final MemberRepository memberRepository;

    // 장바구니 조회
    @GetMapping("/get")
    public ResponseEntity<CartResponseDto> getCart(@RequestHeader("Authorization") String token,
                                                   @RequestBody CartItemListRequestDto requestDto) {
        Long memberId = extractMemberIdFromToken(token);
        return ResponseEntity.ok(cartService.getActiveCart(requestDto));
    }

    // 장바구니에 상품 추가
    @PostMapping("/add")
    public ResponseEntity<Void> addToCart(@RequestHeader("Authorization") String token,
                                          @RequestBody CartAddItemRequestDto request) {
        Long memberId = extractMemberIdFromToken(token);
        cartService.addItemToCart(request);
        return ResponseEntity.ok().build();
    }

    // 장바구니 상품 삭제
    @DeleteMapping("/remove")
    public ResponseEntity<Void> removeItem(@RequestHeader("Authorization") String token,
                                           @RequestBody CartRemoveItemRequestDto requestDto) {
        Long memberId = extractMemberIdFromToken(token);
        cartService.removeItemFromCart(requestDto);
        return ResponseEntity.noContent().build();
    }

    // 장바구니 비활성화 (구매 완료 처리)
    @PostMapping("/archive")
    public ResponseEntity<Void> archiveCart(@RequestHeader("Authorization") String token,
                                            @RequestBody DeactivateCartDto deactivateCartDto) {
        Long memberId = extractMemberIdFromToken(token);
        cartService.archiveCurrentCart(deactivateCartDto);

        return ResponseEntity.ok().build();
    }

    // 기록 조회
    @GetMapping("/history")
    public ResponseEntity<CartResponseDto> getHistory(@RequestHeader("Authorization") String token,
                                                      @RequestBody HistoryRequestDto historyRequestDto) {
        Long memberId = extractMemberIdFromToken(token);
        return ResponseEntity.ok(cartService.getHistoryCarts(historyRequestDto));
    }

    // 토큰에서 memberId 추출
    private Long extractMemberIdFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            String username = jwtUtil.getUsername(token);
            Long memberId = memberRepository.findByUsername(username).get().getId();
            return memberId;

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid token");
        }
    }

}