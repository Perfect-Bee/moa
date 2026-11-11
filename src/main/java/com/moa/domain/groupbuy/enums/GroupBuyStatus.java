package com.moa.domain.groupbuy.enums;

// 공동구매 상태
// OPEN에서 시작해 마감 스케줄러가 SUCCEEDED 또는 FAILED로 바꾼다
public enum GroupBuyStatus {
    OPEN,       // 진행 중 (참여 가능)
    SUCCEEDED,  // 마감 시 목표 수량 달성 → 구매 확정
    FAILED      // 마감 시 목표 수량 미달 → 참여자 전원 환불
}