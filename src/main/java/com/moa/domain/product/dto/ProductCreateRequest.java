package com.moa.domain.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// 상품 등록 요청. 길이 제한은 Product 엔티티 컬럼 길이와 맞춤
public record ProductCreateRequest(

        @NotBlank(message = "상품명은 필수입니다")
        @Size(max = 100, message = "상품명은 100자 이하입니다")
        String name,

        // 선택 입력
        String description,

        // int가 아니라 Integer: 값이 빠지면 0이 아니라 null이 되어 @NotNull로 잡을 수 있음
        @NotNull(message = "정가는 필수입니다")
        @Positive(message = "정가는 0보다 커야 합니다")
        Integer listPrice,

        @Size(max = 500, message = "이미지 URL은 500자 이하입니다")
        String imageUrl
) {
}