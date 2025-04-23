package com.kyonggi.backend.external.naverShopping;

import com.kyonggi.backend.model.item.dto.OnlineItemDto;
import com.kyonggi.backend.model.member.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shopping")
@RequiredArgsConstructor
public class NaverShoppingController {

    private final NaverShoppingService naverShoppingService;

    @PostMapping("/search")
    public NaverSearchResponseDto search(@RequestBody NaverSearchRequestDto condition) {
        return naverShoppingService.searchWithFilter(condition);
    }

    @PostMapping("/cart")
    public ResponseEntity<String> addToCart(@RequestBody OnlineItemDto dto) {
        Long memberId = getCurrentMemberId();
        naverShoppingService.addItemToCart(dto, memberId);
        return ResponseEntity.ok("장바구니에 담았습니다");
    }

    private Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getMember().getId();
    }



    @PostMapping("/compareMall")
    public List<Map<String, Object>> compareMall(@RequestBody List<NaverSearchRequestDto> conditions) {
        return naverShoppingService.compareMallName(conditions);
    }

}
