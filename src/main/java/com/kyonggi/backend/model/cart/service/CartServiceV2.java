package com.kyonggi.backend.model.cart.service;

import com.kyonggi.backend.model.cart.dto.CartItemDto;
import com.kyonggi.backend.model.cart.dto.CartSummaryDto;
import com.kyonggi.backend.model.cart.entity.Cart;
import com.kyonggi.backend.model.item.Item;
import com.kyonggi.backend.model.item.OnlineItem;
import com.kyonggi.backend.model.item.dto.OnlineItemDto;
import com.kyonggi.backend.model.member.entity.Member;
import com.kyonggi.backend.model.member.entity.DailySavedAmount;
import com.kyonggi.backend.model.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CartServiceV2 {

    private final MemberRepository memberRepository;

    public OnlineItemDto addItemToCart(OnlineItemDto dto, Long memberId) {
        // 장바구니에 상품 추가하는 로직

        Member member = memberRepository.findById(memberId).orElseThrow();
        Cart cart = getActiveCart(member); // 기존 Cart 재사용 or 새로 생성
        log.info("cartName: {}", cart.getName());
        OnlineItem item = new OnlineItem();


        String cleanTitle = dto.getTitle().replaceAll("<[^>]*>", "");// HTML 태그 제거용 정규표현식
        String brand = dto.getBrand();
        String volume = dto.getVolume();

        StringBuilder nameBuilder = new StringBuilder();

        // 브랜드 && 용량 정보가 있으면 이름에 추가
        if (brand != null && !brand.isBlank()) nameBuilder.append(brand).append(" ");
        nameBuilder.append(cleanTitle);
        if (volume != null && !volume.isBlank()) nameBuilder.append(" ").append(volume);

        item.setName(nameBuilder.toString().trim());
        item.setPrice(dto.getPrice());
        item.setLink(dto.getLink());
        item.setImage(dto.getImage());
        item.setMallName(dto.getMallName());
        item.setBrand(dto.getBrand());
        item.setQuantity(dto.getQuantity());
        item.setVolume("N/A");
        item.setCompareItemPrice(dto.getCompareItemPrice());


        cart.addItem(item);
        memberRepository.save(member);

        return dto;
    }

    public void removeItemFromCart(List<CartItemDto> selectedItems, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Cart cart = member.getCartList().stream()
                .filter(Cart::isActive)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("활성화된 장바구니가 없습니다."));

        // 선택된 아이템 제거
        cart.getItemList().removeIf(item ->
                selectedItems.stream()
                        .anyMatch(selected -> selected.getId().equals(item.getId()))
        );

        memberRepository.save(member);
    }

    @Transactional
    public void removeCartFromHistory(List<CartSummaryDto> selectedCarts, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();

        List<Long> cartIdsToDelete = selectedCarts.stream()
                .map(CartSummaryDto::getCartId)
                .toList();

        List<Cart> targetCarts = member.getCartList().stream()
                .filter(cart -> !cart.isActive() && cartIdsToDelete.contains(cart.getId()))
                .toList();

        for (Cart cart : targetCarts) {
            int savedToRemove = cart.getItemList().stream()
                    .filter(item -> item instanceof OnlineItem)
                    .mapToInt(item -> {
                        OnlineItem online = (OnlineItem) item;
                        if (online.getCompareItemPrice() > 0) {
                            return (online.getCompareItemPrice() - online.getPrice()) * online.getQuantity();
                        }
                        return 0;
                    })
                    .sum();

            LocalDateTime createdAt = cart.getCreatedAt();
            int year = createdAt.getYear();
            int month = createdAt.getMonthValue();
            int day = createdAt.getDayOfMonth();

            Optional<DailySavedAmount> match = member.getMonthlySavedAmounts().stream()
                    .filter(d -> d.getYear() == year && d.getMonth() == month && d.getDay() == day)
                    .findFirst();

            match.ifPresent(daily -> {
                int current = daily.getSavedAmount();
                daily.setSavedAmount(Math.max(0, current - savedToRemove));
            });

            System.out.printf("🧾 장바구니 ID %d에서 %d원 절약금 차감 완료\n", cart.getId(), savedToRemove);
        }

        boolean removed = member.getCartList().removeIf(cart ->
                !cart.isActive() && cartIdsToDelete.contains(cart.getId())
        );

        System.out.println("🗑️ 삭제 성공 여부: " + removed);
        memberRepository.save(member);
    }



    public void completeCart(List<CartItemDto> selectedItems, int totalSavedAmount, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Cart activeCart = member.getCartList().stream()
                .filter(Cart::isActive)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("활성화된 장바구니가 없습니다."));

        activeCart.getItemList().removeIf(item ->
                selectedItems.stream().noneMatch(dto -> dto.getId().equals(item.getId()))
        );

        for (Item item : activeCart.getItemList()) {
            selectedItems.stream()
                    .filter(dto -> dto.getId().equals(item.getId()))
                    .findFirst()
                    .ifPresent(dto -> item.setQuantity(dto.getQuantity()));
        }

        LocalDateTime createdAt = activeCart.getCreatedAt();
        int year = createdAt.getYear();
        int month = createdAt.getMonthValue();
        int day = createdAt.getDayOfMonth();

        int totalConsumedAmount = selectedItems.stream()
                .mapToInt(item -> item.getPrice() * item.getQuantity())
                .sum();

        DailySavedAmount dailySaved = member.getMonthlySavedAmounts().stream()
                .filter(m -> m.getYear() == year && m.getMonth() == month && m.getDay() == day)
                .findFirst()
                .orElse(null);

        if (dailySaved == null) {
            dailySaved = DailySavedAmount.builder()
                    .year(year)
                    .month(month)
                    .day(day)
                    .savedAmount(totalSavedAmount)
                    .consumedAmount(totalConsumedAmount)
                    .member(member)
                    .build();
            member.getMonthlySavedAmounts().add(dailySaved);
        } else {
            dailySaved.setSavedAmount(dailySaved.getSavedAmount() + totalSavedAmount);
            dailySaved.setConsumedAmount(dailySaved.getConsumedAmount() + totalConsumedAmount);
        }

        activeCart.setActive(false);
        memberRepository.save(member);
    }




    private Cart getActiveCart(Member member) {
        return member.getCartList().stream()
                .filter(Cart::isActive) // 활성화된 카트 있나 확인
                .findFirst()
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    //String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                    newCart.setName("쇼핑카트");
                    newCart.setActive(true);
                    newCart.setCreatedAt(LocalDateTime.now());
                    member.addCart(newCart);
                    return newCart;
                });
    }

}
