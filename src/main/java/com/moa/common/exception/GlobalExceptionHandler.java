package com.moa.common.exception;

import com.moa.common.dto.response.BaseResponse;
import com.moa.common.dto.response.FieldErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;


// Controller 이후에 발생한 예외를(@RestControllerAdvice) BaseResponse로 변환
// BaseResponse, FieldErrorResponse, ErrorCode, BusinessException 사용
@Slf4j // log 변수 만듦
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @RestControllerAdvice
    // = 모든 Controller에서 올라온 예외를 이 클래스가 받음. 반환값은 JSON으로 응답

    // 도메인 비즈니스 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("비즈니스 에러 발생 : {}", e.getMessage());
        return ResponseEntity.status(e.getHttpStatus())
                .body(BaseResponse.fail(e.getHttpStatus(), e.getMessage()));
    }

    // @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<List<FieldErrorResponse>>> handleValidationException(MethodArgumentNotValidException e) {
        List<FieldErrorResponse> errors = e.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.fail(HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다", errors));
    }

    // JSON 형식 오류 (본문 누락, 타입 불일치 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleNotReadableException(HttpMessageNotReadableException e) {
        log.warn("요청 본문 파싱 실패 : {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.fail(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다"));
    }

    // 쿼리 파라미터·경로 변수 타입 불일치
    // 예: ?groupBuyStatus=abc (enum에 없는 값), ?page=abc, /api/v1/products/abc
    // 이 핸들러가 없으면 아래 Exception 핸들러로 떨어져 500이 됨
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("요청 파라미터 타입 불일치 : {} = {}", e.getName(), e.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.fail(HttpStatus.BAD_REQUEST, "잘못된 요청 값입니다: " + e.getName()));
    }

    // 존재하지 않는 경로
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.fail(HttpStatus.NOT_FOUND, "잘못된 접근입니다"));
    }

    // 예상치 못한 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleUnexpectedException(Exception e) {
        log.error("예상치 못한 에러 발생 : ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류로 인해 잠시 후 시도 바랍니다"));
    }
}