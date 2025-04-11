package com.kyonggi.backend.model.cart.repository;

import com.kyonggi.backend.model.cart.entity.Cart;
import com.kyonggi.backend.model.item.OnlineItem;
import com.kyonggi.backend.model.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("SELECT i FROM OnlineItem i WHERE i.cart.member = :member AND i.cart.isActive = true")
    List<OnlineItem> findActiveCartByMember(@Param("member") Member member);

    @Query("SELECT i FROM OnlineItem i WHERE i.cart.member = :member AND i.cart.isActive = false")
    List<OnlineItem> findInactiveCartByMember(@Param("member") Member member);


}
