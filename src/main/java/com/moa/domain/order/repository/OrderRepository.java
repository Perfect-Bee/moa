package com.moa.domain.order.repository;

import com.moa.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 내 주문 목록 (최신순). 정렬은 SQL에 고정 → PageRequest에는 Sort를 넣지 않음
    @Query(value = """
            SELECT * FROM orders
            WHERE user_id = :userId
            ORDER BY id DESC
            """,
            countQuery = "SELECT COUNT(*) FROM orders WHERE user_id = :userId",
            nativeQuery = true)
    Page<Order> findPageByUserId(@Param("userId") Long userId, Pageable pageable);

    // 취소: 주문 행 잠금 -> 같은 주문 동시 취소 시 두 번째는 대기
    @Query(value = "SELECT * FROM orders " +
            "WHERE id = :id FOR UPDATE",
            nativeQuery = true)
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
}