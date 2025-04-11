package com.kyonggi.backend.model.cart.repository;

import com.kyonggi.backend.model.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
}
