package com.moa.common.dto.response;

// @Valid 검증 실패 시 필드별 오류 한 줄(어떤 필드가 / 왜 틀렸는가)
// BaseResponse의 fail(status, message, data)에서 data에 들어간다.
// data의 목록을 만드는 건 commom/exception/GlobalExceptionHandler
public record FieldErrorResponse(
        String field,
        String message
) {}