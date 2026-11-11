package com.moa.domain.product.exception;

import com.moa.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {

    // 상세 조회
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다"),
    // 등록 시 항상 함께 만들어지므로 이 에러가 나면 데이터 이상
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "상품의 채팅방을 찾을 수 없습니다"),

    // 목록 조회
    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "페이지는 0 이상, 크기는 1~100이어야 합니다");

    private final HttpStatus httpStatus;
    private final String message;
}