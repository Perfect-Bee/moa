package com.moa.common.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;


// yml의 jwt.* 값을 받아오는 record, 설정값을 묶어서 주입
// 서명 키와 만료 시간 각각 사용
// 예) application.yml (Git에 올라감)
//jwt:
//  access-expiration: 3600000   # 1시간 (ms)
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessExpiration
) {}