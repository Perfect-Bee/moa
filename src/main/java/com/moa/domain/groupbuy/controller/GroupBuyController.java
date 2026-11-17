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

import com.moa.domain.groupbuy.service.GroupBuySseService;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GroupBuyController {

    private final GroupBuyService groupBuyService;
    private final GroupBuySseService groupBuySseService;

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
    public ResponseEntity<BaseResponse<GroupBuyProgressResponse>> getProgress(
            @PathVariable Long groupBuyId
    ) {
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK, "공동구매 진행 현황 조회에 성공했습니다",
                groupBuyService.getProgress(groupBuyId)));
    }

    // 공동구매 실시간 구독 (SSE, 비로그인 가능)
    // BaseResponse로 감싸지 않음: 응답이 JSON 한 번이 아니라 이벤트 스트림이라서
    @GetMapping(value = "/group-buys/{groupBuyId}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @PathVariable Long groupBuyId
    ) {
        return groupBuySseService.subscribe(groupBuyId);
    }
}