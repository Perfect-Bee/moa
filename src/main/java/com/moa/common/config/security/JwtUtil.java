package com.moa.common.config.security;

import com.moa.domain.user.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

// 토큰 생성·검증·인증 객체 변환
@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    // 서명 키는 생성 시 한 번만 만들어 재사용
    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    // Access Token 생성
    public String createAccessToken(Long userId, UserRole role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtProperties.accessExpiration()))
                .signWith(signingKey)
                .compact();
    }

    // 서명·만료 검증 후 Claims 반환 (실패 시 예외 → JwtFilter에서 처리)
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Claims → Spring Security 인증 객체 (principal = userId)
    public UsernamePasswordAuthenticationToken getAuthentication(Claims claims) {
        Long userId = Long.parseLong(claims.getSubject());
        UserRole role = UserRole.valueOf(claims.get("role", String.class));

        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));

        return new UsernamePasswordAuthenticationToken(userId, null, authorities);
    }
}