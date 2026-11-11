package com.moa.domain.groupbuy.entity;

import com.moa.domain.groupbuy.enums.GroupBuyStatus;
import com.moa.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 공동구매 회차. 한 상품에 여러 번 열 수 있지만 OPEN은 동시에 1개만 (개설 API에서 검사 예정)
// 재고 개념 없이, 참여 수량을 모아 마감 시 목표 달성 여부로 성사/실패를 판정한다
@Entity
@Table(name = "group_buys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupBuy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 공동구매 도매가 (정가보다 낮은 가격)
    @Column(nullable = false)
    private int wholesalePrice;

    // 성사 기준 수량. 마감 시 이 값 이상이면 SUCCEEDED
    @Column(nullable = false)
    private int targetQuantity;

    // 참여 상한. 넘지 않도록 Redis 선점 + 원자적 UPDATE로 막을 예정
    @Column(nullable = false)
    private int maxQuantity;

    // 현재까지 참여한 수량
    @Column(nullable = false)
    private int currentQuantity;

    // STRING으로 저장: ORDINAL(0,1,2)은 enum 순서가 바뀌면 기존 데이터 의미가 틀어짐
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupBuyStatus status;

    @Column(nullable = false)
    private LocalDateTime startAt;

    // 마감 시각. 스케줄러가 이 시각이 지난 OPEN 공동구매를 판정
    @Column(nullable = false)
    private LocalDateTime endAt;

    // 낙관적 락용 버전 (ERD 기준으로 추가)
    // 벌크 UPDATE로 수량을 바꾸면 자동 증가하지 않으므로 주문 단계에서 유지 여부 결정
    @Version
    private Long version;

    private GroupBuy(Product product, int wholesalePrice, int targetQuantity, int maxQuantity,
                     LocalDateTime startAt, LocalDateTime endAt) {
        this.product = product;
        this.wholesalePrice = wholesalePrice;
        this.targetQuantity = targetQuantity;
        this.maxQuantity = maxQuantity;
        this.currentQuantity = 0;             // 개설 직후엔 참여 0
        this.status = GroupBuyStatus.OPEN;    // 개설 즉시 진행 중
        this.startAt = startAt;
        this.endAt = endAt;
    }

    // 입력값 검증(target <= max, endAt > startAt 등)은 공동구매 개설 API를 만들 때 추가
    public static GroupBuy create(Product product, int wholesalePrice, int targetQuantity, int maxQuantity,
                                  LocalDateTime startAt, LocalDateTime endAt) {
        return new GroupBuy(product, wholesalePrice, targetQuantity, maxQuantity, startAt, endAt);
    }
}