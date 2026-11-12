package com.moa.domain.groupbuy.dto;

import com.moa.domain.groupbuy.entity.GroupBuy;
import com.moa.domain.groupbuy.enums.GroupBuyStatus;

public record GroupBuyCreateResponse(Long groupBuyId, GroupBuyStatus status) {
    public static GroupBuyCreateResponse from(GroupBuy groupBuy) {
        return new GroupBuyCreateResponse(groupBuy.getId(), groupBuy.getStatus());
    }
}