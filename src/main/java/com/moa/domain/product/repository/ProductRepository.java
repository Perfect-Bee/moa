package com.moa.domain.product.repository;

import com.moa.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 목록 조회 groupBuyStatus 필터: 해당 상태의 공동구매가 하나라도 있는 상품
    // 엔티티가 아니라 실제 테이블·컬럼 이름(products, group_buys, product_id)을 쓴다
    // JOIN 대신 EXISTS: 공동구매 회차가 여러 개여도 상품이 중복으로 나오지 않고 count도 정확함
    // countQuery: 페이징의 totalElements 계산용 (네이티브 쿼리는 직접 지정)
    // status는 String: 네이티브 쿼리에 enum을 넘기면 'OPEN' 대신 순번(0,1,2)으로 바인딩될 수 있음
    @Query(value = """
            SELECT p.*
            FROM products p
            WHERE EXISTS (
                SELECT 1
                FROM group_buys g
                WHERE g.product_id = p.id
                  AND g.status = :status
            )
            """,
           countQuery = """
            SELECT COUNT(*)
            FROM products p
            WHERE EXISTS (
                SELECT 1
                FROM group_buys g
                WHERE g.product_id = p.id
                  AND g.status = :status
            )
            """,
           nativeQuery = true)
    Page<Product> findAllByGroupBuyStatus(@Param("status") String status, Pageable pageable);

    // 공동구매 개설: 상품 행을 잠가서 동시 개설 요청을 줄 세움 (트랜잭션 안에서만 의미 있음)
    @Query(value =
            "SELECT * " +
            "FROM products " +
            "WHERE id = :id FOR UPDATE",
            nativeQuery = true)
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}