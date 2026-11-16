// 주문, 목록, 상세 조회, 취소
let orderPage = 0;
let orderLastPage = 0;

async function orderNow(groupBuyId, quantity) {
    const res = await api('POST', '/api/v1/orders', { groupBuyId, quantity });
    if (res.ok) {
        loadProducts();           // 참여 수량 갱신
        loadProgress(groupBuyId); // CLOSED 전환 여부 확인
    }
}

document.getElementById('load-orders').addEventListener('click', () => loadOrders(0));
document.getElementById('prev-page').addEventListener('click', () => loadOrders(Math.max(orderPage - 1, 0)));
document.getElementById('next-page').addEventListener('click', () => loadOrders(Math.min(orderPage + 1, orderLastPage)));

async function loadOrders(page) {
    const res = await api('GET', `/api/v1/orders?page=${page}&size=10`);
    const tbody = document.getElementById('order-rows');
    tbody.innerHTML = '';
    if (!res.ok) return;

    const data = res.body.data;
    orderPage = data.page;
    orderLastPage = Math.max(Math.ceil(data.totalElements / data.size) - 1, 0);
    document.getElementById('page-info').textContent =
        `${data.page + 1} / ${orderLastPage + 1} 페이지 (총 ${data.totalElements}건)`;

    data.content.forEach(o => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${o.orderId}</td>
            <td>${esc(o.orderNumber)}</td>
            <td><span class="badge ${esc(o.status)}">${esc(o.status)}</span></td>
            <td>${won(o.totalPrice)}</td>
            <td>${fmtTime(o.createdAt)}</td>
            <td class="actions"></td>`;
        // 취소 버튼은 상태와 상관없이 표시 -> 이미 취소된 주문으로 409 확인 가능
        tr.querySelector('.actions').append(
            button('상세', () => showOrder(o.orderId)),
            button('취소', () => cancelOrder(o.orderId), 'small secondary'),
        );
        tbody.append(tr);
    });
}

document.getElementById('order-detail-form').addEventListener('submit', (e) => {
    e.preventDefault();
    showOrder(field(e.target, 'orderId'));
});

async function showOrder(orderId) {
    const res = await api('GET', `/api/v1/orders/${orderId}`);
    const view = document.getElementById('order-detail');
    document.getElementById('order-detail-form').elements.namedItem('orderId').value = orderId;
    if (!res.ok) {
        view.textContent = res.body?.message ?? '조회 실패';
        return;
    }
    const o = res.body.data;
    view.innerHTML = `
        <p><b>#${o.orderId}</b> ${esc(o.orderNumber)} · <span class="badge ${esc(o.status)}">${esc(o.status)}</span>
           · ${won(o.totalPrice)} · ${fmtTime(o.createdAt)}</p>
        <p>공동구매 #${o.groupBuy.groupBuyId} ${esc(o.groupBuy.productName)}
           · ${o.groupBuy.quantity}개 × ${won(o.groupBuy.unitPrice)} · 공동구매 상태 <b>${esc(o.groupBuy.groupBuyStatus)}</b></p>
        <p>결제 ${won(o.payment.amount)} · <b>${esc(o.payment.status)}</b>
           · 결제 ${fmtTime(o.payment.paidAt)} · 환불 ${fmtTime(o.payment.refundedAt)}</p>`;
    view.append(button('이 주문 취소', () => cancelOrder(o.orderId), 'small secondary'));
}

// 취소: DELETE가 아닌 POST (상태 변경 + 환불·수량 복구)
async function cancelOrder(orderId) {
    const res = await api('POST', `/api/v1/orders/${orderId}/cancel`);
    if (res.ok) {
        loadOrders(orderPage);
        showOrder(orderId);
    }
}