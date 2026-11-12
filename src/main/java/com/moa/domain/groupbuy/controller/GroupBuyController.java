package com.moa.domain.groupbuy.controller;

import com.moa.common.dto.response.BaseResponse;
import com.moa.domain.groupbuy.dto.GroupBuyCreateRequest;
import com.moa.domain.groupbuy.dto.GroupBuyCreateResponse;
import com.moa.domain.groupbuy.dto.GroupBuyProgressResponse;
import com.moa.domain.groupbuy.service.GroupBuyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GroupBuyController {

    private final GroupBuyService groupBuyService;

    // 공동구매 개설 (SELLER 권한은 SecurityConfig에서 확인)
    @PostMapping("/products/{productId}/group-buys")
    public ResponseEntity<BaseResponse<GroupBuyCreateResponse>> createGroupBuy(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId,

            @Valid
            @RequestBody
            GroupBuyCreateRequest request
    ) {
        GroupBuyCreateResponse response = groupBuyService.createGroupBuy(userId, productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(HttpStatus.CREATED, "공동구매가 개설되었습니다", response));
    }

    // 공동구매 진행 현황 (비로그인 가능)
    @GetMapping("/group-buys/{groupBuyId}")
    public ResponseEntity<BaseResponse<GroupBuyProgressResponse>> getProgress(@PathVariable Long groupBuyId) {
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK, "공동구매 진행 현황 조회에 성공했습니다",
                groupBuyService.getProgress(groupBuyId)));
    }
}