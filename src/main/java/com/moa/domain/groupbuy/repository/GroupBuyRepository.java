package com.moa.domain.groupbuy.repository;

import com.moa.domain.groupbuy.entity.GroupBuy;
import com.moa.domain.groupbuy.enums.GroupBuyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GroupBuyRepository extends JpaRepository<GroupBuy, Long> {

    // 상품 목록: 이번 페이지 상품들의 OPEN 공동구매를 IN 쿼리 한 번으로 조회 (N+1 방지)
    // ProductId → product.id 로 해석됨
    List<GroupBuy> findByProductIdInAndStatus(Collection<Long> productIds, GroupBuyStatus status);

    // 상품 상세: 가장 최근에 열린 공동구매 (마감 후에도 SUCCEEDED/FAILED 상태를 보여주기 위함)
    Optional<GroupBuy> findFirstByProductIdOrderByIdDesc(Long productId);

    // 개설: 이 상품에 진행 중(OPEN·CLOSED) 공동구매가 있는지
    boolean existsByProductIdAndStatusIn(Long productId, Collection<GroupBuyStatus> statuses);
}