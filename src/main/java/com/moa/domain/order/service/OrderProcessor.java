package com.moa.domain.order.service;

import com.moa.common.exception.BusinessException;
import com.moa.domain.cart.repository.CartItemRepository;
import com.moa.domain.groupbuy.entity.GroupBuy;
import com.moa.domain.groupbuy.exception.GroupBuyErrorCode;
import com.moa.domain.groupbuy.repository.GroupBuyRepository;
import com.moa.domain.groupbuy.service.GroupBuyQuantityService;
import com.moa.domain.order.dto.OrderResponse;
import com.moa.domain.order.entity.Order;
import com.moa.domain.order.repository.OrderRepository;
import com.moa.domain.payment.client.MockPaymentClient;
import com.moa.domain.payment.entity.Payment;
import com.moa.domain.payment.repository.PaymentRepository;
import com.moa.domain.user.entity.User;
import com.moa.domain.user.exception.UserErrorCode;
import com.moa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

// 공동구매 1개 참여 = 주문 1건을 하나의 트랜잭션으로 처리
// OrderService와 다른 빈으로 분리한 이유: 같은 클래스 안에서 this.process()로 부르면
// 프록시를 거치지 않아 @Transactional이 적용되지 않음 (self-invocation)
// → 묶음 주문에서 항목마다 트랜잭션이 따로여야 한 항목이 실패해도 나머지가 유지됨
@Component
@RequiredArgsConstructor
public class OrderProcessor {

    private final UserRepository userRepository;
    private final GroupBuyRepository groupBuyRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CartItemRepository cartItemRepository;
    private final GroupBuyQuantityService groupBuyQuantityService;
    private final MockPaymentClient paymentClient;

    // cartItemId: 장바구니 주문이면 성공 시 그 항목 삭제, 단건 주문이면 null
    @Transactional
    public OrderResponse process(Long userId, Long groupBuyId, int quantity, Long cartItemId) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 회원·공동구매 존재(404) → 참여 가능 상태(409)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        GroupBuy groupBuy = groupBuyRepository.findById(groupBuyId)
                .orElseThrow(() -> new BusinessException(GroupBuyErrorCode.GROUP_BUY_NOT_FOUND));
        if (!groupBuy.isOrderable(now)) {
            throw new BusinessException(GroupBuyErrorCode.GROUP_BUY_NOT_OPEN);
        }

        // 2. Redis Lua 선점 (최대 수량 초과면 여기서 바로 409)
        groupBuyQuantityService.reserve(groupBuyId, quantity);

        // 3. 보상 처리 등록: 이후 어떤 이유로든 트랜잭션이 커밋되지 않으면
        //    선점 수량 반납 + (결제했다면) 결제 취소
        //    try-catch 대신 afterCompletion을 쓰는 이유: 커밋 단계에서 나는 실패까지 한 곳에서 처리
        AtomicReference<String> paymentKey = new AtomicReference<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    groupBuyQuantityService.release(groupBuyId, quantity);
                    if (paymentKey.get() != null) {
                        paymentClient.cancel(paymentKey.get());
                    }
                }
            }
        });

        // 4. 모의 결제 (선점 먼저, 결제 나중: 자리 없이 결제만 되는 상황 방지)
        long amount = (long) groupBuy.getWholesalePrice() * quantity;
        paymentKey.set(paymentClient.pay(userId, amount));

        // 5. DB 조건부 UPDATE (최종 방어선). 영향 행 0이면 실패 → 롤백 → 3번 보상 처리
        //    max에 도달하면 같은 UPDATE에서 CLOSED 전환
        int updated = groupBuyRepository.increaseQuantity(groupBuyId, quantity, now);
        if (updated == 0) {
            throw new BusinessException(GroupBuyErrorCode.EXCEEDS_REMAINING_QUANTITY);
        }

        // 6. 주문(PAID) + 결제 기록 저장
        Order order = orderRepository.save(Order.create(user, groupBuy, quantity, now));
        paymentRepository.save(Payment.complete(order, paymentKey.get(), amount, now));

        // 7. 장바구니 주문이면 해당 항목 삭제 (같은 트랜잭션 → 주문 실패 시 항목 유지)
        if (cartItemId != null) {
            cartItemRepository.deleteById(cartItemId);
        }

        // SSE 진행률 전송은 11/16 (커밋 후)
        return OrderResponse.from(order);
    }
}