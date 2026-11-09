package com.moa.common.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.moa.common.enums.AuthErrorCode.*;

// 요청마다 토큰 검사, 실패 원인을 request에 저장
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = resolveToken(request);

            // 토큰이 없으면 그대로 통과 → 공개 API는 성공, 인증 필요 API는 EntryPoint(401)
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            Claims claims = jwtUtil.validateToken(token);
            UsernamePasswordAuthenticationToken authentication = jwtUtil.getAuthentication(claims);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다");
            request.setAttribute("exception", ERR_AUTH_EXPIRED_TOKEN);
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException | UnsupportedJwtException e) {
            // SecurityException: 서명 불일치(위조·다른 키로 서명된 토큰)
            log.info("유효하지 않은 JWT 토큰입니다 : {}", e.getClass().getSimpleName());
            request.setAttribute("exception", ERR_AUTH_INVALID_TOKEN);
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 비어있습니다");
            request.setAttribute("exception", ERR_AUTH_EMPTY_TOKEN);
        } catch (Exception e) {
            log.error("인증 처리 중 오류 발생", e);
            request.setAttribute("exception", ERR_AUTH_UNAUTHORIZED);
        }

        filterChain.doFilter(request, response);
    }

    // Authorization: Bearer {token} 에서 토큰만 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}