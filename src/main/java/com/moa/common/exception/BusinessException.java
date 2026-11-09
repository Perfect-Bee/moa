package com.moa.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;


// 서비스에서 던지는 공통 예외
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus httpStatus;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.httpStatus = errorCode.getHttpStatus();
    }
}