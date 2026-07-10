# iFood — Authentication

iFood uses **OAuth 2.0** with Bearer tokens. All requests require `Authorization: Bearer <accessToken>`.
HTTPS (TLS 1.2+) is mandatory — plain HTTP and unauthenticated requests are rejected.

**Base URL:** `https://merchant-api.ifood.com.br/authentication/v1.0`

The token is **application-level**, not per-merchant. A single `accessToken` carries a `merchant_scope` claim
listing every merchant that has authorized the app. When a new merchant authorizes, a new token must be
requested so that merchant's permissions are included.

---

## Expiry reference

| Code / Token | Expiry |
|---|---|
| `userCode` (link code) | 10 minutes |
| `authorizationCode` | 5 minutes |
| `accessToken` | 3 hours (default) |
| `refreshToken` | 168 hours (7 days) |

> **Never hardcode these durations.** Always use the `expiresIn` value returned by the API.
> iFood may change expiry periods at any time.

---

## Flow: Distributed app (authorization_code)

Used when the app is public and each merchant must explicitly authorize it through the Partner Portal.

```
1. POST /oauth/userCode
       → returns userCode (XXXX-XXXX) + authorizationCodeVerifier
       → store authorizationCodeVerifier securely (backend only — never expose to frontend)
       → display userCode to merchant; it expires in 10 minutes

2. Merchant opens https://portal.ifood.com.br/setup/onboarding
       → logs in → clicks "Integrações" → "Ativar aplicativo por código"
       → enters userCode → portal issues an authorizationCode

3. Merchant provides authorizationCode back to the app (via UI input field)

4. POST /oauth/token (grantType=authorization_code)
       → send: clientId, clientSecret, authorizationCode, authorizationCodeVerifier
       → returns: accessToken + refreshToken + expiresIn

5. Delete previous app token; persist new accessToken, refreshToken, expiresAt, refreshExpiresAt

6. POST /oauth/token (grantType=refresh_token) — renew before expiry
       → send: clientId, clientSecret, refreshToken
       → returns new accessToken + expiresIn
```

**Propagation delay:** after a new merchant authorizes, it can take up to 10 minutes for the new
`merchantId` to appear in the merchants list endpoint. Request a new token after this delay if needed.

---

## `POST /oauth/userCode`

Generates the link code the merchant enters in the Partner Portal.

**Request** (`application/x-www-form-urlencoded`):

| Field | Required | Description |
|---|---|---|
| `clientId` | Yes | App client ID |

**Response (200)**:

| Field | Description | Example |
|---|---|---|
| `userCode` | Code (XXXX-XXXX format) displayed to the merchant | `HJLX-LPSQ` |
| `authorizationCodeVerifier` | Store on backend — required for the token exchange | `test123` |
| `verificationUrl` | Portal URL to show the merchant | `https://portal.ifood.com.br/apps/code` |
| `verificationUrlComplete` | Portal URL with `userCode` pre-filled as query param | `https://portal.ifood.com.br/apps/code?c=HJLX-LPSQ` |
| `expiresIn` | `userCode` validity in seconds | `600` |

---

## `POST /oauth/token`

Exchanges an authorization code or refreshes an existing token.

**Request** (`application/x-www-form-urlencoded`):

| Field | Required | Description |
|---|---|---|
| `grantType` | Yes | `authorization_code` or `refresh_token` |
| `clientId` | Yes | App client ID |
| `clientSecret` | Yes | App client secret |
| `authorizationCode` | `authorization_code` only | Code the merchant received from the portal |
| `authorizationCodeVerifier` | `authorization_code` only | Verifier from `/oauth/userCode` — must match exactly |
| `refreshToken` | `refresh_token` only | Refresh token from a previous token response |

**Response (200)**:

| Field | Description |
|---|---|
| `accessToken` | JWT — use as `Authorization: Bearer <token>` |
| `type` | Always `bearer` |
| `expiresIn` | Expiry in seconds — use this to compute `expiresAt = now() + expiresIn` |

**Error responses**:

| Status | `error.code` | Meaning |
|---|---|---|
| 401 | `Unauthorized` | Invalid credentials or mismatched verifier |
| 500 | `InternalServerError` | Unexpected server error |

---

## Token lifecycle rules

| Event | Action |
|---|---|
| New merchant authorizes | Exchange `authorization_code` → new `accessToken` replaces the old one |
| `expiresAt` within 5 min | Use `refreshToken` to get a new `accessToken` |
| API returns `401` | Force-refresh the token immediately, then retry once |
| `refreshToken` expired (`refreshExpiresAt` passed) | Full re-authorization required — no silent recovery; notify merchant |
| Rate limit guard | **Never** request a new token if the current one is still valid |

---

## Error handling

| Status | Cause | Action |
|---|---|---|
| `401` | `accessToken` expired | Refresh token → retry once |
| `403` – `ifood-kong-validate-headers-plugin: forbidden` | App lacks the required scope/module | **Do not retry** — log + alert; requires app reconfiguration |
| `403` – `user is forbidden to access merchant` | Merchant hasn't authorized yet, revoked access, or propagation delay | **Do not retry automatically** — surface to user; suggest re-authorization after 10 min |

> Never auto-retry on `403`. Only retry after the permission issue has been explicitly fixed.

---

## Storage

| Data | Where |
|---|---|
| `clientId` / `clientSecret` | Environment variables (`IFOOD_CLIENT_ID`, `IFOOD_CLIENT_SECRET`) |
| `accessToken` / `refreshToken` | Database — `ifood_app_token` table (one live row) |
| `expiresAt` / `refreshExpiresAt` | Database — computed from `expiresIn` at token issuance |
| `authorizationCodeVerifier` | Backend in-memory only (per-merchant, discarded after exchange or expiry) |
