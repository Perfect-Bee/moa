// 공통 도우미 + 탭 전환 + 첫 화면 (다른 js보다 마지막에 로드)

// 사용자 입력값을 HTML에 넣을 때 태그로 해석되지 않게 (XSS 방지)
function esc(value) {
    return String(value ?? '').replace(/[&<>"']/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
    }[c]));
}

function won(n) {
    return Number(n).toLocaleString('ko-KR') + '원';
}

// 소수점 이하, 분만 남김
// 예) "2026-11-16T14:41:07.99" -> "2026-11-16 14:41"
function fmtTime(iso) {
    return iso ? iso.replace('T', ' ').slice(0, 16) : '-';
}

// 폼 입력값 (form.name은 폼 자신의 name 속성이라 elements로 꺼냄)
function field(form, name) {
    return form.elements.namedItem(name).value;
}

function button(label, onClick, className = 'small') {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.textContent = label;
    btn.className = className;
    btn.addEventListener('click', onClick);
    return btn;
}

// 탭 전환
document.querySelectorAll('#tabs button').forEach(tab => {
    tab.addEventListener('click', () => {
        document.querySelectorAll('#tabs button').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.tab').forEach(s => s.classList.remove('active'));
        tab.classList.add('active');
        document.getElementById('tab-' + tab.dataset.tab).classList.add('active');
    });
});

document.getElementById('clear-log').addEventListener('click', () => {
    document.getElementById('log').innerHTML = '';
});

// 첫 화면
renderAccounts();
loadProducts();