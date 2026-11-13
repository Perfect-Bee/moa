package com.moa.domain.groupbuy.service;


import com.moa.common.exception.BusinessException;
import com.moa.domain.groupbuy.exception.GroupBuyErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

// 공동구매 참여 수량을 Redis에서 관리 (선점용 입구 컷)
// 최종 기준(Source of Truth)은 DB의 currentQuantity, Redis는 동시 참여를 빠르게 거르는 용도
// 값은 Lua에서 tonumber()로 읽을 수 있도록 문자열로 저장 → StringRedisTemplate 사용
@Service
@RequiredArgsConstructor
public class GroupBuyQuantityService {

    // 스크립트는 한 번만 읽어서 재사용. 반환값은 Lua의 정수 → Long
    private static final RedisScript<Long> RESERVE_SCRIPT =
            RedisScript.of(new ClassPathResource("scripts/reserve_quantity.lua"), Long.class);

    private final StringRedisTemplate redisTemplate;

    // 공동구매 개설 시 초기화: 선점 수량 0, 최대 수량 저장
    // DB 커밋 이후에 호출해야 함 (GroupBuyService.createGroupBuy의 afterCommit)
    public void init(Long groupBuyId, int maxQuantity) {
        redisTemplate.opsForValue().set(countKey(groupBuyId), "0");
        redisTemplate.opsForValue().set(maxKey(groupBuyId), String.valueOf(maxQuantity));
    }

    // 수량 선점: "count + quantity <= max"면 증가, 아니면 예외 (Lua로 원자적 실행)
    public void reserve(Long groupBuyId, int quantity) {
        Long result = redisTemplate.execute(RESERVE_SCRIPT,
                List.of(countKey(groupBuyId), maxKey(groupBuyId)),
                String.valueOf(quantity));

        if (result == null || result == -2L) {
            throw new BusinessException(GroupBuyErrorCode.QUANTITY_NOT_READY);
        }
        if (result == -1L) {
            throw new BusinessException(GroupBuyErrorCode.EXCEEDS_REMAINING_QUANTITY);
        }
    }

    // 선점 반납: 결제 실패·DB 반영 실패(보상 처리), 주문 취소 시
    public void release(Long groupBuyId, int quantity) {
        redisTemplate.opsForValue().decrement(countKey(groupBuyId), quantity);
    }

    // 선점된 참여 수량
    private String countKey(Long groupBuyId) {
        return "groupbuy:" + groupBuyId + ":count";
    }

    // 최대 수량
    private String maxKey(Long groupBuyId) {
        return "groupbuy:" + groupBuyId + ":max";
    }
}