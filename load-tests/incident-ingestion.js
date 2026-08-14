import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const SERVICES = ['payments-api', 'identity-service', 'recommendation-api'];
const ALERT_TYPES = ['HIGH_ERROR_RATE', 'LATENCY_SPIKE', 'AUTH_FAILURE', 'TIMEOUT'];
const SEVERITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const SOURCES = Array.from({ length: 15 }, (_, i) => `monitoring-service-${i}`);

const CORRELATION_POOL = Array.from({ length: 15 }, (_, i) => ({
  service: SERVICES[i % SERVICES.length],
  alertType: ALERT_TYPES[i % ALERT_TYPES.length],
  resourceId: `resource-pool-${i}`,
}));

const DUPLICATE_ID_POOL = Array.from({ length: 25 }, (_, i) => `k6-dup-seed-${i}`);

const alertsAccepted = new Counter('alerts_accepted');
const alertsRejected = new Counter('alerts_rejected');
const alertErrorRate = new Rate('alert_error_rate');

export const options = {
  scenarios: {
    alert_ingestion: {
      executor: 'constant-arrival-rate',
      rate: 8,
      timeUnit: '1s',
      duration: '2m',
      preAllocatedVUs: 20,
      maxVUs: 50,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    alert_error_rate: ['rate<0.01'],
  },
};

function buildPayload(service, alertType, resourceId, externalEventId) {
  return {
    externalEventId,
    source: SOURCES[Math.floor(Math.random() * SOURCES.length)],
    service,
    alertType,
    resourceId,
    severity: SEVERITIES[Math.floor(Math.random() * SEVERITIES.length)],
    summary: `k6 load test alert for ${service}`,
    occurredAt: new Date().toISOString(),
    metadata: { loadTest: true },
    failureSimulation: 'NONE',
  };
}

export default function () {
  const roll = Math.random();
  let payload;

  if (roll < 0.70) {
    const pick = CORRELATION_POOL[Math.floor(Math.random() * CORRELATION_POOL.length)];
    payload = buildPayload(
      pick.service,
      pick.alertType,
      pick.resourceId,
      `k6-corr-${__VU}-${__ITER}-${Date.now()}`
    );
  } else if (roll < 0.95) {
    const service = SERVICES[Math.floor(Math.random() * SERVICES.length)];
    const alertType = ALERT_TYPES[Math.floor(Math.random() * ALERT_TYPES.length)];
    payload = buildPayload(
      service,
      alertType,
      `resource-unique-${__VU}-${__ITER}-${Date.now()}`,
      `k6-new-${__VU}-${__ITER}-${Date.now()}`
    );
  } else {
    const pick = CORRELATION_POOL[Math.floor(Math.random() * CORRELATION_POOL.length)];
    const dupId = DUPLICATE_ID_POOL[Math.floor(Math.random() * DUPLICATE_ID_POOL.length)];
    payload = buildPayload(pick.service, pick.alertType, pick.resourceId, dupId);
  }

  const res = http.post(`${BASE_URL}/api/v1/alerts`, JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
  });

  const ok = check(res, {
    'status is 202': (r) => r.status === 202,
    'body has alert id': (r) => JSON.parse(r.body).id !== undefined,
  });

  alertsAccepted.add(ok ? 1 : 0);
  alertsRejected.add(ok ? 0 : 1);
  alertErrorRate.add(!ok);
}
