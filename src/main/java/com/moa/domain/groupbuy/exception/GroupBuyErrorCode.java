package com.moa.domain.groupbuy.exception;

import com.moa.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GroupBuyErrorCode implements ErrorCode {

    // 개설
    NOT_PRODUCT_OWNER(HttpStatus.FORBIDDEN, "본인 상품에만 공동구매를 개설할 수 있습니다"),
    GROUP_BUY_ALREADY_OPEN(HttpStatus.CONFLICT, "진행 중인 공동구매가 있습니다"),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "목표 수량은 최대 수량보다 클 수 없습니다"),
    INVALID_PERIOD(HttpStatus.BAD_REQUEST, "시작 시각은 현재 이후, 마감 시각은 시작 시각 이후여야 합니다"),

    // 참여 (장바구니 담기·주문)
    GROUP_BUY_NOT_OPEN(HttpStatus.CONFLICT, "참여할 수 없는 공동구매입니다 (시작 전 또는 마감)"),
    EXCEEDS_REMAINING_QUANTITY(HttpStatus.CONFLICT, "남은 수량을 초과했습니다"),

    // Redis 수량 키가 없음 (개설 직후 Redis 장애 등). DB 기준 재적재는 추후
    QUANTITY_NOT_READY(HttpStatus.SERVICE_UNAVAILABLE, "참여 수량 정보를 준비 중입니다. 잠시 후 다시 시도해주세요"),

    // 조회
    GROUP_BUY_NOT_FOUND(HttpStatus.NOT_FOUND, "공동구매를 찾을 수 없습니다");

    private final HttpStatus httpStatus;
    private final String message;
}