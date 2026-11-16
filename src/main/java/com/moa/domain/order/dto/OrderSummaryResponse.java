package com.moa.domain.order.dto;

import com.moa.domain.order.enums.OrderStatus;
import com.moa.domain.order.entity.Order;

import java.time.LocalDateTime;

// 내 주문 목록의 한 줄 (GET /api/v1/orders)
// 주문 테이블 값만 담음 -> 공동구매·상품을 조회하지 않아 목록 크기만큼 추가 쿼리가 생기지 않음 (N+1 방지)
public record OrderSummaryResponse(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        long totalPrice,
        LocalDateTime createdAt
) {
    // 서비스에서 Page<Order>.map(OrderSummaryResponse::from)으로 변환
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(order.getId(), order.getOrderNumber(), order.getStatus(),
                order.getTotalPrice(), order.getCreatedAt());
    }
}