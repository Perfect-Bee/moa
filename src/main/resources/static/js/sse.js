// 공동구매 실시간 진행률 (SSE)
// - EventSource는 헤더를 못 넣음 -> 실시간 보기 API는 비로그인 허용 (SecurityConfig)
// - 처음 값은 조회 API로, 이후 변화는 SSE 이벤트로 받음

let eventSource = null; // 한 번에 공동구매 1개만 실시간 보기

// 상태값 -> 화면 표시 문구
const STATUS_LABEL = {
    OPEN: '진행 중',
    CLOSED: '조기 마감 (최대 수량 도달)',
    SUCCEEDED: '성사',
    FAILED: '실패 (전원 환불)',
};

// 연결 상태 문구 표시
function setSseState(text) {
    document.getElementById('sse-state').textContent = text;
}

// 조회 API 응답(data)과 SSE 이벤트 data가 같은 형식(GroupBuyProgressResponse)
function renderSseProgress(g) {
    document.getElementById('sse-view').innerHTML = `
        <p>#${g.groupBuyId} · <b>${esc(STATUS_LABEL[g.status] ?? g.status)}</b> · 마감 ${fmtTime(g.endAt)}</p>
        <p>참여 ${g.currentQuantity} / 목표 ${g.targetQuantity} / 최대 ${g.maxQuantity} (남은 ${g.remainingQuantity})</p>
        <div class="bar"><div style="width:${Math.min(g.progressRate, 100)}%"></div></div>
        <p>목표 달성률 ${g.progressRate}% · 갱신 ${new Date().toLocaleTimeString()}</p>`;
}

// 실시간 보기
async function subscribeGroupBuy(groupBuyId) {
    closeSse(); // 이전 연결 정리

    // 1) 현재 값 먼저 (SSE는 연결 전의 변화를 다시 보내주지 않음)
    const res = await api('GET', `/api/v1/group-buys/${groupBuyId}`);
    if (!res.ok) {
        setSseState(res.body?.message ?? '조회 실패');
        return;
    }
    renderSseProgress(res.body.data);

    // 2) 이후 변화는 SSE로
    const url = `/api/v1/group-buys/${groupBuyId}/subscribe`;
    eventSource = new EventSource(url);

    eventSource.addEventListener('connect', e => {
        setSseState(`실시간 보기 (공동구매 #${groupBuyId})`);
        writeLog('SSE', url, undefined, 200, { event: 'connect', data: e.data }, null);
    });

    eventSource.addEventListener('progress', e => {
        const g = JSON.parse(e.data);
        renderSseProgress(g);
        writeLog('SSE', url, undefined, 200, { event: 'progress', data: g }, null);
    });

    eventSource.addEventListener('finished', e => {
        const g = JSON.parse(e.data);
        renderSseProgress(g);
        writeLog('SSE', url, undefined, 200, { event: 'finished', data: g }, null);
        closeSse('판정 완료 · 실시간 보기 종료');
    });

    // 끊기면 EventSource가 자동 재연결을 시도함
    eventSource.onerror = () => setSseState('연결 끊김 · 자동 재연결 시도 중');
}

// 실시간 보기 중지와 이벤트 연결
function closeSse(message = '연결 안 됨') {
    if (eventSource) {
        eventSource.close(); // 자동 재연결도 멈춤
        eventSource = null;
    }
    setSseState(message);
}

document.getElementById('sse-form').addEventListener('submit', e => {
    e.preventDefault();
    subscribeGroupBuy(field(e.target, 'groupBuyId'));
});

document.getElementById('sse-close').addEventListener('click', () => closeSse());