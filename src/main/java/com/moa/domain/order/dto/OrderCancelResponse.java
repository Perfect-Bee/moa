package com.moa.domain.order.dto;

import com.moa.domain.order.entity.Order;
import com.moa.domain.order.enums.OrderStatus;
import com.moa.domain.payment.entity.Payment;

// 주문 취소 결과 (POST /api/v1/orders/{orderId}/cancel)
// 취소 여부와 환불 금액만 알려줌. 자세한 내용은 상세 조회로 확인
public record OrderCancelResponse(
        Long orderId,
        OrderStatus status,
        long refundedAmount) {

    // 결제 금액 전체가 환불 금액
    public static OrderCancelResponse of(Order order, Payment payment) {

        return new OrderCancelResponse(order.getId(), order.getStatus(), payment.getAmount());
    }
}