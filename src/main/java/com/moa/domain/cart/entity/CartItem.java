package com.moa.domain.cart.entity;

import com.moa.common.exception.BusinessException;
import com.moa.domain.cart.exception.CartErrorCode;
import com.moa.domain.groupbuy.entity.GroupBuy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 장바구니에 담긴 공동구매 1개 (항목 1개 = 나중에 주문 1건)
// 같은 장바구니에 같은 공동구매는 1행만 → 다시 담으면 수량 합산
@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "group_buy_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_buy_id", nullable = false)
    private GroupBuy groupBuy;

    @Column(nullable = false)
    private int quantity;

    private CartItem(Cart cart, GroupBuy groupBuy, int quantity) {
        this.cart = cart;
        this.groupBuy = groupBuy;
        this.quantity = quantity;
    }

    public static CartItem create(Cart cart, GroupBuy groupBuy, int quantity) {
        validateQuantity(quantity);
        return new CartItem(cart, groupBuy, quantity);
    }

    // 수량을 요청 값으로 덮어씀 (명세: 멱등)
    public void changeQuantity(int quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
    }

    // DTO 검증(@Positive)을 거치지 않는 경로에서도 규칙 보장
    private static void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(CartErrorCode.INVALID_QUANTITY);
        }
    }
}