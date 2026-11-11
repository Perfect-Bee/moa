package com.moa.domain.product.dto;

import com.moa.domain.groupbuy.entity.GroupBuy;
import com.moa.domain.groupbuy.enums.GroupBuyStatus;
import com.moa.domain.product.entity.Product;

import java.time.LocalDateTime;

// 상품 상세 + 채팅방 ID + 가장 최근 공동구매
// 공동구매를 한 번도 연 적 없으면 groupBuy = null
public record ProductDetailResponse(
        Long productId,
        String name,
        String description,
        int listPrice,
        String imageUrl,
        Long chatRoomId,
        GroupBuyInfo groupBuy
) {

    // 상세에는 상한(maxQuantity)과 상태(status)까지 포함 (명세 기준)
    public record GroupBuyInfo(
            Long groupBuyId,
            int wholesalePrice,
            int targetQuantity,
            int maxQuantity,
            int currentQuantity,
            GroupBuyStatus status,
            LocalDateTime endAt
    ) {
        static GroupBuyInfo from(GroupBuy groupBuy) {
            return new GroupBuyInfo(
                    groupBuy.getId(),
                    groupBuy.getWholesalePrice(),
                    groupBuy.getTargetQuantity(),
                    groupBuy.getMaxQuantity(),
                    groupBuy.getCurrentQuantity(),
                    groupBuy.getStatus(),
                    groupBuy.getEndAt()
            );
        }
    }

    public static ProductDetailResponse of(Product product, Long chatRoomId, GroupBuy latestGroupBuy) {
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getListPrice(),
                product.getImageUrl(),
                chatRoomId,
                latestGroupBuy == null ? null : GroupBuyInfo.from(latestGroupBuy)
        );
    }
}