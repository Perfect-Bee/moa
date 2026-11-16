// 상품, 공동 구매

document.getElementById('product-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const f = e.target;
    const res = await api('POST', '/api/v1/products', {
        name: field(f, 'name'),
        description: field(f, 'description'),
        listPrice: Number(field(f, 'listPrice')),
        imageUrl: field(f, 'imageUrl') || null,
    });
    if (res.ok) {
        f.reset();
        loadProducts();
    }
});

document.getElementById('groupbuy-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const f = e.target;
    const res = await api('POST', `/api/v1/products/${field(f, 'productId')}/group-buys`, {
        wholesalePrice: Number(field(f, 'wholesalePrice')),
        targetQuantity: Number(field(f, 'targetQuantity')),
        maxQuantity: Number(field(f, 'maxQuantity')),
        endAt: field(f, 'endAt'), // datetime-local 값 예: 2026-11-20T18:00 (초 생략 가능)
    });
    if (res.ok) loadProducts();
});

document.getElementById('load-products').addEventListener('click', loadProducts);

document.getElementById('progress-form').addEventListener('submit', (e) => {
    e.preventDefault();
    loadProgress(field(e.target, 'groupBuyId'));
});

// 상품 목록: 진행 중 공동구매가 있으면 수량 입력 + 현황·담기·바로 주문 버튼
async function loadProducts() {
    const res = await api('GET', '/api/v1/products?page=0&size=20');
    const tbody = document.getElementById('product-rows');
    tbody.innerHTML = '';
    if (!res.ok) return;

    res.body.data.content.forEach(p => {
        const g = p.groupBuy;
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${p.productId}</td>
            <td>${esc(p.name)}</td>
            <td>${won(p.listPrice)}</td>
            <td>${g ? `#${g.groupBuyId} · ${won(g.wholesalePrice)} · ${g.currentQuantity}/${g.targetQuantity}개 · ~${fmtTime(g.endAt)}` : '-'}</td>
            <td class="actions"></td>`;

        if (g) {
            const qty = document.createElement('input');
            qty.type = 'number';
            qty.min = 1;
            qty.value = 1;
            qty.className = 'qty';
            tr.querySelector('.actions').append(
                qty,
                button('현황', () => loadProgress(g.groupBuyId)),
                button('담기', () => addToCart(g.groupBuyId, Number(qty.value))),
                button('바로 주문', () => orderNow(g.groupBuyId, Number(qty.value))),
            );
        }
        tbody.append(tr);
    });
}

// 진행 현황 (비로그인도 가능한 API)
async function loadProgress(groupBuyId) {
    const res = await api('GET', `/api/v1/group-buys/${groupBuyId}`);
    const view = document.getElementById('progress-view');
    if (!res.ok) {
        view.textContent = res.body?.message ?? '조회 실패';
        return;
    }
    const g = res.body.data;
    document.getElementById('progress-form').elements.namedItem('groupBuyId').value = g.groupBuyId;
    view.innerHTML = `
        <p>#${g.groupBuyId} · 상품 ${g.productId} · <b>${esc(g.status)}</b> · 마감 ${fmtTime(g.endAt)}</p>
        <p>참여 ${g.currentQuantity} / 목표 ${g.targetQuantity} / 최대 ${g.maxQuantity} (남은 ${g.remainingQuantity})</p>
        <div class="bar"><div style="width:${Math.min(g.progressRate, 100)}%"></div></div>
        <p>목표 달성률 ${g.progressRate}%</p>`;
}