package com.moa.domain.order.entity;

import com.moa.common.exception.BusinessException;
import com.moa.domain.groupbuy.entity.GroupBuy;
import com.moa.domain.order.enums.OrderStatus;
import com.moa.domain.order.exception.OrderErrorCode;
import com.moa.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

// 주문 1건 = 공동구매 1개 참여 (ERD의 ORDER_ITEMS는 사용하지 않고 이 테이블에 합침)
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 화면·문의용 주문 번호 (예: 20261113-1A2B3C4D)
    @Column(nullable = false, unique = true, length = 30)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_buy_id", nullable = false)
    private GroupBuy groupBuy;

    @Column(nullable = false)
    private int quantity;

    // 주문 시점의 도매가 (이후 가격이 바뀌어도 주문 금액은 유지)
    @Column(nullable = false)
    private int unitPrice;

    @Column(nullable = false)
    private long totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Order(User user, GroupBuy groupBuy, int quantity, LocalDateTime now) {
        this.orderNumber = now.format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.user = user;
        this.groupBuy = groupBuy;
        this.quantity = quantity;
        this.unitPrice = groupBuy.getWholesalePrice();
        this.totalPrice = (long) unitPrice * quantity;
        this.status = OrderStatus.PAID; // 결제 성공 후 생성
    }

    // 본인 주문인지 (user는 LAZY 프록시 -> getId()는 추가 쿼리 없음)
    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    // 사용자 취소: PAID만 가능
    // 공동구매가 OPEN인지는 DB 조건부 UPDATE가 판단 (동시성 때문에 엔티티 값으로 판단하지 않음)
    public void cancel() {
        if (status != OrderStatus.PAID) {
            throw new BusinessException(OrderErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.CANCELED;
    }

    // -------- 공동 구매 -------- //

    // 공동구매 성사: PAID -> CONFIRMED
    // 스케줄러가 PAID 주문만 조회하므로 실제로 걸릴 일은 없지만, 잘못된 전이를 막는 방어선
    public void confirm() {
        if (status != OrderStatus.PAID) {
            throw new BusinessException(OrderErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    // 공동구매 실패: PAID -> REFUNDED (사용자 취소인 CANCELED와 구분)
    public void refund() {
        if (status != OrderStatus.PAID) {
            throw new BusinessException(OrderErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.REFUNDED;
    }

    public static Order create(User user, GroupBuy groupBuy, int quantity, LocalDateTime now) {
        return new Order(user, groupBuy, quantity, now);
    }
}