package com.moa.domain.user.entity;

import com.moa.domain.user.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA용 기본 생성자, 외부 new 차단
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password; // BCrypt로 암호화된 값 저장

    @Column(nullable = false, length = 30)
    private String nickname;

    @Enumerated(EnumType.STRING) // 숫자(ORDINAL) 대신 이름으로 저장
    @Column(nullable = false, length = 20)
    private UserRole role;

    private User(String email, String password, String nickname, UserRole role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
    }

    // 생성 경로를 하나로 고정 (암호화된 비밀번호만 받도록 이름으로 표시)
    public static User create(String email, String encodedPassword, String nickname, UserRole role) {
        return new User(email, encodedPassword, nickname, role);
    }
}