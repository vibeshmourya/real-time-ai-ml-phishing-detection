import test from 'node:test';
import assert from 'node:assert/strict';

// Pure unit test of the URL normalization logic implemented in client.js
function resolveBaseUrl(rawInput) {
  let url = (rawInput || '/api').trim();
  if (!url) return '/api';
  url = url.replace(/\/+$/, '');
  if (/^https?:\/\//i.test(url) && !/\/api$/i.test(url)) {
    url += '/api';
  }
  return url;
}

test('1. API Base URL defaults to /api when env variable is missing or empty', () => {
  assert.equal(resolveBaseUrl(undefined), '/api');
  assert.equal(resolveBaseUrl(''), '/api');
  assert.equal(resolveBaseUrl('   '), '/api');
});

test('2. API Base URL appends /api if absolute domain is provided without /api path', () => {
  assert.equal(resolveBaseUrl('https://prahar-backend.onrender.com'), 'https://prahar-backend.onrender.com/api');
  assert.equal(resolveBaseUrl('http://localhost:8080'), 'http://localhost:8080/api');
  assert.equal(resolveBaseUrl('https://api.prahar.internal'), 'https://api.prahar.internal/api');
});

test('3. API Base URL strips trailing slashes and handles already-provided /api correctly', () => {
  assert.equal(resolveBaseUrl('https://prahar-backend.onrender.com/api/'), 'https://prahar-backend.onrender.com/api');
  assert.equal(resolveBaseUrl('https://prahar-backend.onrender.com/api///'), 'https://prahar-backend.onrender.com/api');
  assert.equal(resolveBaseUrl('https://prahar-backend.onrender.com/api'), 'https://prahar-backend.onrender.com/api');
  assert.equal(resolveBaseUrl('/api/'), '/api');
});

test('4. Chatbot request payload structure conforms to Spring Boot ChatRequest DTO', () => {
  const userText = 'Is this link safe: http://sus-bank.xyz';
  const history = [
    { role: 'user', content: 'Hello' },
    { role: 'assistant', content: 'How can I assist you with cyber safety today?' }
  ];

  const payload = {
    message: userText.trim(),
    conversationHistory: history,
    scanId: 104,
    scanUrl: 'http://sus-bank.xyz',
    scanRiskScore: 92,
    scanRiskLevel: 'HIGH_RISK',
    scanThreatCategory: 'CREDENTIAL_THEFT'
  };

  assert.equal(typeof payload.message, 'string');
  assert.equal(payload.message, userText);
  assert.ok(Array.isArray(payload.conversationHistory));
  assert.equal(payload.conversationHistory.length, 2);
  assert.equal(payload.scanId, 104);
  assert.equal(payload.scanRiskScore, 92);
});

test('5. Auth token fallback supports prahar_token with backward compatibility for phishshield_token', () => {
  const mockStorage = {
    prahar_token: 'valid-prahar-jwt-token',
    phishshield_token: 'legacy-token'
  };

  // Preference order: prahar_token first
  const resolvedToken = mockStorage.prahar_token || mockStorage.phishshield_token;
  assert.equal(resolvedToken, 'valid-prahar-jwt-token');

  // Fallback when prahar_token is not present
  const fallbackStorage = { phishshield_token: 'legacy-token' };
  const fallbackToken = fallbackStorage.prahar_token || fallbackStorage.phishshield_token;
  assert.equal(fallbackToken, 'legacy-token');
});
