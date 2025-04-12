package com.kyonggi.backend.model.cart.service;

import com.kyonggi.backend.model.cart.dto.CartItemDto;
import com.kyonggi.backend.model.cart.entity.Cart;
import com.kyonggi.backend.model.item.OnlineItem;
import com.kyonggi.backend.model.item.dto.OnlineItemDto;
import com.kyonggi.backend.model.member.entity.Member;
import com.kyonggi.backend.model.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        item.setVolume("N/A");


        cart.addItem(item);
        memberRepository.save(member);

        return dto;
    }

    public void removeItemFromCart(Long itemId, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Cart cart = member.getCartList().stream()
                .filter(Cart::isActive)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("활성화된 장바구니가 없습니다."));

        // 아이템 찾기
        OnlineItem itemToRemove = cart.getItemList().stream()
                .filter(i -> i.getId().equals(itemId))
                .map(i -> (OnlineItem) i)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("해당 상품이 존재하지 않습니다."));

        // 삭제
        cart.getItemList().remove(itemToRemove);
    }


//    public void completeCart(List<CartItemDto> selectedItems, Long memberId) {
//        Member member = memberRepository.findById(memberId).orElseThrow();
//
//        // 기존 활성화된 장바구니 비활성화
//        member.getCartList().forEach(cart -> {
//            if (cart.isActive()) {
//                cart.setActive(false);
//            }
//        });
//
//        // 새 Cart 생성
//        Cart completedCart = new Cart();
//        completedCart.setActive(false); // 이건 비활성화된 완료된 Cart
//        completedCart.setName("완료된 장바구니 " + LocalDate.now());
//        member.addCart(completedCart);
//
//        // 선택된 아이템 저장
//        for (CartItemDto dto : selectedItems) {
//            OnlineItem item = new OnlineItem();
//            item.setName(dto.getTitle());
//            item.setPrice(dto.getPrice());
//            item.setLink(dto.getLink());
//            item.setImage(dto.getImage());
//            item.setMallName(dto.getMallName());
//            item.setBrand(dto.getBrand());
//            item.setVolume(dto.getVolume());
//            completedCart.addItem(item);
//        }
//
//        memberRepository.save(member); // Cart와 Item 저장됨
//    }

    public void completeCart(List<CartItemDto> selectedItems, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();

        // 기존 활성 Cart 조회
        Cart activeCart = member.getCartList().stream()
                .filter(Cart::isActive)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("활성화된 장바구니가 없습니다."));

        // 선택된 아이템 외 나머지 제거
        activeCart.getItemList().removeIf(item ->
                selectedItems.stream().noneMatch(dto -> dto.getId().equals(item.getId()))
        );

        // 상태 비활성화
        activeCart.setActive(false);

        memberRepository.save(member); // 변경 사항 저장
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
                    member.addCart(newCart);
                    return newCart;
                });
    }

}
