package com.moa.common.config.security;

import com.moa.common.dto.response.BaseResponse;
import com.moa.common.enums.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

import static com.moa.common.enums.AuthErrorCode.ERR_AUTH_UNAUTHORIZED;

// 인증 없이 인증 필요 API에 접근 → 401 응답
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            @NonNull AuthenticationException authException
    ) throws IOException {
        // JwtFilter가 저장한 원인이 있으면 사용, 없으면 기본 메시지
        Object exception = request.getAttribute("exception");
        AuthErrorCode errorCode = (exception instanceof AuthErrorCode authErrorCode)
                ? authErrorCode
                : ERR_AUTH_UNAUTHORIZED;

        log.info("인증 실패 : {}", errorCode.getMessage());

        BaseResponse<Void> body = BaseResponse.fail(errorCode.getHttpStatus(), errorCode.getMessage());
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}