package com.moa.domain.groupbuy.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

// 공동구매 참여 수량을 Redis에서 관리 (선점용 입구 컷)
// 최종 기준(Source of Truth)은 DB의 currentQuantity, Redis는 동시 참여를 빠르게 거르는 용도
// 값은 Lua에서 tonumber()로 읽을 수 있도록 문자열로 저장 → StringRedisTemplate 사용
@Service
@RequiredArgsConstructor
public class GroupBuyQuantityService {

    private final StringRedisTemplate redisTemplate;

    // 공동구매 개설 시 초기화: 선점 수량 0, 최대 수량 저장
    // DB 커밋 이후에 호출해야 함 (GroupBuyService.createGroupBuy의 afterCommit)
    public void init(Long groupBuyId, int maxQuantity) {
        redisTemplate.opsForValue().set(countKey(groupBuyId), "0");
        redisTemplate.opsForValue().set(maxKey(groupBuyId), String.valueOf(maxQuantity));
    }

    // reserve(Lua 선점), release(반납)는 주문 단계에서 추가

    // 선점된 참여 수량
    private String countKey(Long groupBuyId) {
        return "groupbuy:" + groupBuyId + ":count";
    }

    // 최대 수량
    private String maxKey(Long groupBuyId) {
        return "groupbuy:" + groupBuyId + ":max";
    }
}