// 장바구니 조회, 체크한 항목 묶음 주문, 담기, 수량 변경, 삭제

document.getElementById('load-cart').addEventListener('click', loadCart);

async function addToCart(groupBuyId, quantity) {
    const res = await api('POST', '/api/v1/carts/items', { groupBuyId, quantity });
    if (res.ok) loadCart();
}

async function loadCart() {
    const res = await api('GET', '/api/v1/carts');
    const tbody = document.getElementById('cart-rows');
    tbody.innerHTML = '';
    if (!res.ok) return;

    const cart = res.body.data;
    cart.items.forEach(item => {
        const tr = document.createElement('tr');
        // 주문 가능한 항목만 기본 체크 (불가 항목도 직접 체크해서 실패 응답 확인 가능)
        tr.innerHTML = `
            <td><input type="checkbox" class="cart-check" value="${item.cartItemId}" ${item.orderable ? 'checked' : ''}></td>
            <td>${item.cartItemId}</td>
            <td>${esc(item.productName)} (#${item.groupBuyId})</td>
            <td>${won(item.wholesalePrice)}</td>
            <td class="qty-cell"></td>
            <td>${item.remainingQuantity}</td>
            <td>${item.orderable ? 'O' : 'X'}</td>
            <td class="actions"></td>`;

        const qty = document.createElement('input');
        qty.type = 'number';
        qty.min = 1;
        qty.value = item.quantity;
        qty.className = 'qty';
        tr.querySelector('.qty-cell').append(qty);
        tr.querySelector('.actions').append(
            button('수량 변경', () => updateCartItem(item.cartItemId, Number(qty.value))),
            button('삭제', () => deleteCartItem(item.cartItemId), 'small secondary'),
        );
        tbody.append(tr);
    });
    document.getElementById('cart-total').textContent = won(cart.totalPrice);
}

async function updateCartItem(cartItemId, quantity) {
    const res = await api('PUT', `/api/v1/carts/items/${cartItemId}`, { quantity });
    if (res.ok) loadCart();
}

async function deleteCartItem(cartItemId) {
    const res = await api('DELETE', `/api/v1/carts/items/${cartItemId}`);
    if (res.ok) loadCart();
}

// 묶음 주문: 항목별 독립 처리 -> 실패한 항목만 실패 (201: 하나라도 성공, 409: 전부 실패)
document.getElementById('order-selected').addEventListener('click', async () => {
    const cartItemIds = [...document.querySelectorAll('.cart-check:checked')].map(c => Number(c.value));
    if (cartItemIds.length === 0) return;

    const res = await api('POST', '/api/v1/orders', { cartItemIds });
    const data = res.body?.data;
    document.getElementById('bundle-result').textContent = data
        ? `성공 ${data.orders.length}건 / 실패 ${data.failedItems.length}건\n` +
          data.failedItems.map(f => `- 항목 ${f.cartItemId}: ${f.code} ${f.message}`).join('\n')
        : (res.body?.message ?? '');
    loadCart();
});