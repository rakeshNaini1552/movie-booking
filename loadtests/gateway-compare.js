// Sends the same request through the Spring gateway and through Kong and tags every request with its gateway.
// KONG_PERCENT is the share of requests sent to Kong (0, 10, 50 or 100); the rest go to the Spring gateway.
//
//   read -s "TEST_PASSWORD?password: "; export TEST_PASSWORD
//   TEST_USER=<local user> KONG_PERCENT=50 RATE=50 DURATION=60s k6 run loadtests/gateway-compare.js
//
// Needs two port-forwards: Spring gateway on 8090 and Kong on 8092 (see SPRING_URL and KONG_URL).
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const SPRING_URL = __ENV.SPRING_URL || 'http://localhost:8090';
const KONG_URL = __ENV.KONG_URL || 'http://localhost:8092';
const KONG_PERCENT = Number(__ENV.KONG_PERCENT || 50);
const RATE = Number(__ENV.RATE || 50);
const DURATION = __ENV.DURATION || '60s';
const PRE_VUS = Number(__ENV.PRE_VUS || 20);
const MAX_VUS = Number(__ENV.MAX_VUS || 100);

const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8180';
const REALM = __ENV.REALM || 'movie-booking';
const CLIENT_ID = __ENV.CLIENT_ID || 'movie-booking-app';
// The pods and Kong expect the issuer host.docker.internal, which Keycloak takes from the Host header.
const TOKEN_HOST = __ENV.TOKEN_HOST || 'host.docker.internal:8180';
const USERNAME = __ENV.TEST_USER;
const PASSWORD = __ENV.TEST_PASSWORD;

// 404 is the normal answer (the booking does not exist) and 429 is the rate limiter; anything else counts as failed.
http.setResponseCallback(http.expectedStatuses(404, 429));

const rateLimited = new Counter('rate_limited');

export const options = {
  scenarios: {
    compare: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: PRE_VUS,
      maxVUs: MAX_VUS,
    },
  },
  thresholds: {
    // Always true; they only make k6 print a separate line per gateway.
    'http_req_duration{gateway:spring}': ['max>=0'],
    'http_req_duration{gateway:kong}': ['max>=0'],
    'http_req_failed{gateway:spring}': ['rate>=0'],
    'http_req_failed{gateway:kong}': ['rate>=0'],
  },
};

// Logs in once before the load starts, so a wrong user or password stops the run here instead of in every iteration.
export function setup() {
  if (!USERNAME || !PASSWORD) {
    throw new Error('Set TEST_USER and TEST_PASSWORD in the environment');
  }
  requestToken();
  return { token, tokenExpiresAt };
}

let token = null;
let tokenExpiresAt = 0;

function requestToken() {
  const res = http.post(
    `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`,
    { grant_type: 'password', client_id: CLIENT_ID, username: USERNAME, password: PASSWORD },
    {
      headers: { Host: TOKEN_HOST },
      tags: { name: 'token' },
      responseCallback: http.expectedStatuses(200),
    },
  );
  if (res.status !== 200) {
    // Keycloak's error body names the problem (for example invalid_grant) and holds no secret.
    throw new Error(`Token request failed with status ${res.status}: ${res.body}`);
  }
  token = res.json('access_token');
  tokenExpiresAt = Date.now() + Number(res.json('expires_in')) * 1000;
}

// Every VU starts from the token that setup() fetched and logs in again only when it is about to expire.
function getToken(shared) {
  if (!token) {
    token = shared.token;
    tokenExpiresAt = shared.tokenExpiresAt;
  }
  if (Date.now() >= tokenExpiresAt - 30000) {
    requestToken();
  }
  return token;
}

function randomUuid() {
  const hex = (n) => Array.from({ length: n }, () => Math.floor(Math.random() * 16).toString(16)).join('');
  return `${hex(8)}-${hex(4)}-4${hex(3)}-a${hex(3)}-${hex(12)}`;
}

export default function (shared) {
  const useKong = Math.random() * 100 < KONG_PERCENT;
  const gateway = useKong ? 'kong' : 'spring';
  const base = useKong ? KONG_URL : SPRING_URL;

  const res = http.get(`${base}/api/v1/bookings/${randomUuid()}`, {
    headers: { Authorization: `Bearer ${getToken(shared)}` },
    tags: { gateway },
  });

  check(res, { 'status is 404 or 429': (r) => r.status === 404 || r.status === 429 }, { gateway });
  if (res.status === 429) {
    rateLimited.add(1, { gateway });
  }
}
