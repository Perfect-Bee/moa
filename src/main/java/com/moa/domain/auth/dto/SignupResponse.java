package com.moa.domain.auth.dto;

import com.moa.domain.user.entity.User;

public record SignupResponse(
        Long userId,
        String email,
        String nickname
) {
    // from() : 다른 객체 하나를 반환할 때. user 받고 id, email, nickname 반환
    public static SignupResponse from(
            User user
    ) {
        return new SignupResponse(user.getId(), user.getEmail(), user.getNickname());
    }
}