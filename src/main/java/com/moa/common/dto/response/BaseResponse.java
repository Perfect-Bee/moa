package com.moa.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

// 모든 응답을   success
//              status
//              message
//              data    로 통일
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BaseResponse<T>(
        boolean success,
        HttpStatus status,
        String message,
        T data
) {

    // 성공 응답
    public static <T> BaseResponse<T> success(HttpStatus status, String message, T data) {
        return new BaseResponse<>(true, status, message, data);
    }

    // 실패 응답 - data 없음
    public static <T> BaseResponse<T> fail(HttpStatus status, String message) {
        return new BaseResponse<>(false, status, message, null);
    }

    // 실패 응답 - 검증 오류 목록 등 data 포함
    public static <T> BaseResponse<T> fail(HttpStatus status, String message, T data) {
        return new BaseResponse<>(false, status, message, data);
    }
}