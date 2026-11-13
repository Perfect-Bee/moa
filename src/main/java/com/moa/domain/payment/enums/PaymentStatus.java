package com.moa.domain.payment.enums;

public enum PaymentStatus {
    COMPLETED,  // 결제 완료
    REFUNDED    // 환불 (주문 취소, 공동구매 실패)
}