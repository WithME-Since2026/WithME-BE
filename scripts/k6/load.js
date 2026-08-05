// 읽기 전용 부하 테스트. 로그인 1회 후 카테고리 목록 조회를 반복한다.
//
//   k6 run -e BASE_URL=http://localhost:8080 -e LOGIN_ID=tester -e PASSWORD=... scripts/k6/load.js
//   k6 run -e ... -e VUS=20 -e DURATION=1m scripts/k6/load.js
//
// 주의
// - 서버가 EC2 한 대이므로 부하 테스트 자체가 장애다. 운영에 걸려면 시간대를 잡을 것.
// - 로그인은 setup() 에서 1회만 한다. VU 마다 로그인하면 BCrypt 해싱 속도를 재게 된다.
// - 조회만 한다. 쓰기를 넣으면 대상 DB 에 쓰레기 데이터가 쌓인다.
import http from 'k6/http';
import { check, sleep, fail } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: Number(__ENV.VUS || 5),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],      // 에러율 1% 미만
    http_req_duration: ['p(95)<500'],    // 95%가 500ms 이내
  },
};

export function setup() {
  const response = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ localId: __ENV.LOGIN_ID, password: __ENV.PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  if (response.status !== 200) {
    fail(`로그인 실패(${response.status}). LOGIN_ID/PASSWORD 와 BASE_URL 을 확인하라: ${response.body}`);
  }
  return { token: response.json('data.accessToken') };
}

export default function (data) {
  const response = http.get(`${BASE_URL}/api/v1/todo/category`, {
    headers: { Authorization: `Bearer ${data.token}` },
  });

  check(response, { '200 응답': (r) => r.status === 200 });

  sleep(1);
}
