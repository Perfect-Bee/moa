package com.moa.domain.groupbuy.scheduler;

import com.moa.domain.groupbuy.repository.GroupBuyRepository;
import com.moa.domain.groupbuy.service.GroupBuyFinisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// 마감 시각이 지난 공동구매를 주기적으로 성사/실패 판정
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupBuyScheduler {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyFinisher groupBuyFinisher;

    // fixedDelay: 이전 실행이 끝난 뒤부터 시간을 잼 -> 실행이 겹치지 않음
    // 테스트용 10초(10_000). 시연 후 1분 정도로 늘리기
    @Scheduled(fixedDelay = 10_000)
    public void finishExpiredGroupBuys() {
        // 한 번 실행 안에서는 같은 기준 시각 사용
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = groupBuyRepository.findIdsToFinish(now);

        for (Long id : ids) {
            try {
                groupBuyFinisher.finish(id, now);
            } catch (RuntimeException e) {
                // 한 건 실패해도 나머지는 계속. 실패한 건은 상태가 그대로라 다음 실행에서 다시 시도됨
                log.error("공동구매 마감 실패 groupBuyId={}", id, e);
            }
        }
    }
}