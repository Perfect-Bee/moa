package com.moa.domain.order.dto;

import com.moa.domain.order.entity.Order;
import com.moa.domain.order.enums.OrderStatus;

public record OrderResponse(
        Long orderId, String orderNumber, Long groupBuyId,
        int quantity, int unitPrice, long totalPrice, OrderStatus status
) {
    // groupBuy는 LAZY 프록시라 getId()는 추가 쿼리 없음
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getGroupBuy().getId(),
                order.getQuantity(), order.getUnitPrice(), order.getTotalPrice(), order.getStatus());
    }
}