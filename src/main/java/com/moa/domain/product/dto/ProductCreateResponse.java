package com.moa.domain.product.dto;

// 상품 등록 응답. 채팅방도 함께 만들어졌음을 chatRoomId로 알려줌
public record ProductCreateResponse(
        Long productId,
        Long chatRoomId
) {
}