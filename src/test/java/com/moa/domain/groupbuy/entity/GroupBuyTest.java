package com.moa.domain.groupbuy.entity;

import com.moa.common.exception.BusinessException;
import com.moa.domain.groupbuy.enums.GroupBuyStatus;
import com.moa.domain.groupbuy.exception.GroupBuyErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

// GroupBuy 도메인 단위 테스트 (Spring·DB·Redis 없이)
// now를 밖에서 받는 구조라 시각을 고정해서 테스트할 수 있음
class GroupBuyTest {

    // 모든 테스트에서 같은 기준 시각 사용
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 12, 0);

    // 정상 공동구매 생성 (도매가 1000, 목표 10, 최대 20, 지금 시작, 1시간 뒤 마감)
    // Product는 create 안에서 쓰이지 않아서 null
    private GroupBuy createDefault() {
        return GroupBuy.create(null, 1000, 10, 20, null, NOW.plusHours(1), NOW);
    }

    // ===== create =====

    @Test
    @DisplayName("목표 수량이 최대 수량보다 크면 INVALID_QUANTITY")
    void create_targetGreaterThanMax() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> GroupBuy.create(null, 1000, 30, 20, null, NOW.plusHours(1), NOW));

        assertThat(e.getErrorCode()).isEqualTo(GroupBuyErrorCode.INVALID_QUANTITY);
    }

    @Test
    @DisplayName("시작 시각이 현재보다 과거면 INVALID_PERIOD")
    void create_startInPast() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> GroupBuy.create(null, 1000, 10, 20, NOW.minusMinutes(1), NOW.plusHours(1), NOW));

        assertThat(e.getErrorCode()).isEqualTo(GroupBuyErrorCode.INVALID_PERIOD);
    }

    @Test
    @DisplayName("마감 시각이 시작 시각과 같거나 이전이면 INVALID_PERIOD")
    void create_endNotAfterStart() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> GroupBuy.create(null, 1000, 10, 20, null, NOW, NOW));

        assertThat(e.getErrorCode()).isEqualTo(GroupBuyErrorCode.INVALID_PERIOD);
    }

    @Test
    @DisplayName("startAt을 생략하면 현재 시각으로 시작하고, OPEN · 참여 0으로 생성")
    void create_startAtNull() {
        GroupBuy groupBuy = createDefault();

        assertThat(groupBuy.getStartAt()).isEqualTo(NOW);
        assertThat(groupBuy.getStatus()).isEqualTo(GroupBuyStatus.OPEN);
        assertThat(groupBuy.getCurrentQuantity()).isZero();
    }

    // ===== isOrderable =====

    @Test
    @DisplayName("시작 이후 ~ 마감 전이면 참여 가능")
    void isOrderable_withinPeriod() {
        GroupBuy groupBuy = createDefault();

        assertThat(groupBuy.isOrderable(NOW.plusMinutes(30))).isTrue();
    }

    @Test
    @DisplayName("마감 시각과 같으면 참여 불가 (경계값)")
    void isOrderable_atEndAt() {
        GroupBuy groupBuy = createDefault();

        assertThat(groupBuy.isOrderable(NOW.plusHours(1))).isFalse();
    }

    // ===== getProgressRate =====
    // currentQuantity를 바꾸는 메서드가 없어서 테스트에서만 ReflectionTestUtils로 값을 넣음

    @Test
    @DisplayName("목표 수량을 넘으면 진행률 100으로 고정")
    void progressRate_overTarget() {
        GroupBuy groupBuy = createDefault();
        ReflectionTestUtils.setField(groupBuy, "currentQuantity", 15);

        assertThat(groupBuy.getProgressRate()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("진행률은 소수 첫째 자리까지 반올림")
    void progressRate_rounding() {
        GroupBuy groupBuy = GroupBuy.create(
                null, 1000, 3, 20, null, NOW.plusHours(1), NOW
        );
        ReflectionTestUtils.setField(groupBuy, "currentQuantity", 1);

        assertThat(groupBuy.getProgressRate()).isEqualTo(33.3);
    }
}