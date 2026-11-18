package com.moa.domain.order.service;

import com.moa.common.dto.response.PageResponse;
import com.moa.common.exception.BusinessException;
import com.moa.domain.cart.entity.Cart;
import com.moa.domain.cart.entity.CartItem;
import com.moa.domain.cart.exception.CartErrorCode;
import com.moa.domain.cart.repository.CartItemRepository;
import com.moa.domain.cart.repository.CartRepository;
import com.moa.domain.groupbuy.event.GroupBuyChangedEvent;
import com.moa.domain.groupbuy.exception.GroupBuyErrorCode;
import com.moa.domain.groupbuy.repository.GroupBuyRepository;
import com.moa.domain.groupbuy.service.GroupBuyQuantityService;
import com.moa.domain.order.dto.*;
import com.moa.domain.order.entity.Order;
import com.moa.domain.order.exception.OrderErrorCode;
import com.moa.domain.order.repository.OrderRepository;
import com.moa.domain.payment.client.MockPaymentClient;
import com.moa.domain.payment.entity.Payment;
import com.moa.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
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

    // 조회·취소용 의존성 추가
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyQuantityService groupBuyQuantityService;
    private final MockPaymentClient paymentClient;

    // 공동구매 변경 이벤트 발행 (커밋 후 리스너가 SSE로 전송)
    private final ApplicationEventPublisher eventPublisher;

    private static final int MAX_PAGE_SIZE = 100;

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

    // 내 주문 목록 (최신순)
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getOrders(Long userId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(OrderErrorCode.INVALID_PAGE_REQUEST);
        }
        Page<Order> orders = orderRepository.findPageByUserId(userId, PageRequest.of(page, size));
        return PageResponse.from(orders.map(OrderSummaryResponse::from));
    }

    // 주문 상세 (사용자 한정)
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
        if (!order.isOwnedBy(userId)) {
            throw new BusinessException(OrderErrorCode.NOT_ORDER_OWNER);
        }
        return OrderDetailResponse.of(order, findPayment(orderId));
    }

    // 주문 취소 : 주문 PAID + 공동구매 OPEN·마감 전일 때만
    @Transactional
    public OrderCancelResponse cancel(Long userId, Long orderId) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 주문 행 잠금
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        // 2. 본인 주문
        if (!order.isOwnedBy(userId)) {
            throw new BusinessException(OrderErrorCode.NOT_ORDER_OWNER);
        }

        // 3. PAID -> CANCELED (아니면 409)
        order.cancel();

        // 4. 공동구매 수량 감소. 영향 행 0 -> 롤백 + 409
        Long groupBuyId = order.getGroupBuy().getId();
        int quantity = order.getQuantity();
        if (groupBuyRepository.decreaseQuantity(groupBuyId, quantity, now) == 0) {
            throw new BusinessException(GroupBuyErrorCode.GROUP_BUY_CLOSED);
        }

        // 5. 환불
        Payment payment = findPayment(orderId);
        payment.refund(now);
        paymentClient.cancel(payment.getPaymentKey());

        // 6. 커밋 후 Redis 반납
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    groupBuyQuantityService.release(groupBuyId, quantity);
                } catch (RuntimeException e) {
                    log.error("취소 후 Redis 반납 실패 groupBuyId={}, quantity={}", groupBuyId, quantity, e);
                }
            }
        });

        // 커밋 후 SSE 진행률 전송
        eventPublisher.publishEvent(new GroupBuyChangedEvent(groupBuyId));

        log.info("주문 취소 userId={}, orderId={}, groupBuyId={}, quantity={}", userId, orderId, groupBuyId, quantity);
        return OrderCancelResponse.of(order, payment);
    }

    // 주문이 있으면 결제도 반드시 있음 -> 없으면 데이터 이상이라 500
    private Payment findPayment(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("결제 기록이 없는 주문 orderId=" + orderId));
    }
}