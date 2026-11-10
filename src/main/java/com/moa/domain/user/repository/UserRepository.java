package com.moa.domain.user.repository;

import com.moa.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 회원가입: 이메일 중복 체크
    boolean existsByEmail(String email);

    // 로그인: 이메일로 회원 조회
    Optional<User> findByEmail(String email);
}