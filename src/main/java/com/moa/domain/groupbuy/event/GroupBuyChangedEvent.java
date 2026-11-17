package com.moa.domain.groupbuy.event;

// 공동구매 수량·상태가 바뀜 (주문, 취소, 마감 판정)
// 값은 담지 않음 -> 커밋 후 리스너가 최신 값을 다시 조회
public record GroupBuyChangedEvent(Long groupBuyId) {
}