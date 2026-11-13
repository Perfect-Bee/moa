package com.moa.domain.auth.service;

import com.moa.common.config.security.JwtUtil;
import com.moa.common.exception.BusinessException;
import com.moa.domain.auth.dto.LoginRequest;
import com.moa.domain.auth.dto.LoginResponse;
import com.moa.domain.auth.dto.SignupRequest;
import com.moa.domain.auth.dto.SignupResponse;
import com.moa.domain.cart.entity.Cart;
import com.moa.domain.cart.repository.CartRepository;
import com.moa.domain.user.entity.User;
import com.moa.domain.user.exception.UserErrorCode;
import com.moa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본은 읽기 전용, 저장하는 메서드만 따로 열어줌
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // SecurityConfig에 등록한 BCryptPasswordEncoder
    private final JwtUtil jwtUtil;
    private final CartRepository cartRepository;

    // 회원가입
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 1. 이메일 중복 체크
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }

        // 2. 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = User.create(request.email(), encodedPassword, request.nickname(), request.role());
        User savedUser = userRepository.save(user);

        // 3. 장바구니 생성 (같은 트랜잭션 → 하나라도 실패하면 둘 다 롤백)
        cartRepository.save(Cart.create(savedUser));

        // 4. 응답 변환 (password 제외)
        return SignupResponse.from(savedUser);
    }

    // 로그인
    public LoginResponse login(LoginRequest request) {
        // 1. 이메일로 회원 조회 (없으면 401)
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_CREDENTIALS));

        // 2. 비밀번호 비교 (틀려도 같은 401)
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(UserErrorCode.INVALID_CREDENTIALS);
        }

        // 3. 토큰 발급
        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getRole());
        return LoginResponse.of(accessToken);
    }
}