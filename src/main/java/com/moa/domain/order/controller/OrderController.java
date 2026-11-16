package com.moa.domain.order.controller;

import com.moa.common.dto.response.BaseResponse;
import com.moa.common.dto.response.PageResponse;
import com.moa.domain.order.dto.*;
import com.moa.domain.order.exception.OrderErrorCode;
import com.moa.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 로그인 사용자 전용 (SecurityConfig의 anyRequest().authenticated()에 해당)
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 주문 생성 (공동구매 참여)
    // 단건: 성공 201 / 실패는 에러 코드 그대로
    // 묶음: 하나라도 성공 201, 전부 실패 409 (둘 다 본문에 orders + failedItems)
    @PostMapping
    public ResponseEntity<BaseResponse<?>> createOrder(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        request.validate();

        if (request.isCartOrder()) {
            OrderBundleResponse response = orderService.createFromCart(userId, request.cartItemIds());
            if (response.orders().isEmpty()) {
                OrderErrorCode error = OrderErrorCode.ALL_ITEMS_FAILED;
                return ResponseEntity.status(error.getHttpStatus())
                        .body(BaseResponse.fail(error.getHttpStatus(), error.getMessage(), response));
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(BaseResponse.success(HttpStatus.CREATED, "주문이 완료되었습니다", response));
        }

        OrderResponse response = orderService.createSingle(userId, request.groupBuyId(), request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(HttpStatus.CREATED, "주문이 완료되었습니다", response));
    }

    // 주문 목록 조회
    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<OrderSummaryResponse>>> getOrders(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<OrderSummaryResponse> response = orderService.getOrders(userId, page, size);
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK, "주문 목록 조회에 성공했습니다", response));
    }

    // 주문(결제) 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<BaseResponse<OrderDetailResponse>> getOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orderId
    ) {
        OrderDetailResponse response = orderService.getOrder(userId, orderId);
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK, "주문 조회에 성공했습니다", response));
    }

    // 주문 취소
    // DELETE가 아닌 POST : 삭제가 아니라 상태 변경 + 환불, 수량 복구
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<BaseResponse<OrderCancelResponse>> cancelOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orderId
    ) {
        OrderCancelResponse response = orderService.cancel(userId, orderId);
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK, "주문이 취소되었습니다", response));
    }
}