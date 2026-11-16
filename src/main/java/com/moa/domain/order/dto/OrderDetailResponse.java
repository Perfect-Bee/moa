package com.moa.domain.order.dto;

import com.moa.domain.groupbuy.entity.GroupBuy;
import com.moa.domain.groupbuy.enums.GroupBuyStatus;
import com.moa.domain.order.entity.Order;
import com.moa.domain.order.enums.OrderStatus;
import com.moa.domain.payment.entity.Payment;
import com.moa.domain.payment.enums.PaymentStatus;

import java.time.LocalDateTime;

// 주문 상세 (GET /api/v1/orders/{orderId})
// 주문 + 참여한 공동구매 + 결제 정보를 한 응답에 모음
public record OrderDetailResponse(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        long totalPrice,
        LocalDateTime createdAt,
        GroupBuyInfo groupBuy,
        PaymentInfo payment
) {
    // 참여한 공동구매 정보
    // 주문 1건 = 공동구매 1개 → 명세서의 items 배열 대신 객체 1개
    public record GroupBuyInfo(Long groupBuyId, String productName, int quantity,
                               int unitPrice, GroupBuyStatus groupBuyStatus) {
    }

    // 결제·환불 정보 (취소·실패 환불 전에는 refundedAt이 null)
    public record PaymentInfo(long amount, PaymentStatus status,
                              LocalDateTime paidAt, LocalDateTime refundedAt) {
    }

    // groupBuy·product는 LAZY → 서비스 트랜잭션 안에서 호출
    // (밖에서 호출하면 LazyInitializationException 가능)
    public static OrderDetailResponse of(Order order, Payment payment) {
        GroupBuy groupBuy = order.getGroupBuy();
        return new OrderDetailResponse(
                order.getId(), order.getOrderNumber(), order.getStatus(),
                order.getTotalPrice(), order.getCreatedAt(),

                new GroupBuyInfo(groupBuy.getId(), groupBuy.getProduct().getName(),
                        order.getQuantity(), order.getUnitPrice(), groupBuy.getStatus()),

                new PaymentInfo(payment.getAmount(), payment.getStatus(),
                        payment.getPaidAt(), payment.getRefundedAt())
        );
    }
}