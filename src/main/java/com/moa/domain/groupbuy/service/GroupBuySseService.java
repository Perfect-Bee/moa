package com.moa.domain.groupbuy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// 공동구매별 SSE 연결 관리 + 이벤트 전송
// 연결 목록을 서버 메모리에 보관 -> 서버 1대 기준 (여러 대면 Redis Pub/Sub 등 필요)
@Slf4j
@Service
public class GroupBuySseService {

    // 연결 유지 시간 30분. 만료되면 브라우저 EventSource가 자동 재연결
    private static final long TIMEOUT = 30 * 60 * 1000L;

    // key: groupBuyId, value: 그 공동구매를 구독 중인 연결들
    // ConcurrentHashMap: 여러 요청 스레드가 동시에 구독·해제
    // CopyOnWriteArrayList: 전송(순회) 중에 다른 스레드가 제거해도 예외 없음
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long groupBuyId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.computeIfAbsent(groupBuyId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        // 완료·만료·에러 시 목록에서 제거 (안 하면 끊긴 연결이 계속 쌓임)
        emitter.onCompletion(() -> remove(groupBuyId, emitter));
        emitter.onTimeout(() -> remove(groupBuyId, emitter));
        emitter.onError(e -> remove(groupBuyId, emitter));

        // 연결 직후 이벤트 1개 전송
        // 아무것도 안 보내면 만료 전까지 응답이 비어 있어 연결 여부를 확인할 수 없음
        send(groupBuyId, emitter, "connect", "connected groupBuyId=" + groupBuyId);

        log.info("SSE 구독 groupBuyId={}, 현재 연결 수={}", groupBuyId, emitters.get(groupBuyId).size());
        return emitter;
    }

    // 이 공동구매 구독자 전원에게 전송 (2단계에서 사용)
    public void sendToAll(Long groupBuyId, String eventName, Object data) {
        List<SseEmitter> list = emitters.get(groupBuyId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            send(groupBuyId, emitter, eventName, data);
        }
    }

    private void send(Long groupBuyId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException e) {
            // 클라이언트가 이미 끊김 -> 제거
            remove(groupBuyId, emitter);
        }
    }

    private void remove(Long groupBuyId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(groupBuyId);
        if (list != null) {
            list.remove(emitter);
        }
    }
}