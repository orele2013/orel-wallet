import { createServer } from 'node:http';
import { randomUUID, timingSafeEqual } from 'node:crypto';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const NETWORKS = ['VISA', 'MASTERCARD', 'AMEX', 'LOYALTY', 'GIFT'];
const PAYABLE_NETWORKS = ['VISA', 'MASTERCARD', 'AMEX'];
const SKINS = ['blue', 'graphite', 'gold', 'violet', 'mint', 'ice', 'coral'];
const SENSITIVE_KEYS = new Set([
  'pan', 'primaryaccountnumber', 'cardnumber', 'fullcardnumber', 'cvv', 'cvv2',
  'cvc', 'cvc2', 'cid', 'securitycode', 'pin', 'pinblock', 'iban', 'accountnumber',
  'bankaccount', 'routingnumber', 'expiry', 'expiration', 'expirydate', 'expirationdate',
  'expmonth', 'expyear', 'track1', 'track2', 'trackdata', 'cryptogram', 'paymenttoken',
]);
const ROUTES = new Map([
  ['/health', ['GET']], ['/wallet/cards', ['GET', 'POST']],
  ['/payments/session', ['POST']], ['/payments/authorize', ['POST']],
  ['/transactions', ['GET']],
]);

class ApiError extends Error {
  constructor(status, code, message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}
const invalid = () => { throw new ApiError(400, 'INVALID_INPUT', 'The request contains invalid or unsupported metadata.'); };
const isObject = value => value !== null && typeof value === 'object' && !Array.isArray(value);

function fields(value, allowed) {
  if (!isObject(value) || Object.keys(value).some(key => !allowed.includes(key))) invalid();
}

function scanForSensitiveData(value) {
  const pending = [{ value, depth: 0 }];
  while (pending.length) {
    const entry = pending.pop();
    if (entry.depth > 32) invalid();
    if (typeof entry.value === 'string') {
      const referenceKind = entry.key === 'cardId' ? 'card' : entry.key === 'sessionId' ? 'session' : undefined;
      const isDemoReference = referenceKind && new RegExp(`^demo-${referenceKind}-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$`).test(entry.value);
      // Reject PAN-like strings even when hidden inside free text; no value is echoed.
      // Card/session references are validated against server-owned maps instead.
      if (!isDemoReference && /(?:\d[ -]?){12,}/.test(entry.value)) {
        throw new ApiError(400, 'SENSITIVE_DATA_REJECTED', 'Only fictional demo metadata is accepted; sensitive payment data is forbidden.');
      }
    } else if (entry.value !== null && typeof entry.value === 'object') {
      for (const [key, nested] of Object.entries(entry.value)) {
        if (SENSITIVE_KEYS.has(key.toLowerCase().replace(/[^a-z0-9]/g, ''))) {
          throw new ApiError(400, 'SENSITIVE_DATA_REJECTED', 'Only fictional demo metadata is accepted; sensitive payment data is forbidden.');
        }
        if (key === 'isDemo' && nested !== true) {
          throw new ApiError(400, 'DEMO_REQUIRED', 'This local server accepts only isDemo: true.');
        }
        pending.push({ value: nested, depth: entry.depth + 1, key });
      }
    }
  }
}

function requireDemo(body) {
  if (body.isDemo !== true) throw new ApiError(400, 'DEMO_REQUIRED', 'This local server accepts only isDemo: true.');
}

function name(value, maxLength) {
  if (typeof value !== 'string') invalid();
  const trimmed = value.trim();
  if (!trimmed || [...trimmed].length > maxLength || /[\u0000-\u001f\u007f]/.test(trimmed)) invalid();
  return trimmed;
}

function oneOf(value, allowed) {
  if (!allowed.includes(value)) invalid();
  return value;
}

function flag(value, fallback = false) {
  if (value === undefined) return fallback;
  if (typeof value !== 'boolean') invalid();
  return value;
}

function appearance(input = {}) {
  fields(input, ['backgroundType', 'backgroundValue', 'textColor', 'brightness', 'contrast', 'chipStyle', 'numberPosition', 'textStyle']);
  if (Object.values(input).some(value => value === null)) invalid();
  const result = {
    backgroundType: input.backgroundType ?? 'SKIN', backgroundValue: input.backgroundValue ?? 'blue',
    textColor: input.textColor ?? '#FFFFFF', brightness: input.brightness ?? 0.5,
    contrast: input.contrast ?? 0.5, chipStyle: input.chipStyle ?? 'SILVER',
    numberPosition: input.numberPosition ?? 'BOTTOM', textStyle: input.textStyle ?? 'CLASSIC',
  };
  oneOf(result.backgroundType, ['SKIN', 'COLOR', 'GRADIENT', 'IMAGE']);
  oneOf(result.chipStyle, ['SILVER', 'GOLD', 'MINIMAL']);
  oneOf(result.numberPosition, ['BOTTOM', 'TOP']);
  oneOf(result.textStyle, ['CLASSIC', 'MONO']);
  if (typeof result.textColor !== 'string' || !/^#[0-9A-Fa-f]{6}$/.test(result.textColor)) invalid();
  for (const value of [result.brightness, result.contrast]) {
    if (typeof value !== 'number' || !Number.isFinite(value) || value < 0 || value > 1) invalid();
  }
  if (typeof result.backgroundValue !== 'string') invalid();
  if (['SKIN', 'GRADIENT'].includes(result.backgroundType)) oneOf(result.backgroundValue, SKINS);
  if (result.backgroundType === 'COLOR' && !/^#[0-9A-Fa-f]{6}$/.test(result.backgroundValue)) invalid();
  // IMAGE is a fictional opaque reference, never a URL, path, or uploaded image.
  if (result.backgroundType === 'IMAGE' && !/^demo-image:[a-z0-9-]{1,64}$/.test(result.backgroundValue)) invalid();
  return result;
}

function readJson(request, maxBytes) {
  if (!/^application\/json(?:\s*;\s*charset=utf-8)?$/i.test(request.headers['content-type'] ?? '')) {
    request.resume();
    throw new ApiError(415, 'JSON_REQUIRED', 'POST requests require application/json encoded as UTF-8.');
  }
  if (request.headers['content-encoding'] && request.headers['content-encoding'] !== 'identity') {
    request.resume();
    throw new ApiError(415, 'JSON_REQUIRED', 'Compressed request bodies are unsupported.');
  }
  if (Number(request.headers['content-length']) > maxBytes) {
    request.resume();
    throw new ApiError(413, 'BODY_TOO_LARGE', 'The request body exceeds the local demo limit.');
  }
  return new Promise((resolveBody, reject) => {
    let size = 0;
    let exceeded = false;
    const chunks = [];
    request.on('data', chunk => {
      size += chunk.length;
      if (size > maxBytes) {
        chunks.length = 0;
        if (!exceeded) reject(new ApiError(413, 'BODY_TOO_LARGE', 'The request body exceeds the local demo limit.'));
        exceeded = true;
      } else if (!exceeded) chunks.push(chunk);
    });
    request.on('error', () => reject(new ApiError(400, 'INVALID_JSON', 'The JSON request body could not be read.')));
    request.on('end', () => {
      if (exceeded) return;
      let body;
      try { body = JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(Buffer.concat(chunks))); }
      catch { return reject(new ApiError(400, 'INVALID_JSON', 'The request body must be a valid JSON object.')); }
      if (!isObject(body)) return reject(new ApiError(400, 'INVALID_INPUT', 'The request body must be a JSON object.'));
      resolveBody(body);
    });
  });
}

function localOrigin(value) {
  if (!value) return undefined;
  const url = new URL(value);
  if (url.origin !== value || url.protocol !== 'http:' || !['localhost', '127.0.0.1', '[::1]'].includes(url.hostname)) {
    throw new Error('MOCK_ALLOWED_ORIGIN must be a single exact local HTTP origin.');
  }
  return value;
}

function positiveInteger(value, max, label) {
  if (!Number.isSafeInteger(value) || value <= 0 || value > max) throw new Error(`${label} is outside its supported range.`);
  return value;
}

/** Each server owns ephemeral demo state; it never contacts a payment provider. */
export function createWalletServer({ clock = Date.now, sessionTtlMs = 60_000, maxBodyBytes = 16_384, allowedOrigin, apiToken = '' } = {}) {
  positiveInteger(sessionTtlMs, 300_000, 'Session TTL');
  positiveInteger(maxBodyBytes, 1_048_576, 'Body limit');
  if (typeof clock !== 'function' || typeof apiToken !== 'string') throw new Error('Invalid local server configuration.');
  const origin = localOrigin(allowedOrigin);
  const tokenBytes = Buffer.from(apiToken);
  const cards = new Map();
  const sessions = new Map();
  const transactions = new Map();

  function usableCard(cardId) {
    const card = cards.get(cardId);
    if (!card) throw new ApiError(404, 'CARD_NOT_FOUND', 'The demo card does not exist.');
    if (card.isLocked) throw new ApiError(409, 'CARD_LOCKED', 'The demo card is locked.');
    if (card.isHidden) throw new ApiError(409, 'CARD_HIDDEN', 'The demo card is hidden.');
    if (!PAYABLE_NETWORKS.includes(card.network)) throw new ApiError(409, 'CARD_NOT_PAYABLE', 'This demo card type cannot simulate payments.');
    return card;
  }

  const server = createServer(async (request, response) => {
    const send = (status, payload) => {
      response.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' });
      response.end(JSON.stringify({ isDemo: true, ...payload }));
    };
    try {
      const host = new URL(`http://${request.headers.host ?? ''}`);
      if (!['127.0.0.1', 'localhost', '[::1]'].includes(host.hostname)) throw new ApiError(403, 'LOCAL_HOST_REQUIRED', 'This API is available only through a local host.');
      if (request.headers.origin !== undefined) {
        if (!origin || request.headers.origin !== origin) throw new ApiError(403, 'ORIGIN_DENIED', 'This browser origin is not permitted.');
        response.setHeader('Access-Control-Allow-Origin', origin);
        response.setHeader('Vary', 'Origin');
      }
      const url = new URL(request.url, 'http://127.0.0.1');
      if (url.search) {
        scanForSensitiveData(Object.fromEntries(url.searchParams));
        invalid(); // No route accepts query parameters.
      }
      const methods = ROUTES.get(url.pathname);
      if (!methods) throw new ApiError(404, 'NOT_FOUND', 'This local demo route does not exist.');
      if (request.method === 'OPTIONS' && request.headers.origin !== undefined) {
        const requestedMethod = request.headers['access-control-request-method'];
        const requestedHeaders = (request.headers['access-control-request-headers'] ?? '').split(',').map(value => value.trim().toLowerCase()).filter(Boolean);
        if (!methods.includes(requestedMethod) || requestedHeaders.some(value => !['content-type', 'authorization'].includes(value))) {
          throw new ApiError(403, 'ORIGIN_DENIED', 'The requested browser operation is not permitted.');
        }
        response.writeHead(204, { 'Access-Control-Allow-Methods': methods.join(', '), 'Access-Control-Allow-Headers': 'Content-Type, Authorization', 'Cache-Control': 'no-store' });
        response.end();
        return;
      }
      if (!methods.includes(request.method)) {
        response.setHeader('Allow', methods.join(', '));
        throw new ApiError(405, 'METHOD_NOT_ALLOWED', 'This HTTP method is unsupported for the route.');
      }
      if (apiToken) {
        const supplied = Buffer.from(request.headers.authorization?.startsWith('Bearer ') ? request.headers.authorization.slice(7) : '');
        if (supplied.length !== tokenBytes.length || !timingSafeEqual(supplied, tokenBytes)) {
          throw new ApiError(401, 'UNAUTHORIZED', 'The optional local API token is missing or invalid.');
        }
      }
      if (request.method === 'GET') {
        if (request.headers['transfer-encoding'] || Number(request.headers['content-length']) > 0) invalid();
        if (url.pathname === '/health') send(200, { status: 'ok', mode: 'DEMO', storage: 'memory' });
        if (url.pathname === '/wallet/cards') send(200, { cards: [...cards.values()] });
        if (url.pathname === '/transactions') send(200, { transactions: [...transactions.values()] });
        return;
      }
      const body = await readJson(request, maxBodyBytes);
      scanForSensitiveData(body);
      requireDemo(body);
      if (url.pathname === '/wallet/cards') {
        fields(body, ['isDemo', 'displayName', 'network', 'last4', 'isLocked', 'isHidden', 'appearance']);
        const displayName = name(body.displayName, 40);
        const network = oneOf(body.network, NETWORKS);
        if (typeof body.last4 !== 'string' || !/^\d{4}$/.test(body.last4)) invalid();
        const card = {
          id: `demo-card-${randomUUID()}`, displayName, network, last4: body.last4, isDemo: true,
          isLocked: flag(body.isLocked), isHidden: flag(body.isHidden), appearance: appearance(body.appearance),
        };
        cards.set(card.id, card);
        send(201, { card });
      } else if (url.pathname === '/payments/session') {
        fields(body, ['isDemo', 'cardId', 'amountMinor', 'currency', 'merchant', 'channel']);
        const cardId = name(body.cardId, 80);
        if (!Number.isSafeInteger(body.amountMinor) || body.amountMinor <= 0) invalid();
        const currency = oneOf(body.currency, ['EUR']);
        const merchant = name(body.merchant, 80);
        const channel = oneOf(body.channel, ['IN_STORE', 'ONLINE']);
        usableCard(cardId);
        const createdAt = clock();
        const session = {
          id: `demo-session-${randomUUID()}`, cardId, amountMinor: body.amountMinor,
          currency, merchant, channel, state: 'AUTHORIZING', isDemo: true,
          createdAt, expiresAt: createdAt + sessionTtlMs, authenticationMethod: null,
        };
        sessions.set(session.id, session);
        send(201, { session });
      } else if (url.pathname === '/payments/authorize') {
        fields(body, ['isDemo', 'sessionId', 'authenticationMethod', 'confirmed']);
        const sessionId = name(body.sessionId, 80);
        if (body.authenticationMethod !== 'DEMO_SIMULATION' || body.confirmed !== true) {
          throw new ApiError(400, 'DEMO_CONFIRMATION_REQUIRED', 'Explicit demo simulation and confirmed: true are required.');
        }
        const session = sessions.get(sessionId);
        if (!session) throw new ApiError(404, 'SESSION_NOT_FOUND', 'The demo payment session does not exist.');
        // Return the original outcome even if a successful retry arrives after its TTL.
        if (session.state === 'CONFIRMED') {
          send(200, { session, transaction: transactions.get(session.transactionId), idempotent: true });
          return;
        }
        if (clock() >= session.expiresAt || session.state === 'EXPIRED') {
          session.state = 'EXPIRED';
          throw new ApiError(410, 'SESSION_EXPIRED', 'The demo payment session has expired.');
        }
        if (session.state !== 'AUTHORIZING') throw new ApiError(409, 'INVALID_SESSION_STATE', 'The demo session cannot be authorized.');
        const card = usableCard(session.cardId);
        const transaction = {
          id: `demo-tx-${randomUUID()}`, sessionId, cardId: card.id, cardDisplayName: card.displayName,
          network: card.network, last4: card.last4, amountMinor: session.amountMinor, currency: session.currency,
          merchant: session.merchant, channel: session.channel, status: 'COMPLETED', isDemo: true,
          authenticationMethod: 'DEMO_SIMULATION', createdAt: clock(),
        };
        // No await between confirmation and storage: concurrent retries observe this same outcome.
        session.state = 'CONFIRMED';
        session.authenticationMethod = 'DEMO_SIMULATION';
        session.transactionId = transaction.id;
        transactions.set(transaction.id, transaction);
        send(200, { session, transaction, idempotent: false });
      }
    } catch (error) {
      request.resume();
      const known = error instanceof ApiError;
      send(known ? error.status : 500, { error: { code: known ? error.code : 'INTERNAL_ERROR', message: known ? error.message : 'The local demo request could not be completed.' } });
    }
  });
  server.requestTimeout = 10_000;
  server.headersTimeout = 10_000;
  return server;
}

export function startLocalServer(env = process.env) {
  const port = positiveInteger(Number(env.PORT || 8787), 65_535, 'PORT');
  const ttlSeconds = positiveInteger(Number(env.MOCK_SESSION_TTL_SECONDS || 60), 300, 'MOCK_SESSION_TTL_SECONDS');
  const server = createWalletServer({ sessionTtlMs: ttlSeconds * 1000, allowedOrigin: env.MOCK_ALLOWED_ORIGIN || undefined, apiToken: env.MOCK_API_TOKEN || '' });
  // Deliberately no configurable bind address: this mock must stay on loopback.
  server.listen(port, '127.0.0.1', () => {
    console.log(`Orel Wallet DEMO API: http://127.0.0.1:${port} (memory only, no real payments)`);
  });
  return server;
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const server = startLocalServer();
    server.on('error', () => { console.error('The local DEMO API could not start. Check its port and configuration.'); process.exitCode = 1; });
    for (const signal of ['SIGINT', 'SIGTERM']) process.once(signal, () => server.close());
  } catch {
    console.error('Invalid local DEMO API configuration. Check PORT and MOCK_* values.');
    process.exitCode = 1;
  }
}
