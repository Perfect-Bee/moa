package com.moa.domain.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemAddRequest(
        @NotNull(message = "공동구매 ID는 필수입니다")
        Long groupBuyId,

        @NotNull(message = "수량은 필수입니다")
        @Positive(message = "수량은 1 이상이어야 합니다")
        Integer quantity
) {}