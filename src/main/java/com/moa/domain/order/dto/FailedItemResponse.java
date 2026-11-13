package com.moa.domain.order.dto;

import com.moa.common.exception.ErrorCode;

// 묶음 주문에서 실패한 항목과 이유
public record FailedItemResponse(Long cartItemId, Long groupBuyId, String code, String message) {

    public static FailedItemResponse of(Long cartItemId, Long groupBuyId, ErrorCode errorCode) {
        String code = (errorCode instanceof Enum<?> e) ? e.name() : "UNKNOWN";
        return new FailedItemResponse(cartItemId, groupBuyId, code, errorCode.getMessage());
    }

    // 예상하지 못한 예외 (내부 메시지는 응답에 노출하지 않고 로그로만)
    public static FailedItemResponse unexpected(Long cartItemId, Long groupBuyId) {
        return new FailedItemResponse(cartItemId, groupBuyId, "INTERNAL_ERROR", "주문 처리 중 오류가 발생했습니다");
    }
}