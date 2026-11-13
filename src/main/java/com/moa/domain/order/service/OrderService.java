package com.moa.domain.order.service;

import com.moa.common.exception.BusinessException;
import com.moa.domain.cart.entity.Cart;
import com.moa.domain.cart.entity.CartItem;
import com.moa.domain.cart.exception.CartErrorCode;
import com.moa.domain.cart.repository.CartItemRepository;
import com.moa.domain.cart.repository.CartRepository;
import com.moa.domain.order.dto.FailedItemResponse;
import com.moa.domain.order.dto.OrderBundleResponse;
import com.moa.domain.order.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 주문 진입점. 트랜잭션은 OrderProcessor가 항목마다 따로 연다
// 이 클래스에 @Transactional을 붙이면 항목들이 하나의 트랜잭션으로 묶여
// 한 항목 실패 시 전체가 롤백되므로 붙이지 않음
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderProcessor orderProcessor;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    // 단건 바로 주문 (상품 화면·채팅방에서): 성공 아니면 예외
    public OrderResponse createSingle(Long userId, Long groupBuyId, int quantity) {
        return orderProcessor.process(userId, groupBuyId, quantity, null);
    }

    // 장바구니 묶음 주문: 항목별 독립 처리, 실패한 항목만 실패
    public OrderBundleResponse createFromCart(Long userId, List<Long> cartItemIds) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(CartErrorCode.CART_NOT_FOUND));

        // 내 장바구니 항목만 조회 (남의 항목·없는 항목은 결과에 없음 → 아래에서 실패 처리)
        // getGroupBuy().getId()는 LAZY 프록시의 ID라 트랜잭션 밖에서도 쿼리 없이 사용 가능
        Map<Long, CartItem> owned = cartItemRepository.findAllByIdInAndCartId(cartItemIds, cart.getId())
                .stream()
                .collect(Collectors.toMap(CartItem::getId, Function.identity()));

        List<OrderResponse> orders = new ArrayList<>();
        List<FailedItemResponse> failedItems = new ArrayList<>();

        for (Long cartItemId : cartItemIds.stream().distinct().toList()) {
            CartItem item = owned.get(cartItemId);
            if (item == null) {
                log.warn("주문 실패 userId={}, cartItemId={}, reason=CART_ITEM_NOT_FOUND", userId, cartItemId);
                failedItems.add(FailedItemResponse.of(cartItemId, null, CartErrorCode.CART_ITEM_NOT_FOUND));
                continue;
            }

            Long groupBuyId = item.getGroupBuy().getId();
            try {
                orders.add(orderProcessor.process(userId, groupBuyId, item.getQuantity(), cartItemId));
            } catch (BusinessException e) {
                // 예상한 실패 (마감, 수량 초과, 결제 실패 등): 이유를 응답과 로그에 남김
                log.warn("주문 실패 userId={}, cartItemId={}, groupBuyId={}, reason={}",
                        userId, cartItemId, groupBuyId, e.getMessage());
                failedItems.add(FailedItemResponse.of(cartItemId, groupBuyId, e.getErrorCode()));
            } catch (RuntimeException e) {
                // 예상하지 못한 실패: 다른 항목은 계속 처리, 상세 내용은 로그로만
                log.error("주문 처리 중 예외 userId={}, cartItemId={}, groupBuyId={}",
                        userId, cartItemId, groupBuyId, e);
                failedItems.add(FailedItemResponse.unexpected(cartItemId, groupBuyId));
            }
        }

        return new OrderBundleResponse(orders, failedItems);
    }
}