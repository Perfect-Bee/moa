package com.moa.domain.groupbuy.enums;

// 공동구매 상태
// OPEN에서 시작. 주문으로 최대 수량에 도달하면 CLOSED(조기 마감, 되돌리지 않음)
// 마감 시각이 지나면 스케줄러가 OPEN, CLOSED를 SUCCEEDED 또는 FAILED로 바꾼다
public enum GroupBuyStatus {
    OPEN,       // 진행 중 (참여 가능)
    CLOSED,     // 최대 수량 도달로 조기 마감 (참여 불가, 마감 시각 전)
    SUCCEEDED,  // 마감 시 목표 수량 달성 -> 구매 확정
    FAILED      // 마감 시 목표 수량 미달 -> 참여자 전원 환불
}