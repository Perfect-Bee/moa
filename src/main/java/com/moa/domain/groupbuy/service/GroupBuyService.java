package com.moa.domain.groupbuy.service;

import com.moa.common.exception.BusinessException;
import com.moa.domain.groupbuy.dto.GroupBuyCreateRequest;
import com.moa.domain.groupbuy.dto.GroupBuyCreateResponse;
import com.moa.domain.groupbuy.dto.GroupBuyProgressResponse;
import com.moa.domain.groupbuy.entity.GroupBuy;
import com.moa.domain.groupbuy.enums.GroupBuyStatus;
import com.moa.domain.groupbuy.exception.GroupBuyErrorCode;
import com.moa.domain.groupbuy.repository.GroupBuyRepository;
import com.moa.domain.product.entity.Product;
import com.moa.domain.product.exception.ProductErrorCode;
import com.moa.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupBuyService {

    // 진행 중으로 보는 상태. 하나라도 있으면 새 공동구매를 열 수 없음
    private static final List<GroupBuyStatus> IN_PROGRESS = List.of(GroupBuyStatus.OPEN, GroupBuyStatus.CLOSED);

    private final ProductRepository productRepository;
    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyQuantityService groupBuyQuantityService;


    // 공동구매 개설
    @Transactional
    public GroupBuyCreateResponse createGroupBuy(Long sellerId, Long productId, GroupBuyCreateRequest request) {
        // 1. 상품 행 잠금 조회 → 같은 상품의 동시 개설 요청은 여기서 줄을 섬
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 2. 본인 상품인지 (seller는 LAZY 프록시라 getId()는 추가 쿼리 없음)
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new BusinessException(GroupBuyErrorCode.NOT_PRODUCT_OWNER);
        }

        // 3. 진행 중(OPEN·CLOSED) 공동구매 중복 검사 (잠금 상태라 동시 요청도 순서대로 검사)
        if (groupBuyRepository.existsByProductIdAndStatusIn(productId, IN_PROGRESS)) {
            throw new BusinessException(GroupBuyErrorCode.GROUP_BUY_ALREADY_OPEN);
        }

        // 4. 생성(엔티티에서 값 검증) + 저장
        GroupBuy groupBuy = groupBuyRepository.save(GroupBuy.create(
                product, request.wholesalePrice(), request.targetQuantity(), request.maxQuantity(),
                request.startAt(), request.endAt(), LocalDateTime.now()));

        // 5. 커밋 후 Redis 수량 초기화
        // 트랜잭션 안에서 넣으면 DB가 롤백돼도 Redis에 키가 남으므로, 커밋 성공 후에만 실행
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                groupBuyQuantityService.init(groupBuy.getId(), groupBuy.getMaxQuantity());
            }
        });

        // 분산락(@RedissonLock) 추후 적용 예정
        return GroupBuyCreateResponse.from(groupBuy);
    }

    // 공동구매 진행 현황
    public GroupBuyProgressResponse getProgress(Long groupBuyId) {
        GroupBuy groupBuy = groupBuyRepository.findById(groupBuyId)
                .orElseThrow(() -> new BusinessException(GroupBuyErrorCode.GROUP_BUY_NOT_FOUND));
        return GroupBuyProgressResponse.from(groupBuy);
    }
}