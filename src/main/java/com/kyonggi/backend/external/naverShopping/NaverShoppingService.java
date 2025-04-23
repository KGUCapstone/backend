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
        Cart cart = getActiveCart(member);
        log.info("cartName: {}", cart.getName());

        OnlineItem item = new OnlineItem();
        String cleanTitle = dto.getTitle().replaceAll("<[^>]*>", "");
        StringBuilder nameBuilder = new StringBuilder();

        if (dto.getBrand() != null && !dto.getBrand().isBlank()) nameBuilder.append(dto.getBrand()).append(" ");
        nameBuilder.append(cleanTitle);
        if (dto.getVolume() != null && !dto.getVolume().isBlank()) nameBuilder.append(" ").append(dto.getVolume());

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
                .filter(Cart::isActive)
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

        if (items == null || items.isEmpty()) return List.of();

        int lower = (int) (price * 0.6);
        int upper = (int) (price * 1.3);

        return items.stream()
                .filter(item -> {
                    if (item.getTitle() == null) return false;
                    item.setTitle(item.getTitle().replaceAll("<[^>]*>", ""));
                    boolean priceRange = item.getLprice() >= lower && item.getLprice() <= upper;
                    boolean brandMatch = item.getBrand() != null && item.getBrand().contains(brand);
                    boolean makerMatch = item.getMaker() != null && item.getMaker().contains(brand);
                    return priceRange && (brandMatch || makerMatch);
                })
                .sorted(Comparator.comparingInt(item -> item.getLprice() != null ? item.getLprice() : Integer.MAX_VALUE))
                .toList();
    }

    public List<Map<String, Object>> compareAllWithSelectItems(List<NaverSearchRequestDto> conditions, List<String> targetMallNames) {
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (NaverSearchRequestDto condition : conditions) {
            String query = condition.getTitle();
            String volume = condition.getVolume() != null ? condition.getVolume() : "";
            String brand = condition.getBrand() != null ? condition.getBrand() : "";
            String searchQuery = String.format("%s %s %s", brand, query, volume).trim();

            ShoppingRequest request = new ShoppingRequest();
            request.setQuery(searchQuery);
            ShoppingResponse response = naverClient.search(request);
            List<ShoppingResponse.ShoppingItem> items = response.getItems();
            if (items == null || items.isEmpty()) continue;

            Map<String, Object> queryResult = new LinkedHashMap<>();
            queryResult.put("query", query);

            Map<String, Object> resultsByMall = new LinkedHashMap<>();

            for (String mall : targetMallNames) {
                List<ShoppingResponse.ShoppingItem> mallItems = items.stream()
                        .filter(item -> item.getMallName() != null && item.getMallName().contains(mall))
                        .map(item -> {
                            if (item.getTitle() != null) item.setTitle(item.getTitle().replaceAll("<[^>]*>", ""));
                            return item;
                        })
                        .toList();

                if (mallItems.isEmpty()) continue;

                ShoppingResponse.ShoppingItem cheapest = mallItems.stream()
                        .filter(i -> i.getLprice() != null)
                        .min(Comparator.comparingInt(ShoppingResponse.ShoppingItem::getLprice))
                        .orElse(null);

                if (cheapest == null) continue;

                Map<String, Object> selectItemMap = toSelectItemMap(cheapest);
                Map<String, Object> mallData = new LinkedHashMap<>();
                mallData.put("selectItem", selectItemMap);
                mallData.put("items", mallItems);
                resultsByMall.put(mall, mallData);
            }

            queryResult.put("resultsByMall", resultsByMall);
            resultList.add(queryResult);
        }

        return resultList;
    }

//    public Map<String, List<Map<String, Object>>> compareItemsGroupedByMall(CompareRequestDto requestDto) {
//        List<NaverSearchRequestDto> conditions = requestDto.getConditions();
//        List<String> targetMallNames = requestDto.getMallNames();
//        List<Map<String, Object>> fullResults = compareAllWithSelectItems(conditions, targetMallNames);
//
//
//        return groupByMallAcrossQueries(fullResults);
//    }
    public Map<String, Object> compareItemsGroupedByMall(CompareRequestDto requestDto) {
        List<NaverSearchRequestDto> conditions = requestDto.getConditions();
        List<String> targetMallNames = requestDto.getMallNames();
        List<Map<String, Object>> fullResults = compareAllWithSelectItems(conditions, targetMallNames);

        Map<String, List<Map<String, Object>>> grouped = groupByMallAcrossQueries(fullResults);

        List<String> expectedQueries = conditions.stream()
                .map(NaverSearchRequestDto::getTitle)
                .toList();

        Map<String, Object> summary = getMallSummary(fullResults, expectedQueries);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("grouped", grouped);
        response.put("summary", summary);

        return response;
    }


    public Map<String, List<Map<String, Object>>> groupByMallAcrossQueries(List<Map<String, Object>> fullResults) {
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();

        for (Map<String, Object> result : fullResults) {
            String query = (String) result.get("query");
            Map<String, Map<String, Object>> resultsByMall = (Map<String, Map<String, Object>>) result.get("resultsByMall");

            for (Map.Entry<String, Map<String, Object>> mallEntry : resultsByMall.entrySet()) {
                String mallName = mallEntry.getKey();
                Map<String, Object> mallData = mallEntry.getValue();
                if (mallData == null || !mallData.containsKey("selectItem")) continue;

                Map<String, Object> selectItem = (Map<String, Object>) mallData.get("selectItem");
                Map<String, Object> itemSummary = new LinkedHashMap<>(selectItem);
                itemSummary.put("query", query);

                grouped.computeIfAbsent(mallName, k -> new ArrayList<>()).add(itemSummary);
            }
        }

        return grouped;
    }

    private Map<String, Object> toSelectItemMap(ShoppingResponse.ShoppingItem item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", item.getTitle());
        map.put("link", item.getLink());
        map.put("image", item.getImage());
        map.put("lprice", item.getLprice());
        map.put("mallName", item.getMallName());
        map.put("maker", item.getMaker());
        map.put("brand", item.getBrand());
        map.put("priceInfo", buildPriceInfo(item));
        return map;
    }

    public Map<String, Object> getMallSummary(List<Map<String, Object>> fullResults, List<String> expectedQueries) {
        Map<String, List<Map<String, Object>>> grouped = groupByMallAcrossQueries(fullResults);
        Map<String, Object> summaryByMall = new LinkedHashMap<>();

        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            String mallName = entry.getKey();
            List<Map<String, Object>> items = entry.getValue();

            int totalPrice = items.stream()
                    .mapToInt(item -> (int) item.getOrDefault("lprice", 0))
                    .sum();

            Map<String, String> inclusion = new LinkedHashMap<>();
            Set<String> foundQueries = new HashSet<>();
            for (Map<String, Object> item : items) {
                if (item.containsKey("query")) {
                    foundQueries.add(item.get("query").toString());
                }
            }

            for (String query : expectedQueries) {
                inclusion.put(query, foundQueries.contains(query) ? "O" : "X");
            }

            Map<String, Object> mallSummary = new LinkedHashMap<>();
            mallSummary.put("totalPrice", totalPrice);
            mallSummary.put("includes", inclusion);

            summaryByMall.put(mallName, mallSummary);
        }

        return summaryByMall;
    }


    //private static final Pattern MULTI_PACK_PATTERN = Pattern.compile("(\\d+)\\s*(입|팩|개)(\\s*[xX*]\\s*(\\d+))?");
    private static final Pattern MULTI_PACK_PATTERN = Pattern.compile(
            "(\\d+)\\s*(개|입|팩)?(x(\\d+))?", Pattern.CASE_INSENSITIVE
    );

    private String buildPriceInfo(ShoppingResponse.ShoppingItem item) {
        String info = item.getLprice() + "원";
        String title = item.getTitle();

        // 용량 단위 제거용 정규식
        Pattern volumePattern = Pattern.compile("(\\d+)(ml|g|kg|L)", Pattern.CASE_INSENSITIVE);
        String titleWithoutVolume = volumePattern.matcher(title).replaceAll("");

        Matcher matcher = MULTI_PACK_PATTERN.matcher(titleWithoutVolume);
        int count = 1;

        if (matcher.find()) {
            try {
                int base = Integer.parseInt(matcher.group(1));
                int multiplier = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 1;
                count = base * multiplier;
            } catch (NumberFormatException ignore) {}
        }

        if (count > 1 && item.getLprice() != null) {
            int unit = item.getLprice() / count;
            info += " (1개당 약 " + unit + "원)";
        }

        return info;
    }


}