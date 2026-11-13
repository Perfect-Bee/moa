package com.moa.domain.groupbuy.repository;

import com.moa.domain.groupbuy.entity.GroupBuy;
import com.moa.domain.groupbuy.enums.GroupBuyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    // 주문: 참여 수량 증가 (조건부 UPDATE = DB 최종 방어선)
    // 조건을 만족하지 않으면 영향 행 0 -> 호출한 쪽에서 실패 처리
    // max에 도달하면 같은 UPDATE에서 CLOSED로 전환
    //   -> SET 순서: status를 먼저 둔다. MySQL은 SET을 왼쪽부터 적용해서
    //   -> current_quantity를 먼저 바꾸면 CASE가 바뀐 값을 보고 판단함 (H2는 바뀌기 전 값)
    //   -> status를 먼저 두면 두 DB 모두 "바뀌기 전 값"으로 판단 -> 결과 동일
    @Modifying
    @Query(value = """
            UPDATE group_buys
            SET status = CASE WHEN current_quantity + :quantity >= max_quantity
                              THEN 'CLOSED' ELSE status END,
                current_quantity = current_quantity + :quantity
            WHERE id = :id
              AND status = 'OPEN'
              AND end_at > :now
              AND current_quantity + :quantity <= max_quantity
            """, nativeQuery = true)
    int increaseQuantity(@Param("id") Long id,
                         @Param("quantity") int quantity,
                         @Param("now") LocalDateTime now);
}