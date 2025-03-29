package com.kyonggi.backend.model.member.entity;

import com.kyonggi.backend.model.cart.Cart;
import com.kyonggi.backend.model.item.OnlineItem;
import com.kyonggi.backend.model.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional

class MemberTest {
    @Autowired
    private MemberRepository memberRepository;

    @PersistenceContext
    private EntityManager em;

    @Test
    void member_cart_item_mappingTest() {
        // 회원 생성
        Member member = new Member();
        member.setUsername("testuser");
        member.setPassword("1234");
        member.setEmail("test@abc.com");
        member.setRole("USER");
        member.setName("tester");

        // 장바구니 생성
        Cart cart = new Cart();
        cart.setName("cart");

        // 양방향 연관관계 설정
        member.addCart(cart); // member.set → cart.set 동시에 처리됨

        // 온라인 아이템 생성
        OnlineItem item = new OnlineItem();
        item.setName("malk");
        item.setPrice(1500);
        item.setVolume("500ml");
        item.setLink("https://example.com");
        item.setImage("https://example.com/image.jpg");
        item.setMallName("testMall");
        item.setBrand("오뚜기");

        // 양방향 연관관계 설정
        cart.addItem(item); // cart.set → item.set 동시에 처리됨

        //저장 (Cascade로 전부 저장됨)
        memberRepository.save(member);

        //flush/clear → DB 반영 + 1차 캐시 초기화
        em.flush();
        em.clear();

        // 검증
        Member savedMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(savedMember.getRole()).isEqualTo("USER");
        assertThat(savedMember.getCartList()).hasSize(1);

        Cart savedCart = savedMember.getCartList().get(0);
        assertThat(savedCart.getItemList()).hasSize(1);

        OnlineItem savedItem = (OnlineItem) savedCart.getItemList().get(0);
        assertThat(savedItem.getName()).isEqualTo("테스트우유");
        assertThat(savedItem.getCart().getName()).isEqualTo("테스트카트");
        assertThat(savedItem.getMember().getUsername()).isEqualTo("testuser");
    }

}