package com.moa.domain.order.entity;

import com.moa.domain.groupbuy.entity.GroupBuy;
import com.moa.domain.order.enums.OrderStatus;
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

    public static Order create(User user, GroupBuy groupBuy, int quantity, LocalDateTime now) {
        return new Order(user, groupBuy, quantity, now);
    }
}