package com.moa.domain.groupbuy.entity;

import com.moa.common.exception.BusinessException;
import com.moa.domain.groupbuy.enums.GroupBuyStatus;
import com.moa.domain.groupbuy.exception.GroupBuyErrorCode;
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
    // now를 밖에서 받는 이유: 한 요청 안에서 시간 기준을 하나로 맞추고, 테스트에서 시각을 고정할 수 있게
    public static GroupBuy create(Product product, int wholesalePrice, int targetQuantity, int maxQuantity,
                                  LocalDateTime startAt, LocalDateTime endAt, LocalDateTime now) {
        // 목표 수량은 최대 수량 이하
        if (targetQuantity > maxQuantity) {
            throw new BusinessException(GroupBuyErrorCode.INVALID_QUANTITY);
        }

        // startAt은 생략 가능 → 현재 시각. 요청으로 받으면 현재 이후인지 확인
        LocalDateTime start = (startAt == null) ? now : startAt;
        if (start.isBefore(now) || !endAt.isAfter(start)) {
            throw new BusinessException(GroupBuyErrorCode.INVALID_PERIOD);
        }

        // 도매가 < 정가 검증 (현재 미사용. 쓸 때 에러 코드 INVALID_WHOLESALE_PRICE 추가 후 주석 해제)
        // if (wholesalePrice >= product.getListPrice()) {
        //     throw new BusinessException(GroupBuyErrorCode.INVALID_WHOLESALE_PRICE);
        // }

        return new GroupBuy(product, wholesalePrice, targetQuantity, maxQuantity, start, endAt);
    }

    // 참여 가능 여부: OPEN이고, 시작 시각이 지났고, 마감 전
    // CLOSED(최대 수량 도달)·SUCCEEDED·FAILED는 참여 불가
    public boolean isOrderable(LocalDateTime now) {
        return status == GroupBuyStatus.OPEN
                && !now.isBefore(startAt)
                && now.isBefore(endAt);
    }

    // 남은 수량 (최대 수량 기준)
    public int getRemainingQuantity() {
        return maxQuantity - currentQuantity;
    }

    // 진행률: 목표 달성이면 100, 아니면 소수 첫째 자리까지
    public double getProgressRate() {
        if (currentQuantity >= targetQuantity) {
            return 100.0;
        }
        return Math.round(currentQuantity * 1000.0 / targetQuantity) / 10.0;
    }
}

