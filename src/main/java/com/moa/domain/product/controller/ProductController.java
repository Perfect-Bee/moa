package com.moa.domain.product.controller;

import com.moa.common.dto.response.BaseResponse;
import com.moa.domain.product.dto.ProductCreateRequest;
import com.moa.domain.product.dto.ProductCreateResponse;
import com.moa.domain.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 상품 등록 (SELLER 권한은 SecurityConfig의 hasRole("SELLER")에서 확인)
    // @AuthenticationPrincipal: JwtFilter가 SecurityContext에 넣어둔 principal(userId)
    @PostMapping
    public ResponseEntity<BaseResponse<ProductCreateResponse>> createProduct(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ProductCreateRequest request
    ) {
        ProductCreateResponse response = productService.createProduct(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(HttpStatus.CREATED, "상품이 등록되었습니다", response));
    }
}