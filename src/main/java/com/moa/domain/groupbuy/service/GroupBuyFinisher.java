package com.moa.domain.groupbuy.service;

import com.moa.domain.groupbuy.entity.GroupBuy;
import com.moa.domain.groupbuy.enums.GroupBuyStatus;
import com.moa.domain.groupbuy.event.GroupBuyChangedEvent;
import com.moa.domain.groupbuy.repository.GroupBuyRepository;
import com.moa.domain.order.entity.Order;
import com.moa.domain.order.repository.OrderRepository;
import com.moa.domain.payment.client.MockPaymentClient;
import com.moa.domain.payment.entity.Payment;
import com.moa.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// 공동구매 1건 마감 = 판정 + 주문·결제 처리를 하나의 트랜잭션으로
// 스케줄러와 다른 빈으로 분리한 이유: 같은 클래스 안에서 호출하면 @Transactional이 적용되지 않음 (self-invocation)
// -> 건마다 트랜잭션이 따로여야 한 건이 실패해도 다른 공동구매 처리는 유지됨
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupBuyFinisher {

    private final GroupBuyRepository groupBuyRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final MockPaymentClient paymentClient;

    // 공동구매 변경 이벤트 발행 (커밋 후 리스너가 SSE로 전송)
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void finish(Long groupBuyId, LocalDateTime now) {
        // 1. 조건부 UPDATE로 판정. 영향 행 0 = 이미 처리됨 -> 건너뜀
        if (groupBuyRepository.finish(groupBuyId, now) == 0) {
            return;
        }

        // 2. 판정 결과 조회
        // 이 트랜잭션에서 GroupBuy를 미리 읽지 않았으므로 UPDATE 이후 값이 그대로 조회됨
        GroupBuy groupBuy = groupBuyRepository.findById(groupBuyId)
                .orElseThrow(() -> new IllegalStateException("판정한 공동구매가 없음 groupBuyId=" + groupBuyId));
        boolean succeeded = groupBuy.getStatus() == GroupBuyStatus.SUCCEEDED;

        // 3. 참여 주문 처리: 성사 -> 구매 확정 / 실패 -> 주문·결제 환불
        List<Order> orders = orderRepository.findPaidByGroupBuyId(groupBuyId);
        for (Order order : orders) {
            if (succeeded) {
                order.confirm();
            } else {
                order.refund();
                Payment payment = paymentRepository.findByOrderId(order.getId())
                        .orElseThrow(() -> new IllegalStateException("결제 없는 주문 orderId=" + order.getId()));
                payment.refund(now);   // 이미 환불이면 그대로 (멱등)
                paymentClient.cancel(payment.getPaymentKey());
            }
        }

        // 커밋 후 SSE 성사/실패 전송
        eventPublisher.publishEvent(new GroupBuyChangedEvent(groupBuyId));
        log.info("공동구매 마감 groupBuyId={}, result={}, orders={}",
                groupBuyId, groupBuy.getStatus(), orders.size());
    }
}