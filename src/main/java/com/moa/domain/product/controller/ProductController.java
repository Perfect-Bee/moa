package com.moa.domain.product.controller;

import com.moa.common.dto.response.BaseResponse;
import com.moa.common.dto.response.PageResponse;
import com.moa.domain.groupbuy.enums.GroupBuyStatus;
import com.moa.domain.product.dto.ProductCreateRequest;
import com.moa.domain.product.dto.ProductCreateResponse;
import com.moa.domain.product.dto.ProductDetailResponse;
import com.moa.domain.product.dto.ProductSummaryResponse;
import com.moa.domain.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    // 상품 목록 (비로그인 가능)
    // groupBuyStatus는 선택. 없으면 전체, 있으면 해당 상태의 공동구매가 있는 상품만
    // 잘못된 값(OPEN/SUCCEEDED/FAILED 외)은 enum 변환 단계에서 걸러짐
    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<ProductSummaryResponse>>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) GroupBuyStatus groupBuyStatus
    ) {
        PageResponse<ProductSummaryResponse> response = productService.getProducts(page, size, groupBuyStatus);
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK, "상품 목록 조회에 성공했습니다", response));
    }

    // 상품 상세 (비로그인 가능)
    @GetMapping("/{productId}")
    public ResponseEntity<BaseResponse<ProductDetailResponse>> getProduct(
            @PathVariable Long productId
    ) {
        ProductDetailResponse response = productService.getProduct(productId);
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK, "상품 조회에 성공했습니다", response));
    }
}