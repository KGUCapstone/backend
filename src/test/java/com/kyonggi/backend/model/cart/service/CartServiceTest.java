package com.kyonggi.backend.model.cart.service;

import com.kyonggi.backend.model.cart.dto.CartAddItemRequestDto;
import com.kyonggi.backend.model.cart.dto.CartRemoveItemRequestDto;
import com.kyonggi.backend.model.cart.dto.CartResponseDto;
import com.kyonggi.backend.model.cart.dto.DeactivateCartDto;
import com.kyonggi.backend.model.cart.entity.Cart;
import com.kyonggi.backend.model.cart.repository.CartRepository;
import com.kyonggi.backend.model.item.Item;
import com.kyonggi.backend.model.item.dto.OnlineItemDto;
import com.kyonggi.backend.model.member.entity.Member;
import com.kyonggi.backend.model.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

    @SpringBootTest
    //@Transactional
    class CartServiceTest {

        @Autowired
        private CartService cartService;

        @Autowired
        private MemberRepository memberRepository;
        @Autowired
        private CartRepository cartRepository;


        // 장바구니 생성 and 상품 추가
        @Test
        void addItemToCart_Test() {
            // 🔹 given: 테스트용 Member 저장
            Member member = memberRepository.findByUsername("testuser").orElse(null);
            if (member == null) {
                member = new Member();
                member.setUsername("testuser");
                member.setPassword("1234");
                member.setEmail("test@example.com");
                member.setName("테스트유저");
                member.setRole("ROLE_USER");
                member = memberRepository.save(member);
            }

            // 🔹 given: 더미 아이템 생성
            OnlineItemDto itemDto = new OnlineItemDto();
            itemDto.setTitle("허니버터칩");
            itemDto.setPrice(12345);
            itemDto.setMallName("테스트몰");
            itemDto.setLink("https://example.com/product");
            itemDto.setImage("https://example.com/image.jpg");
            itemDto.setBrand("빙그레");
            itemDto.setVolume("300ml");

            // 🔸 when: 장바구니에 아이템 추가
            CartAddItemRequestDto request = new CartAddItemRequestDto();
            request.setMemberId(member.getId());
            request.setItem(itemDto);

            cartService.addItemToCart(request);

            // 🔸 when: 장바구니에 아이템 추가
            OnlineItemDto item2 = new OnlineItemDto();
            item2.setTitle("포카칩");
            item2.setPrice(2000);
            item2.setBrand("서울우유");
            item2.setVolume("400ml");

            CartAddItemRequestDto request2 = new CartAddItemRequestDto();
            request2.setMemberId(member.getId());
            request2.setItem(item2);
            cartService.addItemToCart(request2);


//            // 🔹 then: 장바구니에 잘 들어갔는지 확인
//            CartResponseDto cart = cartService.getActiveCart(member.getId());
//            assertNotNull(cart);
//            assertEquals(2, cart.getItems().size());
//            assertEquals("브랜드 테스트 상품 500ml", cart.getItems().get(0).getName());
//            assertEquals("브랜드 상품 2 200ml", cart.getItems().get(1).getName());
    }


        //장바구니 상품 목록 조회
        @Test
        @Transactional
        void printCartItemList_Test (){
        // 🟢 테스트용 Cart ID (테스트 전 미리 하나 저장해두거나 하드코딩)
        Long cartId = 10L;
        List<Item> items = cartService.getItemsInCart(cartId);

        // 🔸 장바구니 조회
        Cart savedCart = cartRepository.findById((cartId))
                .orElseThrow(() -> new RuntimeException("장바구니를 찾을 수 없습니다."));

        // 🔹 상품 목록 조회
        System.out.println("🛒 장바구니에 담긴 상품 수: " + items.size());

        for (Item item : items) {
            System.out.println("📦 상품명: " + item.getName() + ", 가격: " + item.getPrice());
        }
    }


        //장바구니 상품 삭제
        @Test
        void removeItemFromCart_Test() {

            Long memberId = 13L;    //해당 값은 각자 DB에 맞춰 하드코딩해서 테스트

            CartResponseDto cart = cartService.getActiveCart(13L);
            Long itemId = cart.getItems().get(0).getId();

            // when: 삭제 요청
            CartRemoveItemRequestDto removeDto = new CartRemoveItemRequestDto();
            removeDto.setMemberId(13L);
            removeDto.setItemId(itemId);

            cartService.removeItemFromCart(removeDto);

            // then: 장바구니에 상품 없어야 함
//            CartResponseDto updatedCart = cartService.getActiveCart(member.getId());
//            assertEquals(0, updatedCart.getItems().size());
        }


            //장바구니 상품 기록으로 넘기기
            @Test
//          @Transactional
            void archiveCart_existingCartMovesItemsToHistory() {
                // ✅ 전제: DB에 이미 존재하는 memberId를 지정
                Long memberId = 13L; // 실제 존재하는 값으로 바꿔야 함
                DeactivateCartDto deactivateCartDto;
                Member member = memberRepository.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("테스트용 멤버가 없습니다."));

                // 🔹 archive 수행
                cartService.archiveCurrentCart(13L);
                }
        }
