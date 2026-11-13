package com.moa.domain.payment.entity;

import com.moa.domain.order.entity.Order;
import com.moa.domain.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 주문 1건 : 결제 1건. 결제·환불 이력을 주문 상태와 분리해서 기록
// 마감 실패 시 건별 환불 상태로 중복 환불 방지(멱등성)에 사용 (11/16)
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    // 결제 수단 쪽 식별값 (모의 결제 키). 취소·환불 요청 시 사용
    @Column(nullable = false, unique = true, length = 64)
    private String paymentKey;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false)
    private LocalDateTime paidAt;

    private LocalDateTime refundedAt;

    private Payment(Order order, String paymentKey, long amount, LocalDateTime paidAt) {
        this.order = order;
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.status = PaymentStatus.COMPLETED;
        this.paidAt = paidAt;
    }

    public static Payment complete(Order order, String paymentKey, long amount, LocalDateTime paidAt) {
        return new Payment(order, paymentKey, amount, paidAt);
    }
}