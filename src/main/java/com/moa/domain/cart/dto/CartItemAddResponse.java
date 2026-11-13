package com.moa.domain.cart.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.moa.domain.cart.entity.CartItem;

// created: 새로 담았으면 201, 기존 항목에 합산했으면 200 (응답 JSON에는 포함하지 않음)
public record CartItemAddResponse(
        Long cartItemId,
        int quantity,
        @JsonIgnore boolean created
) {
    public static CartItemAddResponse of(CartItem item, boolean created) {
        return new CartItemAddResponse(item.getId(), item.getQuantity(), created);
    }
}