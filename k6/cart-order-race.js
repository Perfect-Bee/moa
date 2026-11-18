import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const USERS = Number(__ENV.USERS || 2); // 동시에 결제할 사람 수
const MAX = Number(__ENV.MAX || 1);     // 공동구매 최대 수량

const orderSuccess = new Counter('order_success');   // 201
const orderConflict = new Counter('order_conflict'); // 409
const orderOther = new Counter('order_other');       // 그 외 (500 등)

export const options = {
  setupTimeout: '180s', // BCrypt 가입·로그인이 100명이면 시간이 걸림
  scenarios: {
    pay_at_once: {
      executor: 'per-vu-iterations', // VU N개가 동시에 시작해서 각 1번 실행
      vus: USERS,
      iterations: 1,
      maxDuration: '30s',
    },
  },
  thresholds: {
    order_success: [`count==${MAX}`], // 정확히 최대 수량만큼만 성공
    order_other: ['count==0'],        // 500 같은 예상 못 한 응답 없음
  },
};

function post(url, body, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  return http.post(`${BASE}${url}`, JSON.stringify(body), { headers });
}

function signupAndLogin(email, nickname, role) {
  const password = 'Password123!'; // 영문·숫자·특수문자 8~20자 규칙
  post('/api/v1/auth/signup', { email, password, nickname, role });
  return post('/api/v1/auth/login', { email, password }).json('data.accessToken');
}

export function setup() {
  const run = Date.now(); // 실행마다 이메일이 겹치지 않게

  const sellerToken = signupAndLogin(`seller${run}@test.com`, 'seller', 'SELLER');
  const productId = post('/api/v1/products',
      { name: `k6 상품 ${run}`, listPrice: 10000 }, sellerToken).json('data.productId');

  // startAt 생략 = 서버 현재 시각. endAt은 시간대 차이를 피하려고 넉넉히 하루 뒤
  const endAt = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().slice(0, 19);
  const groupBuyId = post(`/api/v1/products/${productId}/group-buys`,
      { wholesalePrice: 8000, targetQuantity: MAX, maxQuantity: MAX, endAt },
      sellerToken).json('data.groupBuyId');

  const buyers = [];
  for (let i = 0; i < USERS; i++) {
    const token = signupAndLogin(`buyer${i}-${run}@test.com`, `buyer${i}`, 'USER');
    // 담기는 1차 검증만 하므로 최대 수량보다 많은 사람이 담을 수 있음
    const cartItemId = post('/api/v1/carts/items',
        { groupBuyId, quantity: 1 }, token).json('data.cartItemId');
    buyers.push({ token, cartItemId });
  }
  return { groupBuyId, buyers };
}

export default function (data) {
  const me = data.buyers[__VU - 1]; // VU 번호는 1부터
  const res = post('/api/v1/orders', { cartItemIds: [me.cartItemId] }, me.token);

  if (res.status === 201) orderSuccess.add(1);
  else if (res.status === 409) orderConflict.add(1);
  else {
    orderOther.add(1);
    console.error(`예상 못 한 응답 ${res.status} ${res.body}`);
  }
  check(res, { '201 또는 409': (r) => r.status === 201 || r.status === 409 });
}

export function teardown(data) {
  const g = http.get(`${BASE}/api/v1/group-buys/${data.groupBuyId}`).json('data');
  console.log(`groupBuyId=${data.groupBuyId} current=${g.currentQuantity} max=${g.maxQuantity} status=${g.status}`);
  check(g, {
    '최대 수량 초과 없음': (x) => x.currentQuantity <= x.maxQuantity,
    '최대 수량까지 정확히 판매': (x) => x.currentQuantity === x.maxQuantity,
    'CLOSED 전환': (x) => x.status === 'CLOSED',
  });
}