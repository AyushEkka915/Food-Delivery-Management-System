# Phase 6.5 — Refresh token rotation (Batch 3)

`POST /api/v1/auth/refresh` reads the raw refresh credential only from the configured HttpOnly cookie. The endpoint does not read a request body or query parameter. On success, it returns the existing access-token response envelope and replaces the cookie. On failure, it returns the same generic 401 response and clears the cookie. `POST /api/v1/auth/logout` uses the same cookie-only lookup and user-then-token lock order, revokes only that token, and is idempotent for missing, unknown, or already-revoked credentials.

## Rotation and replay handling

Refresh token rows store only the lowercase SHA-256 hexadecimal digest. The lifecycle service checks the opaque token's expected URL-safe Base64 shape, hashes it, performs a scalar lookup for the owning user ID, and then locks rows in this order: user row, refresh-token row. The token row is loaded only after obtaining the user lock, so simultaneous requests for a user's refresh tokens serialize and the token's state is re-read under lock.

A valid token is revoked and its replacement hash is inserted in the same transaction. The access JWT is generated from the user's current database roles. A revoked, unexpired token is treated as replay: the service revokes every unrevoked token for that user and returns an internal outcome instead of throwing. Spring commits that transaction before the controller translates the outcome to HTTP 401. Expired tokens are rejected without triggering user-wide replay revocation.

The user-row lock serializes concurrent rotations. If two requests race with the same old token, one can rotate; after it commits, the waiting request sees the old token revoked and its replay response revokes the replacement and all other active sessions. No refresh token, digest, or internal token state is included in JSON or logs.

## CSRF and cookies

Refresh and logout are not CSRF-exempt. They remain covered even when a request includes a Bearer Authorization header; the Bearer-based CSRF exemption excludes both routes. `GET /api/v1/auth/csrf` provides a readable `XSRF-TOKEN` cookie and returns the same token with the `X-XSRF-TOKEN` header name. The CSRF cookie uses the configured refresh-cookie Secure, SameSite, and Path attributes, but has HttpOnly disabled so browser JavaScript can read it. Spring Security 6.5's XOR handler continues to mask request attributes; the SPA request handler accepts the raw repository token in the configured header. The refresh cookie itself remains HttpOnly.

## Verification notes

Unit and MockMvc tests cover rotation and logout behavior, cookie handling, generic failures, CSRF token bootstrap, body/query non-use, and the refresh/logout CSRF boundary. Concurrency unit tests simulate the user-row lock. A real MySQL concurrency test must use a separately provisioned isolated test database; the development database is not used by this batch.
