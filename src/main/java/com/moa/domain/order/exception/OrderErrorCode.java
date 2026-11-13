package com.moa.domain.order.exception;

import com.moa.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    // 요청 형식: cartItemIds 또는 (groupBuyId + quantity) 중 하나만
    INVALID_ORDER_REQUEST(HttpStatus.BAD_REQUEST, "장바구니 주문(cartItemIds) 또는 단건 주문(groupBuyId, quantity) 중 하나로 요청해주세요"),

    // 결제
    PAYMENT_FAILED(HttpStatus.PAYMENT_REQUIRED, "결제에 실패했습니다"),

    // 묶음 주문 전부 실패
    ALL_ITEMS_FAILED(HttpStatus.CONFLICT, "모든 항목의 주문에 실패했습니다");

    private final HttpStatus httpStatus;
    private final String message;
}