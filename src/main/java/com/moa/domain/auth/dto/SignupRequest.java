package com.moa.domain.auth.dto;

import com.moa.domain.user.enums.UserRole;
import jakarta.validation.constraints.*;

public record SignupRequest(

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "이메일 형식이 아닙니다")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자입니다")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).+$",
                message = "비밀번호는 영문, 숫자, 특수문자(!@#$%^&*)를 모두 포함해야 합니다"
        )
        String password,

        @NotBlank(message = "닉네임은 필수입니다")
        @Size(max = 30, message = "닉네임은 30자 이하입니다")
        String nickname,

        @NotNull(message = "역할은 필수입니다")
        UserRole role
) {
}