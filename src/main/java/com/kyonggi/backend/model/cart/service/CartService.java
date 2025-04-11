package com.kyonggi.backend.model.cart.service;

import com.kyonggi.backend.model.cart.dto.*;
import com.kyonggi.backend.model.cart.entity.Cart;
import com.kyonggi.backend.model.cart.repository.CartRepository;
import com.kyonggi.backend.model.item.Item;
import com.kyonggi.backend.model.item.OnlineItem;
import com.kyonggi.backend.model.item.dto.OnlineItemDto;
import com.kyonggi.backend.model.item.repsoitory.OnlineItemRepository;
import com.kyonggi.backend.model.member.entity.Member;
import com.kyonggi.backend.model.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final MemberRepository memberRepository;
    private final OnlineItemRepository onlineItemRepository;
    private final CartRepository cartRepository;


    //장바구니 조회
    public CartResponseDto getActiveCart(CartItemListRequestDto requestDto) {
        Member member = memberRepository.findById(requestDto.getUserId()).orElseThrow();

        Cart cart = member.getCartList().stream()
                .filter(Cart::isActive)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("활성 장바구니가 없습니다."));

        return convertToDto(cart);
    }

    //테스트용
//    public CartResponseDto getActiveCart(Long memberId) {
//        Member member = memberRepository.findById(memberId).orElseThrow();
//
//        Cart cart = member.getCartList().stream()
//                .filter(Cart::isActive)
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("활성 장바구니가 없습니다."));
//
//        return convertToDto(cart);
//    }

    //장바구니 상품 목록 조회 (테스트용?)
    @Transactional(readOnly = true)
    public List<Item> getItemsInCart(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("장바구니를 찾을 수 없습니다."));

        return cart.getItemList(); // Lazy loading 가능: 트랜잭션 안이기 때문에
    }

    
    //장바구니에 상품 추가
    @Transactional
    public CartAddItemResponseDto addItemToCart(CartAddItemRequestDto request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        OnlineItemDto dto = request.getItem();

        // 기존 장바구니 or 새 장바구니 가져오기
        Cart cart = getOrCreateActiveCart(member);

        // OnlineItem 생성 및 정제
        OnlineItem item = createOnlineItem(dto);

        // 장바구니에 추가
        cart.addItem(item);

        return new CartAddItemResponseDto(
                item.getId(),
                item.getName(),
                item.getPrice(),
                cart.getItemList().size()
        );
    }

    //장바구니 생성 or 가져오기
    private Cart getOrCreateActiveCart(Member member) {
        return member.getCartList().stream()
                .filter(Cart::isActive)
                .findFirst()
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setName("장바구니-" + LocalDate.now());
                    newCart.setActive(true);
                    member.addCart(newCart); // 연관관계 설정
                    return newCart;
                });
    }

    //상품 이름 가공
    private OnlineItem createOnlineItem(OnlineItemDto dto) {
        OnlineItem item = new OnlineItem();

        // 이름 정제 (HTML 태그 제거 + 브랜드, 용량 포함)
        String cleanTitle = dto.getTitle().replaceAll("<[^>]*>", "");
        StringBuilder nameBuilder = new StringBuilder();

        if (dto.getBrand() != null && !dto.getBrand().isBlank()) {
            nameBuilder.append(dto.getBrand()).append(" ");
        }

        nameBuilder.append(cleanTitle);

        if (dto.getVolume() != null && !dto.getVolume().isBlank()) {
            nameBuilder.append(" ").append(dto.getVolume());
        }

        item.setName(nameBuilder.toString().trim());

        // 기타 필드 설정
        item.setPrice(dto.getPrice());
        item.setMallName(dto.getMallName());
        item.setLink(dto.getLink());
        item.setImage(dto.getImage());
        item.setBrand(dto.getBrand());
        item.setVolume(dto.getVolume() != null ? dto.getVolume() : "N/A");

        item = onlineItemRepository.save(item);

        return item;
    }

    //장바구니 상품 삭제
    @Transactional
    public void removeItemFromCart(CartRemoveItemRequestDto requestDto) {
        Member member = memberRepository.findById(requestDto.getMemberId())
                .orElseThrow(() -> new RuntimeException("해당 회원을 찾을 수 없습니다."));

        // 활성화된 장바구니 가져오기
        Cart cart = member.getCartList().stream()
                .filter(Cart::isActive)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("활성화된 장바구니가 없습니다."));

        // 삭제할 아이템 찾기
        Item itemToRemove = cart.getItemList().stream()
                .filter(item -> item.getId().equals(requestDto.getItemId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("해당 상품이 장바구니에 없습니다."));

        // 삭제
        cart.getItemList().remove(itemToRemove);
    }


    //장바구니 비활성화 처리
    public void archiveCurrentCart(DeactivateCartDto deactivateCartDto) {
        Member member = memberRepository.findById(deactivateCartDto.getUserId()).orElseThrow();

        // 현재 활성 장바구니 가져오기
        Cart activeCart = member.getCartList().stream()
                .filter(Cart::isActive)
                .findFirst()
                .orElseThrow();

        // 기록용 장바구니 찾기 or 없으면 새로 생성
        Cart historyCart = member.getCartList().stream()
                .filter(c -> !c.isActive())
                .findFirst()
                .orElseGet(() -> {
                    Cart newHistory = new Cart();
                    newHistory.setName("주문이력");
                    newHistory.setActive(false);
                    member.addCart(newHistory);
                    return newHistory;
                });

        // 현재 장바구니의 모든 아이템을 기록용 장바구니로 이동 (현재 장바구니는 비워짐)
        for (Item item : activeCart.getItemList()) {
            item.setCart(historyCart);
            historyCart.addItem(item);
        }

        // 현재 장바구니 비우기 and 비활성화
//        activeCart.getItemList().clear();
//        activeCart.setActive(false);

        member.getCartList().remove(activeCart);
        cartRepository.delete(activeCart);
    }


    // 과거 기록 조회
    public CartResponseDto getHistoryCarts(HistoryRequestDto historyRequestDto) {

        Member member = memberRepository.findById(historyRequestDto.getMemberId()).orElseThrow();

        Cart historyCart = member.getCartList().stream()
                .filter(cart -> !cart.isActive())
                .findFirst()
                .orElseThrow();

        return convertToDto(historyCart);
    }

    // 장바구니 DTO 변환
    private CartResponseDto convertToDto(Cart cart) {
        List<CartItemDto> itemDtos = cart.getItemList().stream().map(item -> {
            CartItemDto dto = new CartItemDto();
            dto.setId(item.getId());
            dto.setName(item.getName());
            dto.setPrice(item.getPrice());
//          dto.setMallName(item.getMallName());
//          dto.setImage(item.getImage());
            return dto;
        }).toList();

        int count = itemDtos.size();

        CartResponseDto dto = new CartResponseDto();
        dto.setCartId(cart.getId());
        dto.setUserName(cart.getName());
        dto.setCount(count);
        dto.setItems(itemDtos);
        dto.setActive(cart.isActive());

        return dto;
    }

}

