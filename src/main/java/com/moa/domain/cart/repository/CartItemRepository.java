package com.moa.domain.cart.repository;

import com.moa.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 묶음 주문: 요청한 항목 중 "내 장바구니에 있는 것"만 조회 (남의 항목·없는 항목은 결과에서 빠짐)
    List<CartItem> findAllByIdInAndCartId(Collection<Long> ids, Long cartId);

    // 담기: 이미 담긴 공동구매인지 (있으면 수량 합산)
    Optional<CartItem> findByCartIdAndGroupBuyId(Long cartId, Long groupBuyId);

    // 조회: 공동구매·상품까지 한 번에 가져와 N+1 방지 (JPQL 없이 @EntityGraph로 fetch join)
    @EntityGraph(attributePaths = {"groupBuy", "groupBuy.product"})
    List<CartItem> findAllByCartIdOrderByIdAsc(Long cartId);
}