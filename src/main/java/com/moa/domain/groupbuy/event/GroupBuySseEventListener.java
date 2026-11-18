package com.moa.domain.groupbuy.event;

import com.moa.domain.groupbuy.dto.GroupBuyProgressResponse;
import com.moa.domain.groupbuy.enums.GroupBuyStatus;
import com.moa.domain.groupbuy.service.GroupBuyService;
import com.moa.domain.groupbuy.service.GroupBuySseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupBuySseEventListener {

    private final GroupBuyService groupBuyService;
    private final GroupBuySseService groupBuySseService;

    // 커밋 성공 후에만 실행 (롤백되면 호출되지 않음)
    // REQUIRES_NEW: 원래 트랜잭션의 영속성 컨텍스트(주문 전 수량이 캐시됨) 대신 새로 DB 조회
    @Async // 비동기 실행
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void handle(GroupBuyChangedEvent event) {
        try {
            GroupBuyProgressResponse progress = groupBuyService.getProgress(event.groupBuyId());

            // 성사·실패면 finished, 그 외(OPEN·CLOSED)는 progress
            boolean finished = progress.status() == GroupBuyStatus.SUCCEEDED
                    || progress.status() == GroupBuyStatus.FAILED;
            groupBuySseService.sendToAll(event.groupBuyId(), finished ? "finished" : "progress", progress);
        } catch (RuntimeException e) {
            // 이미 커밋된 주문·취소·마감에 영향 주지 않도록 로그만 남김
            log.error("SSE 전송 실패 groupBuyId={}", event.groupBuyId(), e);
        }
    }
}