package com.moa.domain.groupbuy.dto;

import com.moa.domain.groupbuy.entity.GroupBuy;
import com.moa.domain.groupbuy.enums.GroupBuyStatus;

import java.time.LocalDateTime;

// 명세 「공동구매 진행 현황 조회」 응답 형식
public record GroupBuyProgressResponse(
        Long groupBuyId, Long productId, int wholesalePrice,
        int currentQuantity, int targetQuantity, int maxQuantity,
        int remainingQuantity, double progressRate,
        GroupBuyStatus status, LocalDateTime endAt
) {
    public static GroupBuyProgressResponse from(GroupBuy g) {
        return new GroupBuyProgressResponse(
                g.getId(), g.getProduct().getId(), g.getWholesalePrice(),
                g.getCurrentQuantity(), g.getTargetQuantity(), g.getMaxQuantity(),
                g.getRemainingQuantity(), g.getProgressRate(),
                g.getStatus(), g.getEndAt());
    }
}