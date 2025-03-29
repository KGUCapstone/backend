package com.kyonggi.backend.model.cart;

import com.kyonggi.backend.model.item.Item;
import com.kyonggi.backend.model.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long id;

    private String name;
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL)
    private List<Item> itemList = new ArrayList<>();

    public void addItem(Item item) {
        this.itemList.add(item);
        item.setCart(this);
        item.setMember(this.member); // optional
    }

    @Column(name = "is_active")
    private boolean isActive = true;

}
