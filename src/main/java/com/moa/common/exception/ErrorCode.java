package com.moa.common.exception;

import org.springframework.http.HttpStatus;

// 에러 코드 enum들이 따를 규칙(인터페이스) : 상속 받기 위해 enum이 아닌 interface 사용
// 매서드 이름 : enum에 Lombok @Getter를 붙였을 때 자동으로 생기는 이름과 동일하게 맞춤
// 에러코드라면 HTTP 상태와 메시지를 돌려줘야 하므로 사용됨
public interface ErrorCode {

    HttpStatus getHttpStatus();
    String getMessage();
}