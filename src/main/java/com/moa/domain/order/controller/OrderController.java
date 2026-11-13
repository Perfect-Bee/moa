package com.moa.domain.order.controller;

import com.moa.common.dto.response.BaseResponse;
import com.moa.domain.order.dto.OrderBundleResponse;
import com.moa.domain.order.dto.OrderCreateRequest;
import com.moa.domain.order.dto.OrderResponse;
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
}