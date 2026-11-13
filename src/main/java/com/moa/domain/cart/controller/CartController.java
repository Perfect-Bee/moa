package com.moa.domain.cart.controller;

import com.moa.common.dto.response.BaseResponse;
import com.moa.domain.cart.dto.*;
import com.moa.domain.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 로그인 사용자 전용 (SecurityConfig의 anyRequest().authenticated()에 해당)
@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // 장바구니 조회
    @GetMapping
    public ResponseEntity<BaseResponse<CartResponse>> getCart(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK, "장바구니 조회에 성공했습니다",
                cartService.getCart(userId)));
    }

    // 장바구니 담기: 새로 담으면 201, 기존 항목에 합산하면 200
    @PostMapping("/items")
    public ResponseEntity<BaseResponse<CartItemAddResponse>> addItem(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CartItemAddRequest request
    ) {
        CartItemAddResponse response = cartService.addItem(userId, request);
        HttpStatus status = response.created() ? HttpStatus.CREATED : HttpStatus.OK;
        String message = response.created() ? "장바구니에 담았습니다" : "장바구니 수량을 합산했습니다";
        return ResponseEntity.status(status).body(BaseResponse.success(status, message, response));
    }

    // 장바구니 수량 변경
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<BaseResponse<CartItemUpdateResponse>> updateQuantity(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartItemUpdateRequest request
    ) {
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK, "장바구니 수량을 변경했습니다",
                cartService.updateQuantity(userId, cartItemId, request)));
    }

    // 장바구니 항목 삭제 (204는 본문 없음)
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> deleteItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cartItemId
    ) {
        cartService.deleteItem(userId, cartItemId);
        return ResponseEntity.noContent().build();
    }
}