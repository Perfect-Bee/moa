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

    // 목록 페이징
    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "페이지는 0 이상, 크기는 1~100이어야 합니다"),

    // 조회, 취소
    NOT_ORDER_OWNER(HttpStatus.FORBIDDEN, "본인 주문만 조회·취소할 수 있습니다"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다"),
    INVALID_ORDER_STATUS(HttpStatus.CONFLICT, "취소할 수 없는 주문입니다 (이미 취소·확정·환불)"),

    // 묶음 주문 전부 실패
    ALL_ITEMS_FAILED(HttpStatus.CONFLICT, "모든 항목의 주문에 실패했습니다");

    private final HttpStatus httpStatus;
    private final String message;
}