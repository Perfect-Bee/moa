package com.moa.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;


// 서비스에서 던지는 공통 예외
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;          // 묶음 주문 failedItems에 코드 이름을 담기 위해 보관
    private final HttpStatus httpStatus;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode; // 에러코드 예시) EXCEEDS_REMAINING_QUANTITY
        this.httpStatus = errorCode.getHttpStatus();
    }
}