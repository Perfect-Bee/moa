package com.moa.domain.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType
) {
    private static final String BEARER = "Bearer";

    // of() : 값을 받아 조립할 때 씀. 엑세스 토큰 받아서 엑세스 토큰 + 접두사(Bearer) 붙이기
    public static LoginResponse of(
            String accessToken
    ) {
        return new LoginResponse(accessToken, BEARER);
    }
}