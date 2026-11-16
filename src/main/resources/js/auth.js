// 회원가입, 로그인, 계정 전환

document.getElementById('signup-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const f = e.target;
    await api('POST', '/api/v1/auth/signup', {
        email: field(f, 'email'),
        password: field(f, 'password'),
        nickname: field(f, 'nickname'),
        role: field(f, 'role'),
    });
});

document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const f = e.target;
    const email = field(f, 'email');
    const res = await api('POST', '/api/v1/auth/login', {
        email,
        password: field(f, 'password'),
    });
    if (res.ok) {
        Session.save(email, res.body.data.accessToken);
        renderAccounts();
    }
});

document.getElementById('clear-accounts').addEventListener('click', () => {
    Session.clear();
    renderAccounts();
});

// 상단 현재 계정 + 계정 목록(누르면 전환)
function renderAccounts() {
    const active = Session.active();
    document.getElementById('current-user').textContent = active
        ? `${active.email} · ${active.role} · userId ${active.userId}`
        : '로그인 안 됨';

    const list = document.getElementById('account-list');
    list.innerHTML = '';
    Object.entries(Session.accounts()).forEach(([email, account]) => {
        const li = document.createElement('li');
        const mark = active && active.email === email ? ' 사용 중' : '';
        li.append(button(`${email} (${account.role})${mark}`, () => {
            Session.switchTo(email);
            renderAccounts();
        }));
        list.append(li);
    });
}