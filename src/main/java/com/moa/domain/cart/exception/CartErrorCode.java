package com.moa.domain.cart.exception;

import com.moa.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CartErrorCode implements ErrorCode {

    // 가입 시 항상 함께 만들어지므로 이 에러가 나면 데이터 이상
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니를 찾을 수 없습니다"),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니 항목을 찾을 수 없습니다"),
    NOT_CART_OWNER(HttpStatus.FORBIDDEN, "본인 장바구니 항목만 변경할 수 있습니다"),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "수량은 1 이상이어야 합니다");

    private final HttpStatus httpStatus;
    private final String message;
}