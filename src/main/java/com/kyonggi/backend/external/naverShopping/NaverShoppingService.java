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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        List<ShoppingResponse.ShoppingItem> list = getShoppingItems(condition);

        NaverSearchResponseDto result = new NaverSearchResponseDto();
        result.setItems(list);
        return result;

    }

    private List<ShoppingResponse.ShoppingItem> getShoppingItems(NaverSearchRequestDto condition) {
        String title = condition.getTitle();
        int price = condition.getPrice();
        String volume = condition.getVolume() != null ? condition.getVolume() : "";
        String brand = condition.getBrand() != null ? condition.getBrand() : "";

        String query = String.format("%s %s %s", brand, title, volume);
        ShoppingRequest request = new ShoppingRequest();
        log.info("query: {}", query);
        request.setQuery(query);

        ShoppingResponse response = naverClient.search(request);
        List<ShoppingResponse.ShoppingItem> items = response.getItems();

        if (items == null || items.isEmpty()) {
            return List.of(); // 빈 리스트 반환
        }

        // 가격 범위 계산
        int lower = (int) (price * 0.6);
        int upper = (int) (price * 1.3);

        // 후처리 필터링
        List<ShoppingResponse.ShoppingItem> list = items.stream()
                .filter(item -> {
                    if (item.getTitle() == null)
                        return false;

                    // HTML 태그 제거
                    String cleanTitle = item.getTitle().replaceAll("<[^>]*>", "");
                    item.setTitle(cleanTitle);


                    boolean priceRange = item.getLprice() >= lower && item.getLprice() <= upper;
                    boolean brandMatch = item.getBrand() != null && item.getBrand().contains(brand);
                    boolean makerMatch = item.getMaker() != null && item.getMaker().contains(brand);
                    if ( priceRange &&
                            (brandMatch || makerMatch)) return true;

                    return false;
                })
                .sorted(Comparator.comparingInt(item -> item.getLprice() != null ? item.getLprice() : Integer.MAX_VALUE)) // 가격 오름차순 정렬

                .toList();
        return list;
    }

    public List<Map<String, Object>> compareMallName(List<NaverSearchRequestDto> conditions) {
        List<Map<String, Object>> resultList = new ArrayList<>();

        List<String> brandList = List.of(
                "이마트몰", "Homeplus", "트레이더스", "홈플러스 익스프레스",
                "GS THE FRESH", "프레딧", "CU편의점", "GS25편의점"
        );

        //Pattern multiPackPattern = Pattern.compile("(\\d+)\\s*(입|팩|개)(\\s*[xX]\\s*(\\d+))?");
        Pattern multiPackPattern = Pattern.compile(
                "(\\d+)\\s*(입|팩|개)|([xX*]\\s*(\\d+))"
        );


        for (NaverSearchRequestDto condition : conditions) {
            String title = condition.getTitle();
            String volume = condition.getVolume() != null ? condition.getVolume() : "";
            String brand = condition.getBrand() != null ? condition.getBrand() : "";
            String query = String.format("%s %s %s", brand, title, volume).trim();

            ShoppingRequest request = new ShoppingRequest();
            request.setQuery(query);
            log.info("query: {}", query);

            ShoppingResponse response = naverClient.search(request);
            List<ShoppingResponse.ShoppingItem> items = response.getItems();
            if (items == null || items.isEmpty()) continue;

            Map<String, Object> result = new HashMap<>();
            result.put("query", query);

            Map<String, Object> resultsByMall = new HashMap<>();

            for (String mall : brandList) {
                List<ShoppingResponse.ShoppingItem> mallItems = items.stream()
                        .filter(item -> item.getMallName() != null && item.getMallName().contains(mall))
                        .map(item -> {
                            String cleanTitle = item.getTitle() != null ? item.getTitle().replaceAll("<[^>]*>", "") : "";
                            item.setTitle(cleanTitle);
                            return item;
                        })
                        .toList();

                if (mallItems.isEmpty()) continue;

                ShoppingResponse.ShoppingItem cheapest = mallItems.stream()
                        .filter(i -> i.getLprice() != null)
                        .min(Comparator.comparingInt(ShoppingResponse.ShoppingItem::getLprice))
                        .orElse(null);

                String priceInfo = cheapest.getLprice() + "원";
                Matcher matcher = multiPackPattern.matcher(cheapest.getTitle());
                if (matcher.find()) {
                    try {
                        int count = Integer.parseInt(matcher.group(1));
                        if (matcher.group(4) != null) {
                            count *= Integer.parseInt(matcher.group(4));
                        }
                        int unit = cheapest.getLprice() / count;
                        priceInfo += " (1개당 약 " + unit + "원)";
                    } catch (Exception ignored) {}
                }

                Map<String, Object> mallResult = new HashMap<>();
                mallResult.put("items", mallItems);

                Map<String, Object> selectItem = new HashMap<>();
                selectItem.put("title", cheapest.getTitle());
                selectItem.put("link", cheapest.getLink());
                selectItem.put("image", cheapest.getImage());
                selectItem.put("lprice", cheapest.getLprice());
                selectItem.put("mallName", cheapest.getMallName());
                selectItem.put("maker", cheapest.getMaker());
                selectItem.put("brand", cheapest.getBrand());
                selectItem.put("priceInfo", priceInfo);

                mallResult.put("selectItem", selectItem);
                resultsByMall.put(mall, mallResult);
            }

            result.put("resultsByMall", resultsByMall);
            resultList.add(result);
        }

        Map<String, List<Map<String, Object>>> mallGroupedResults = groupByMallAcrossQueries(resultList);
        mallGroupedResults.forEach((mall, items) -> {
            System.out.println("== " + mall + " ==");
            items.forEach(System.out::println);
        });


        return resultList;
    }

    public Map<String, List<Map<String, Object>>> groupByMallAcrossQueries(List<Map<String, Object>> fullResults) {
        Map<String, List<Map<String, Object>>> groupedByMall = new LinkedHashMap<>();

        for (Map<String, Object> queryResult : fullResults) {
            String query = (String) queryResult.get("query");
            Map<String, Map<String, Object>> resultsByMall = (Map<String, Map<String, Object>>) queryResult.get("resultsByMall");

            for (Map.Entry<String, Map<String, Object>> mallEntry : resultsByMall.entrySet()) {
                String mallName = mallEntry.getKey();
                Map<String, Object> mallData = mallEntry.getValue();

                if (mallData.containsKey("selectItem")) {
                    Map<String, Object> selectItem = (Map<String, Object>) mallData.get("selectItem");

                    Map<String, Object> mallItemSummary = new LinkedHashMap<>();
                    mallItemSummary.put("query", query);
                    mallItemSummary.put("title", selectItem.get("title"));
                    mallItemSummary.put("lprice", selectItem.get("lprice"));
                    mallItemSummary.put("priceInfo", selectItem.get("priceInfo"));
                    mallItemSummary.put("link", selectItem.get("link"));
                    mallItemSummary.put("image", selectItem.get("image"));
                    mallItemSummary.put("brand", selectItem.get("brand"));
                    mallItemSummary.put("maker", selectItem.get("maker"));

                    groupedByMall.computeIfAbsent(mallName, k -> new ArrayList<>()).add(mallItemSummary);
                }
            }
        }

        return groupedByMall;
    }



}
