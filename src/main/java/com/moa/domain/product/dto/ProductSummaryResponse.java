package com.moa.domain.product.dto;

import com.moa.domain.groupbuy.entity.GroupBuy;
import com.moa.domain.product.entity.Product;

import java.time.LocalDateTime;

// 상품 목록의 한 줄
// 진행 중(OPEN)인 공동구매가 있으면 groupBuy에 담고, 없으면 null
public record ProductSummaryResponse(
        Long productId,
        String name,
        int listPrice,
        String imageUrl,
        GroupBuyInfo groupBuy
) {

    // 목록에 필요한 공동구매 정보만 (명세 기준)
    public record GroupBuyInfo(
            Long groupBuyId,
            int wholesalePrice,
            int currentQuantity,
            int targetQuantity,
            LocalDateTime endAt
    ) {
        static GroupBuyInfo from(GroupBuy groupBuy) {
            return new GroupBuyInfo(
                    groupBuy.getId(),
                    groupBuy.getWholesalePrice(),
                    groupBuy.getCurrentQuantity(),
                    groupBuy.getTargetQuantity(),
                    groupBuy.getEndAt()
            );
        }
    }

    public static ProductSummaryResponse of(Product product, GroupBuy openGroupBuy) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getName(),
                product.getListPrice(),
                product.getImageUrl(),
                openGroupBuy == null ? null : GroupBuyInfo.from(openGroupBuy)
        );
    }
}