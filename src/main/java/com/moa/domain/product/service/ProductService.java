package com.moa.domain.product.service;

import com.moa.common.dto.response.PageResponse;
import com.moa.common.exception.BusinessException;
import com.moa.domain.chat.entity.ChatRoom;
import com.moa.domain.chat.repository.ChatRoomRepository;
import com.moa.domain.groupbuy.entity.GroupBuy;
import com.moa.domain.groupbuy.enums.GroupBuyStatus;
import com.moa.domain.groupbuy.repository.GroupBuyRepository;
import com.moa.domain.product.dto.ProductCreateRequest;
import com.moa.domain.product.dto.ProductCreateResponse;
import com.moa.domain.product.dto.ProductDetailResponse;
import com.moa.domain.product.dto.ProductSummaryResponse;
import com.moa.domain.product.entity.Product;
import com.moa.domain.product.exception.ProductErrorCode;
import com.moa.domain.product.repository.ProductRepository;
import com.moa.domain.user.entity.User;
import com.moa.domain.user.exception.UserErrorCode;
import com.moa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본은 읽기 전용, 저장하는 메서드만 따로 열어줌
public class ProductService {

    private static final int MAX_PAGE_SIZE = 100; // 한 번에 너무 많이 조회하지 않도록 상한

    private final ProductRepository productRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final GroupBuyRepository groupBuyRepository;
    private final UserRepository userRepository;

    // 상품 등록 + 채팅방 생성
    // 한 트랜잭션이라 채팅방 저장이 실패하면 상품 저장도 롤백된다
    @Transactional
    public ProductCreateResponse createProduct(Long sellerId, ProductCreateRequest request) {
        // 1. 판매자 조회 (토큰은 유효하지만 회원이 없는 경우 404)
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // 2. 상품 저장
        Product product = productRepository.save(Product.create(
                seller, request.name(), request.description(), request.listPrice(), request.imageUrl()));

        // 3. 상품 채팅방 저장
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.create(product));

        return new ProductCreateResponse(product.getId(), chatRoom.getId());
    }

    // 상품 목록 (최신 등록순), groupBuyStatus가 있으면 해당 상태의 공동구매가 있는 상품만
    public PageResponse<ProductSummaryResponse> getProducts(int page, int size, GroupBuyStatus groupBuyStatus) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ProductErrorCode.INVALID_PAGE_REQUEST);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        // 1. 상품 페이지 조회 (네이티브 쿼리라 enum은 문자열로 넘김)
        Page<Product> products = (groupBuyStatus == null)
                ? productRepository.findAll(pageable)
                : productRepository.findAllByGroupBuyStatus(groupBuyStatus.name(), pageable);

        // 2. 이번 페이지 상품들의 OPEN 공동구매를 한 번에 조회해서 상품 ID → 공동구매로 묶기
        //    상품마다 따로 조회하면 상품 수만큼 쿼리가 나감 (N+1)
        List<Long> productIds = products.getContent().stream()
                .map(Product::getId)
                .toList();

        Map<Long, GroupBuy> openGroupBuyByProductId = productIds.isEmpty()
                ? Map.of()
                : groupBuyRepository.findByProductIdInAndStatus(productIds, GroupBuyStatus.OPEN).stream()
                        // getProduct().getId()는 LAZY 프록시여도 추가 쿼리 없이 ID 반환
                        // 상품당 OPEN이 2개면 중복 키 예외 → 공동구매 개설 API에서 1개 제한 필요
                        .collect(Collectors.toMap(g -> g.getProduct().getId(), Function.identity()));

        // 3. 상품 + (있으면) 진행 중 공동구매로 응답 조립
        return PageResponse.from(products.map(product ->
                ProductSummaryResponse.of(product, openGroupBuyByProductId.get(product.getId()))));
    }

    // 상품 상세
    public ProductDetailResponse getProduct(Long productId) {
        // 1. 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 2. 채팅방 ID (등록 시 항상 함께 생성되므로 없으면 데이터 이상)
        Long chatRoomId = chatRoomRepository.findByProductId(productId)
                .map(ChatRoom::getId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.CHAT_ROOM_NOT_FOUND));

        // 3. 가장 최근 공동구매 (없으면 null)
        GroupBuy latestGroupBuy = groupBuyRepository.findFirstByProductIdOrderByIdDesc(productId)
                .orElse(null);

        return ProductDetailResponse.of(product, chatRoomId, latestGroupBuy);
    }
}