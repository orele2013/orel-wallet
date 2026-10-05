import test from 'node:test';
import assert from 'node:assert/strict';

import * as serverModule from './server.mjs';
test('provides a local demo HTTP server', () => {
  assert.equal(typeof serverModule?.createWalletServer, 'function');
});

async function fixture(t, options = {}) {
  assert.ok(serverModule, 'The demo server implementation must exist');
  const server = serverModule.createWalletServer(options);
  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', resolve);
  });
  t.after(() => new Promise(resolve => server.close(resolve)));
  const base = `http://127.0.0.1:${server.address().port}`;
  return {
    base,
    async request(path, body, init = {}) {
      const response = await fetch(base + path, {
        method: body === undefined ? 'GET' : 'POST',
        headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
        body: body === undefined ? undefined : JSON.stringify(body), ...init,
      });
      return { status: response.status, body: await response.json(), headers: response.headers };
    },
  };
}
const cardInput = (extra = {}) => ({ isDemo: true, displayName: 'Personal demo', network: 'VISA', last4: '4821', ...extra });
async function addCard(api, extra = {}) {
  const result = await api.request('/wallet/cards', cardInput(extra));
  assert.equal(result.status, 201);
  return result.body.card;
}
async function sessionFor(api, cardId, extra = {}) {
  const result = await api.request('/payments/session', { isDemo: true, cardId, amountMinor: 1299, currency: 'EUR', merchant: 'Comercio demo', channel: 'IN_STORE', ...extra });
  assert.equal(result.status, 201);
  return result.body.session;
}
const authorization = (sessionId, extra = {}) => ({ isDemo: true, sessionId, authenticationMethod: 'DEMO_SIMULATION', confirmed: true, ...extra });

test('stores demo metadata and keeps appearance independent of the card network', async t => {
  const api = await fixture(t);
  const card = await addCard(api, { displayName: '  Viajes demo  ', appearance: { backgroundType: 'SKIN', backgroundValue: 'gold', chipStyle: 'GOLD' } });
  assert.match(card.id, /^demo-card-/);
  assert.equal(card.displayName, 'Viajes demo');
  assert.equal(card.network, 'VISA');
  assert.equal(card.last4, '4821');
  assert.equal(card.isDemo, true);
  assert.equal(card.appearance.backgroundValue, 'gold');
  assert.equal(card.appearance.chipStyle, 'GOLD');
  const listed = await api.request('/wallet/cards');
  assert.deepEqual(listed.body.cards, [card]);
});

test('creates AUTHORIZING sessions without transactions and confirms an explicit demo authorization once', async t => {
  const api = await fixture(t);
  const card = await addCard(api);
  const session = await sessionFor(api, card.id);
  assert.equal(session.state, 'AUTHORIZING');
  assert.equal(session.isDemo, true);
  assert.equal((await api.request('/transactions')).body.transactions.length, 0);
  const first = await api.request('/payments/authorize', authorization(session.id));
  assert.equal(first.status, 200);
  assert.equal(first.body.session.state, 'CONFIRMED');
  assert.equal(first.body.session.authenticationMethod, 'DEMO_SIMULATION');
  assert.equal(first.body.transaction.amountMinor, 1299);
  assert.equal(first.body.transaction.status, 'COMPLETED');
  assert.equal(first.body.transaction.isDemo, true);
  assert.equal(first.body.idempotent, false);
  const repeated = await api.request('/payments/authorize', authorization(session.id));
  assert.equal(repeated.status, 200);
  assert.equal(repeated.body.idempotent, true);
  assert.equal(repeated.body.transaction.id, first.body.transaction.id);
  assert.equal((await api.request('/transactions')).body.transactions.length, 1);
});

test('concurrent retries produce exactly one transaction', async t => {
  const api = await fixture(t);
  const session = await sessionFor(api, (await addCard(api)).id);
  const results = await Promise.all(Array.from({ length: 6 }, () => api.request('/payments/authorize', authorization(session.id))));
  assert.ok(results.every(result => result.status === 200));
  assert.equal(new Set(results.map(result => result.body.transaction.id)).size, 1);
  assert.equal((await api.request('/transactions')).body.transactions.length, 1);
});

test('requires explicit demo mode on every mutation and never accepts real authentication', async t => {
  const api = await fixture(t);
  const missing = cardInput(); delete missing.isDemo;
  for (const body of [missing, cardInput({ isDemo: false }), cardInput({ isDemo: 'true' })]) {
    const result = await api.request('/wallet/cards', body);
    assert.equal(result.status, 400);
    assert.equal(result.body.error.code, 'DEMO_REQUIRED');
  }
  const card = await addCard(api);
  const realSession = await api.request('/payments/session', { isDemo: false, cardId: card.id, amountMinor: 100, currency: 'EUR', merchant: 'Demo', channel: 'ONLINE' });
  assert.equal(realSession.body.error.code, 'DEMO_REQUIRED');
  const session = await sessionFor(api, card.id);
  for (const extra of [{ isDemo: false }, { authenticationMethod: 'BIOMETRIC' }, { authenticationMethod: 'PROVIDER' }, { confirmed: false }, { confirmed: 'true' }]) {
    assert.equal((await api.request('/payments/authorize', authorization(session.id, extra))).status, 400);
  }
  assert.equal((await api.request('/transactions')).body.transactions.length, 0);
});

test('rejects sensitive fields recursively without echoing values', async t => {
  const api = await fixture(t);
  for (const extra of [
    { pan: '4111111111111111' }, { cvv: '123' }, { Card_Number: '4111111111111111' },
    { appearance: { nested: { cvc: '123' } } }, { iban: 'ES0000000000000000000000' },
    { displayName: '4111 1111 1111 1111' }, { appearance: { backgroundValue: '4111111111111111' } },
  ]) {
    const result = await api.request('/wallet/cards', cardInput(extra));
    assert.equal(result.status, 400);
    assert.equal(result.body.error.code, 'SENSITIVE_DATA_REJECTED');
    assert.equal(JSON.stringify(result.body).includes('4111111111111111'), false);
  }
  assert.equal((await api.request('/wallet/cards')).body.cards.length, 0);
});

test('validates names, flags, enums, last4 and appearance values', async t => {
  const api = await fixture(t);
  for (const extra of [
    { displayName: '' }, { displayName: '   ' }, { displayName: 'x'.repeat(41) }, { displayName: 3 },
    { network: 'visa' }, { network: 'UNKNOWN' }, { last4: '12345' }, { last4: 1234 },
    { isLocked: 'false' }, { appearance: { network: 'AMEX' } }, { appearance: { backgroundType: 'NONE' } },
    { appearance: { chipStyle: 'PLATINUM' } }, { appearance: { textStyle: 'UNKNOWN' } },
    { appearance: { numberPosition: 'LEFT' } }, { appearance: { brightness: 1.1 } },
    { appearance: { contrast: -1 } }, { appearance: { textColor: 'red' } },
    { appearance: { backgroundType: 'SKIN', backgroundValue: 'brand-bank' } }, { unknown: true },
  ]) assert.equal((await api.request('/wallet/cards', cardInput(extra))).status, 400, JSON.stringify(extra));
  for (const network of ['VISA', 'MASTERCARD', 'AMEX', 'LOYALTY', 'GIFT']) {
    assert.equal((await addCard(api, { network })).network, network);
  }
});

test('rejects unknown, locked, hidden, and nonpayment cards', async t => {
  const api = await fixture(t);
  for (const [extra, code] of [[{ isLocked: true }, 'CARD_LOCKED'], [{ isHidden: true }, 'CARD_HIDDEN'], [{ network: 'LOYALTY' }, 'CARD_NOT_PAYABLE'], [{ network: 'GIFT' }, 'CARD_NOT_PAYABLE']]) {
    const card = await addCard(api, extra);
    const result = await api.request('/payments/session', { isDemo: true, cardId: card.id, amountMinor: 100, currency: 'EUR', merchant: 'Demo', channel: 'IN_STORE' });
    assert.equal(result.status, 409);
    assert.equal(result.body.error.code, code);
  }
  const unknown = await api.request('/payments/session', { isDemo: true, cardId: 'missing', amountMinor: 100, currency: 'EUR', merchant: 'Demo', channel: 'IN_STORE' });
  assert.equal(unknown.status, 404);
  assert.equal(unknown.body.error.code, 'CARD_NOT_FOUND');
});

test('rejects unsafe minor amounts and invalid currency, channel and merchant', async t => {
  const api = await fixture(t);
  const card = await addCard(api);
  const base = { isDemo: true, cardId: card.id, amountMinor: 100, currency: 'EUR', merchant: 'Demo', channel: 'IN_STORE' };
  for (const extra of [
    { amountMinor: 0 }, { amountMinor: -1 }, { amountMinor: 1.5 }, { amountMinor: '100' },
    { amountMinor: Number.MAX_SAFE_INTEGER + 1 }, { currency: 'USD' }, { currency: 'eur' },
    { channel: 'NFC_REAL' }, { merchant: '' }, { merchant: 'x'.repeat(81) }, { cardId: '' },
  ]) assert.equal((await api.request('/payments/session', { ...base, ...extra })).status, 400, JSON.stringify(extra));
  assert.equal((await api.request('/transactions')).body.transactions.length, 0);
});

test('expired sessions cannot create transactions', async t => {
  let now = 1_700_000_000_000;
  const api = await fixture(t, { clock: () => now, sessionTtlMs: 1000 });
  const session = await sessionFor(api, (await addCard(api)).id);
  assert.equal(session.expiresAt - session.createdAt, 1000);
  now += 1000;
  const result = await api.request('/payments/authorize', authorization(session.id));
  assert.equal(result.status, 410);
  assert.equal(result.body.error.code, 'SESSION_EXPIRED');
  assert.equal((await api.request('/transactions')).body.transactions.length, 0);
});

test('rejects missing sessions and payloads that alter session-bound payment metadata', async t => {
  const api = await fixture(t);
  const session = await sessionFor(api, (await addCard(api)).id);
  const unknown = await api.request('/payments/authorize', authorization('missing'));
  assert.equal(unknown.status, 404);
  assert.equal(unknown.body.error.code, 'SESSION_NOT_FOUND');
  for (const extra of [{ amountMinor: 1 }, { cardId: 'another-card' }, { merchant: 'Changed' }, { sessionId: '' }]) {
    assert.equal((await api.request('/payments/authorize', authorization(session.id, extra))).status, 400);
  }
});

test('rejects malformed JSON, nonobjects, wrong methods, unknown routes, and oversized bodies', async t => {
  const api = await fixture(t, { maxBodyBytes: 1024 });
  for (const body of ['{', 'null', '[]', 'true']) {
    assert.equal((await api.request('/wallet/cards', undefined, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body })).status, 400);
  }
  const noJson = await api.request('/wallet/cards', undefined, { method: 'POST', body: '{}' });
  assert.equal(noJson.status, 415);
  assert.equal(noJson.body.error.code, 'JSON_REQUIRED');
  const large = await api.request('/wallet/cards', cardInput({ displayName: 'x'.repeat(2048) }));
  assert.equal(large.status, 413);
  assert.equal(large.body.error.code, 'BODY_TOO_LARGE');
  assert.equal((await api.request('/bank/accounts')).status, 404);
  assert.equal((await api.request('/payments/session')).status, 405);
  assert.equal((await api.request('/wallet/cards?pan=4111111111111111')).status, 400);
});

test('CORS denies origins by default and allows only an explicitly configured local origin', async t => {
  const api = await fixture(t, { allowedOrigin: 'http://localhost:3000' });
  const denied = await api.request('/wallet/cards', undefined, { headers: { Origin: 'https://untrusted.example' } });
  assert.equal(denied.status, 403);
  assert.equal(denied.headers.get('access-control-allow-origin'), null);
  const allowed = await api.request('/wallet/cards', undefined, { headers: { Origin: 'http://localhost:3000' } });
  assert.equal(allowed.status, 200);
  assert.equal(allowed.headers.get('access-control-allow-origin'), 'http://localhost:3000');
  const preflight = await fetch(api.base + '/wallet/cards', { method: 'OPTIONS', headers: { Origin: 'http://localhost:3000', 'Access-Control-Request-Method': 'POST', 'Access-Control-Request-Headers': 'content-type' } });
  assert.equal(preflight.status, 204);
  const defaults = await fixture(t);
  assert.equal((await defaults.request('/wallet/cards', undefined, { headers: { Origin: 'http://localhost:3000' } })).status, 403);
  assert.throws(() => serverModule.createWalletServer({ allowedOrigin: '*' }));
  assert.throws(() => serverModule.createWalletServer({ allowedOrigin: 'https://remote.example' }));
});

test('optional bearer token protects API metadata and mutations', async t => {
  const api = await fixture(t, { apiToken: 'test-only-local-token' });
  assert.equal((await api.request('/wallet/cards')).status, 401);
  assert.equal((await api.request('/wallet/cards', cardInput())).status, 401);
  assert.equal((await api.request('/transactions', undefined, { headers: { Authorization: 'Bearer wrong' } })).status, 401);
  const result = await api.request('/wallet/cards', cardInput(), { headers: { 'Content-Type': 'application/json', Authorization: 'Bearer test-only-local-token' } });
  assert.equal(result.status, 201);
});

test('explicit null appearance values cannot silently fall back to valid defaults', async t => {
  const api = await fixture(t);
  for (const field of ['backgroundType', 'backgroundValue', 'textColor', 'brightness', 'contrast', 'chipStyle', 'numberPosition', 'textStyle']) {
    const result = await api.request('/wallet/cards', cardInput({ appearance: { [field]: null } }));
    assert.equal(result.status, 400, field);
    assert.equal(result.body.error.code, 'INVALID_INPUT');
  }
});

test('invalid UTF-8 bytes cannot be stored as replacement characters', async t => {
  const api = await fixture(t);
  const body = Buffer.concat([
    Buffer.from('{"isDemo":true,"displayName":"'), Buffer.from([0xff]),
    Buffer.from('","network":"VISA","last4":"4821"}'),
  ]);
  const result = await api.request('/wallet/cards', undefined, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body });
  assert.equal(result.status, 400);
  assert.equal(result.body.error.code, 'INVALID_JSON');
  assert.equal((await api.request('/wallet/cards')).body.cards.length, 0);
});

test('numeric segments in opaque demo UUID references are not mistaken for PAN input', async t => {
  const api = await fixture(t);
  const cardId = 'demo-card-12345678-1234-1234-1234-123456789012';
  const result = await api.request('/payments/session', { isDemo: true, cardId, amountMinor: 100, currency: 'EUR', merchant: 'Demo', channel: 'ONLINE' });
  assert.equal(result.status, 404);
  assert.equal(result.body.error.code, 'CARD_NOT_FOUND');
  const sessionId = 'demo-session-12345678-1234-1234-1234-123456789012';
  const unknown = await api.request('/payments/authorize', authorization(sessionId));
  assert.equal(unknown.status, 404);
  assert.equal(unknown.body.error.code, 'SESSION_NOT_FOUND');
  assert.equal((await api.request('/wallet/cards', cardInput({ displayName: cardId }))).status, 400);
});
