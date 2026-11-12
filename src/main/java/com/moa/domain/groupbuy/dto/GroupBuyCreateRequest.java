package com.moa.domain.groupbuy.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record GroupBuyCreateRequest(
        @NotNull(message = "도매가는 필수입니다")
        @Positive(message = "도매가는 0보다 커야 합니다")
        Integer wholesalePrice,

        @NotNull(message = "목표 수량은 필수입니다")
        @Positive(message = "목표 수량은 0보다 커야 합니다")
        Integer targetQuantity,

        @NotNull(message = "최대 수량은 필수입니다")
        @Positive(message = "최대 수량은 0보다 커야 합니다")
        Integer maxQuantity,

        LocalDateTime startAt,   // 생략하면 서버 현재 시각

        @NotNull(message = "마감 시각은 필수입니다")
        LocalDateTime endAt
) {}