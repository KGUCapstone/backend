package com.kyonggi.backend.model.item;

import com.kyonggi.backend.external.naverShopping.ShoppingResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "online_item")
@PrimaryKeyJoinColumn(name = "item_id") // item과 id 공유
public class OnlineItem extends Item {

    private String link;      // 사이트 주소
    private String image;     // 이미지 주소
    private String mallName;
    private int compareItemPrice; // 가격 비교를 위한 필드

    public OnlineItem(ShoppingResponse.ShoppingItem item) {
        this.setName(item.getTitle());
        this.setPrice(item.getLprice());
        this.setVolume(null);
        this.link = item.getLink();
        this.image = item.getImage();
        this.mallName = item.getMallName();
        this.setBrand(item.getBrand());

    }
}
