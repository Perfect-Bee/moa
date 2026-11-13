package com.moa.domain.cart.entity;

import com.moa.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 사용자당 장바구니 1개. 회원가입 시 같은 트랜잭션에서 함께 생성
@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true: 사용자당 장바구니 1개를 DB 제약으로 보장
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private Cart(User user) {
        this.user = user;
    }

    public static Cart create(User user) {
        return new Cart(user);
    }
}