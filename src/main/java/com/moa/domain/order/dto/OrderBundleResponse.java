package com.moa.domain.order.dto;

import java.util.List;

// 장바구니 묶음 주문 결과: 성공한 주문 + 실패한 항목
public record OrderBundleResponse(List<OrderResponse> orders, List<FailedItemResponse> failedItems) {
}