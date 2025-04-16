package com.kyonggi.backend.model.member.entity;

import com.kyonggi.backend.model.cart.entity.Cart;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(unique = true)
    private String username;
    private String password;
    private String email;
    private String role;
    private String name;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cart> cartList = new ArrayList<>();

//    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
//    private List<Item> itemList = new ArrayList<>();

    public void addCart(Cart cart) {
        cartList.add(cart);
        cart.setMember(this);
    }

}
