package com.moa.domain.order.dto;

import com.moa.common.exception.BusinessException;
import com.moa.domain.order.exception.OrderErrorCode;
import jakarta.validation.constraints.Positive;

import java.util.List;

// 장바구니 주문: { "cartItemIds": [10, 11] }
// 단건 바로 주문: { "groupBuyId": 3, "quantity": 1 }
public record OrderCreateRequest(
        List<Long> cartItemIds,
        Long groupBuyId,
        @Positive(message = "수량은 1 이상이어야 합니다")
        Integer quantity
) {
    public boolean isCartOrder() {
        return cartItemIds != null && !cartItemIds.isEmpty();
    }

    // 둘 중 정확히 하나만 와야 함
    public void validate() {
        boolean single = groupBuyId != null && quantity != null;
        boolean mixed = isCartOrder() && (groupBuyId != null || quantity != null);
        if (mixed || (!isCartOrder() && !single)) {
            throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
        }
    }
}