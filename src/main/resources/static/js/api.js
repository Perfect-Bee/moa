// API 호출 공통 처리
// - 로그인한 계정의 토큰을 이메일별로 localStorage에 보관 -> 계정 전환으로 403 테스트
// - 요청마다 Authorization 헤더 자동 첨부
// - 모든 요청·응답을 오른쪽 로그 패널에 기록 (Postman 결과 화면 대신)

const ACCOUNTS_KEY = 'moa.accounts'; // { email: { token, role, userId } }
const ACTIVE_KEY = 'moa.active';     // 지금 사용하는 계정 이메일

const Session = {
    accounts() {
        return JSON.parse(localStorage.getItem(ACCOUNTS_KEY) || '{}');
    },
    active() {
        const email = localStorage.getItem(ACTIVE_KEY);
        const account = this.accounts()[email];
        return account ? { email, ...account } : null;
    },
    save(email, token) {
        const payload = decodeJwt(token);
        const accounts = this.accounts();
        accounts[email] = { token, role: payload.role, userId: payload.sub };
        localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(accounts));
        localStorage.setItem(ACTIVE_KEY, email);
    },
    switchTo(email) {
        localStorage.setItem(ACTIVE_KEY, email);
    },
    clear() {
        localStorage.removeItem(ACCOUNTS_KEY);
        localStorage.removeItem(ACTIVE_KEY);
    },
};

// JWT 가운데(payload)만 읽어서 role·userId(sub) 표시용으로 사용
// 서명 검증은 서버(JwtFilter)가 하므로 여기서는 하지 않음
function decodeJwt(token) {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64));
}

// fetch 공통: { ok, status, body } 반환 (body = 서버의 BaseResponse)
async function api(method, url, body) {
    const account = Session.active();
    const headers = {};
    if (account) headers['Authorization'] = 'Bearer ' + account.token;
    if (body !== undefined) headers['Content-Type'] = 'application/json';

    let status = 0;
    let data = null;
    try {
        const res = await fetch(url, {
            method,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body),
        });
        status = res.status;
        const text = await res.text(); // 본문이 없는 응답도 있어서 text로 먼저 읽음
        try {
            data = text ? JSON.parse(text) : null;
        } catch {
            data = text;
        }
    } catch (e) {
        data = { success: false, message: '서버에 연결할 수 없습니다: ' + e.message };
    }

    writeLog(method, url, body, status, data, account);
    return { ok: status >= 200 && status < 300, status, body: data };
}

// 로그 패널에 최신 요청을 위에 추가 (textContent라 응답 내용이 태그로 해석되지 않음)
function writeLog(method, url, body, status, data, account) {
    const entry = document.createElement('div');
    entry.className = 'log-entry ' + (status >= 200 && status < 300 ? 'ok' : 'fail');
    const who = account ? `${account.email} (${account.role})` : '비로그인';
    entry.textContent =
        `[${new Date().toLocaleTimeString()}] ${method} ${url} → ${status}\n` +
        `계정: ${who}\n` +
        (body !== undefined ? '요청: ' + JSON.stringify(body) + '\n' : '') +
        '응답: ' + JSON.stringify(data, null, 2);
    document.getElementById('log').prepend(entry);
}