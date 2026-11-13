package com.moa.domain.payment.client;

import com.moa.common.exception.BusinessException;
import com.moa.domain.order.exception.OrderErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

// 모의 결제 (실제 PG 연동 없음. 테스트는 가상의 카카오로 결제 예정)
// payment.mock.fail=true 로 실행하면 항상 실패 -> 결제 실패 보상 처리 확인용
@Slf4j
@Component
public class MockPaymentClient {

    @Value("${payment.mock.fail:false}")
    private boolean alwaysFail;

    // 결제 승인 → 결제 키 반환
    public String pay(Long userId, long amount) {
        if (alwaysFail) {
            log.warn("모의 결제 실패 userId={}, amount={}", userId, amount);
            throw new BusinessException(OrderErrorCode.PAYMENT_FAILED);
        }
        String paymentKey = "MOCK-" + UUID.randomUUID();
        log.info("모의 결제 승인 userId={}, amount={}, paymentKey={}", userId, amount, paymentKey);
        return paymentKey;
    }

    // 결제 취소 (보상 처리, 환불)
    public void cancel(String paymentKey) {
        log.info("모의 결제 취소 paymentKey={}", paymentKey);
    }
}