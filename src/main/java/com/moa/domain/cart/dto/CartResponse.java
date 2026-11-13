package com.moa.domain.cart.dto;

import com.moa.domain.cart.entity.Cart;
import com.moa.domain.cart.entity.CartItem;
import com.moa.domain.groupbuy.entity.GroupBuy;

import java.time.LocalDateTime;
import java.util.List;

// 명세 「장바구니 조회」 응답 형식
public record CartResponse(Long cartId, List<Item> items, long totalPrice) {

    public record Item(
            Long cartItemId, Long groupBuyId, String productName,
            int wholesalePrice, int quantity, int remainingQuantity, boolean orderable
    ) {
        // orderable: 참여 가능한 공동구매이고, 담은 수량이 남은 수량 이내일 때만 true
        static Item of(CartItem cartItem, LocalDateTime now) {
            GroupBuy g = cartItem.getGroupBuy();
            boolean orderable = g.isOrderable(now) && cartItem.getQuantity() <= g.getRemainingQuantity();
            return new Item(cartItem.getId(), g.getId(), g.getProduct().getName(),
                    g.getWholesalePrice(), cartItem.getQuantity(), g.getRemainingQuantity(), orderable);
        }
    }

    // totalPrice: 주문 가능한 항목만 합산 (마감된 항목은 결제 금액에서 제외)
    public static CartResponse of(Cart cart, List<CartItem> cartItems, LocalDateTime now) {
        List<Item> items = cartItems.stream()
                .map(cartItem -> Item.of(cartItem, now))
                .toList();
        long totalPrice = items.stream()
                .filter(Item::orderable)
                .mapToLong(item -> (long) item.wholesalePrice() * item.quantity())
                .sum();
        return new CartResponse(cart.getId(), items, totalPrice);
    }
}