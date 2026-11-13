package com.moa.domain.cart.dto;

import com.moa.domain.cart.entity.CartItem;

public record CartItemUpdateResponse(Long cartItemId, int quantity) {

    public static CartItemUpdateResponse from(CartItem item) {
        return new CartItemUpdateResponse(item.getId(), item.getQuantity());
    }
}