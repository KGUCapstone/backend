package com.kyonggi.backend.external.naverShopping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyonggi.backend.model.cart.entity.Cart;
import com.kyonggi.backend.model.item.Item;
import com.kyonggi.backend.model.item.OnlineItem;
import com.kyonggi.backend.model.item.dto.OnlineItemDto;
import com.kyonggi.backend.model.member.dto.CustomUserDetails;
import com.kyonggi.backend.model.member.entity.Member;
import com.kyonggi.backend.model.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional

class NaverShoppingControllerTest {


    @Autowired
    private NaverClient naverClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private NaverShoppingService shoppingService;

    @Autowired
    private ObjectMapper objectMapper;

    private Member testMember;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void setup() {
        Optional<Member> existing = memberRepository.findByUsername("mockuser");
        if (existing.isPresent()) {
            testMember = existing.get();
        } else {
            testMember = new Member();
            testMember.setUsername("mockuser");
            testMember.setPassword("1234");
            testMember.setEmail("mock@abc.com");
            testMember.setRole("ROLE_USER");
            testMember.setName("tester");
            memberRepository.save(testMember);
        }

        // SecurityContext에 로그인한 사용자 등록
        CustomUserDetails userDetails = new CustomUserDetails(testMember);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteByUsername("mockuser");
    }

    @Test
    public void searchTest() {
        var search = new ShoppingRequest();
        search.setQuery("바나나 우유");

        var result = naverClient.search(search);
        System.out.println(result);
    }


    @Test
    void addItemIntoCart() {

        Cart cart = new Cart();
        cart.setName("cart");
        testMember.addCart(cart);

        // 검색
        ShoppingRequest request = new ShoppingRequest();
        request.setQuery("바나나 우유 240ml");
        ShoppingResponse response = naverClient.search(request);

        assertThat(response.getItems()).isNotEmpty();


        //검색 결과 -> OnlineItem으로 변환 후 Cart에 추가
        response.getItems().stream()
                .limit(5) // 너무 많으면 테스트 느려지니 5개 정도만
                .forEach(item -> {
                    OnlineItem onlineItem = new OnlineItem();
                    onlineItem.setName(item.getTitle().replaceAll("<[^>]*>", ""));
                    onlineItem.setPrice(item.getLprice());
                    onlineItem.setLink(item.getLink());
                    onlineItem.setImage(item.getImage());
                    onlineItem.setMallName(item.getMallName());
                    onlineItem.setBrand(item.getBrand());
                    onlineItem.setVolume("N/A");

                    cart.addItem(onlineItem);
                });

        // 저장
        memberRepository.save(testMember);


        // flush & clear → DB에서 다시 조회
        em.flush();
        em.clear();

        // 검증
        Member savedMember = memberRepository.findById(testMember.getId()).orElseThrow();
        Cart savedCart = savedMember.getCartList().get(0);
        List<Item> itemList = savedCart.getItemList();

        assertThat(itemList).isNotEmpty();
        System.out.println("카트명: " + savedCart.getName());
        itemList.forEach(i -> {
            System.out.println(" 상품명: " + i.getName() + " / 가격: " + i.getPrice());
        });
    }

    @Test
    void searchAndAddTheCheapestItemIntoCart() {
        // 1. 검색
        ShoppingRequest request = new ShoppingRequest();
        request.setQuery("바나나 우유 빙그레 ml");
        ShoppingResponse response = naverClient.search(request);

        List<ShoppingResponse.ShoppingItem> items = response.getItems();
        assertThat(items).isNotEmpty();

        // 디버깅용 전체 출력
        System.out.println("전체 검색 결과:");
        items.forEach(i -> System.out.println("- " + i.getTitle() + " / " + i.getLprice()));


        // 그 중 가장 저렴한 상품 선택
        ShoppingResponse.ShoppingItem cheapest = items.stream()
                .min(Comparator.comparingInt(ShoppingResponse.ShoppingItem::getLprice))
                .orElseThrow();

        System.out.println("가장 저렴한 상품: " + cheapest.getTitle() + " / 가격: " + cheapest.getLprice());

        // DTO 변환 후 장바구니 저장
        OnlineItemDto dto = toDto(cheapest);
        shoppingService.addItemToCart(dto, testMember.getId());

        // flush & clear → DB 다시 조회
        em.flush();
        em.clear();

        // 검증: 카트에 담긴 아이템 확인
        Member savedMember = memberRepository.findById(testMember.getId()).orElseThrow();
        Cart savedCart = savedMember.getCartList().get(0);
        assertThat(savedCart.getItemList()).hasSize(1);

        Item savedItem = savedCart.getItemList().get(0);
        System.out.println(" 저장된 상품명: " + savedItem.getName());
        assertThat(savedItem.getPrice()).isEqualTo(cheapest.getLprice());
    }

    private OnlineItemDto toDto(ShoppingResponse.ShoppingItem item) {
        OnlineItemDto dto = new OnlineItemDto();
        String cleanTitle = item.getTitle().replaceAll("<[^>]*>", "");

        dto.setTitle(cleanTitle);
        dto.setPrice(item.getLprice());
        dto.setLink(item.getLink());
        dto.setImage(item.getImage());
        dto.setMallName(item.getMallName());
        dto.setBrand(item.getBrand());

        // 용량 추출
        dto.setVolume(extractVolume(cleanTitle));
        return dto;
    }

    private String extractVolume(String text) {
        Matcher matcher = Pattern.compile("(\\d+\\s?)", Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? matcher.group(1).replaceAll("\\s+", "") : "N/A";
    }

    @Test
    void conditionBasedSearch() throws Exception {
        NaverSearchRequestDto dto = new NaverSearchRequestDto();
        dto.setTitle("바나나우유");
        dto.setPrice(1300);
        dto.setVolume("240ml");
        dto.setBrand("빙그레");

        String jsonRequest = objectMapper.writeValueAsString(dto);

        MockHttpServletRequestBuilder request = post("/api/shopping/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest);

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("검색 결과:\n" + content);

        NaverSearchResponseDto resultDto = objectMapper.readValue(content, NaverSearchResponseDto.class);
        List<ShoppingResponse.ShoppingItem> items = resultDto.getItems();
        assertThat(items).isNotEmpty();
    }

    @Test
    void conditionBasedSearchAndAddItemIntoCart() throws Exception {
        NaverSearchRequestDto dto = new NaverSearchRequestDto();
        dto.setTitle("바나나우유");
        dto.setPrice(1300);
        dto.setVolume("240ml");
        dto.setBrand("빙그레");

        String jsonRequest = objectMapper.writeValueAsString(dto);

        // 검색 API 요청
        MvcResult result = mockMvc.perform(post("/api/shopping/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andReturn();

        // 응답 파싱
        String content = result.getResponse().getContentAsString();
        NaverSearchResponseDto resultDto = objectMapper.readValue(content, NaverSearchResponseDto.class);
        List<ShoppingResponse.ShoppingItem> items = resultDto.getItems();

        assertThat(items).isNotEmpty();

        // 최저가 선택
        ShoppingResponse.ShoppingItem cheapest = items.stream()
                .min(Comparator.comparingInt(ShoppingResponse.ShoppingItem::getLprice))
                .orElseThrow();

        System.out.println("선택된 상품: " + cheapest.getTitle() + " / " + cheapest.getLprice());

        // DTO로 변환 후 저장
        OnlineItemDto onlineItemDto = toDto(cheapest);
        shoppingService.addItemToCart(onlineItemDto, testMember.getId());

        em.flush();
        em.clear();

        // 저장된 카트 검증
        Member saved = memberRepository.findById(testMember.getId()).orElseThrow();
        Cart cart = saved.getCartList().get(0);
        assertThat(cart.getItemList()).hasSize(1);

        OnlineItem item = (OnlineItem) cart.getItemList().get(0);
        System.out.println("카트에 저장된 상품명: " + item.getName());

        assertThat(item.getPrice()).isEqualTo(cheapest.getLprice());
    }
}