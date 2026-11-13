package com.moa.domain.cart.repository;

import com.moa.domain.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    // 로그인 사용자의 장바구니 (UserId → user.id 로 해석)
    Optional<Cart> findByUserId(Long userId);
}