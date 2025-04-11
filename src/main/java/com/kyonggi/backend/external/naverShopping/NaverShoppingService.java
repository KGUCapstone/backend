package com.kyonggi.backend.external.naverShopping;

import com.kyonggi.backend.model.cart.entity.Cart;
import com.kyonggi.backend.model.item.OnlineItem;
import com.kyonggi.backend.model.item.dto.OnlineItemDto;
import com.kyonggi.backend.model.item.repsoitory.OnlineItemRepository;
import com.kyonggi.backend.model.member.entity.Member;
import com.kyonggi.backend.model.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NaverShoppingService {
    private final NaverClient naverClient;
    private final OnlineItemRepository onlineItemRepository;
    private final MemberRepository memberRepository;


    @Transactional
    public void addItemToCart(OnlineItemDto dto, Long memberId) {
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
    }

    private Cart getActiveCart(Member member) {
        return member.getCartList().stream()
                .filter(Cart::isActive) // 활성화된 카트 있나 확인
                .findFirst()
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                    newCart.setName("쇼핑카트-" + today);
                    newCart.setActive(true);
                    member.addCart(newCart);
                    return newCart;
                });
    }

    public ShoppingResponse search(String query) {
        ShoppingRequest shoppingRequest = new ShoppingRequest();
        shoppingRequest.setQuery(query);

        return naverClient.search(shoppingRequest);
    }

    public NaverSearchResponseDto searchWithFilter(NaverSearchRequestDto condition) {

        String title = condition.getTitle();
        int price = condition.getPrice();
        String volume = condition.getVolume() != null ? condition.getVolume() : "";
        String brand = condition.getBrand() != null ? condition.getBrand() : "";


        String query = String.join(" ", brand, title, volume);

        ShoppingRequest request = new ShoppingRequest();
        log.info("query: {}", query);
        request.setQuery(query);

        ShoppingResponse response = naverClient.search(request);
        List<ShoppingResponse.ShoppingItem> items = response.getItems();

        // 아이템이 없으면 빈 결과 반환
        if (items == null || items.isEmpty()) {
            return new NaverSearchResponseDto(List.of());
        }

        // 가격 범위 계산 
        int lower = (int) (price * 0.5);
        int upper = (int) (price * 1.5);

        //System.out.println("items = " + items);
        // 후처리 필터링
        List<ShoppingResponse.ShoppingItem> list = items.stream()
                .filter(item -> {
                    if (item.getTitle() == null)
                        return false;

                    // HTML 태그 제거
                    String cleanTitle = item.getTitle().replaceAll("<[^>]*>", "");
                    item.setTitle(cleanTitle);

                    if (item.getLprice() >= lower && item.getLprice() <= upper)
                        return true;

                    return false;
                })
                .toList();

        //System.out.println("list = " + list);
        NaverSearchResponseDto result = new NaverSearchResponseDto();
        result.setItems(list);
        return result;

    }


}
