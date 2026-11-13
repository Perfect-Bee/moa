package com.moa.domain.order.enums;

// 결제 성공 후에 주문을 저장하므로 PAID에서 시작 (ERD의 PENDING은 사용하지 않음)
public enum OrderStatus {
    PAID,       // 참여 완료 (선결제)
    CONFIRMED,  // 공동구매 성사 → 구매 확정
    CANCELED,   // 마감 전 사용자 취소
    REFUNDED    // 공동구매 실패 → 환불
}