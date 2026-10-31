# Phase 6: JWT access-token setup

The API accepts signed HS256 access tokens in the `Authorization: Bearer <token>` header. Tokens last 15 minutes. The subject is the database user ID; email and authorities are copied from the authenticated database principal when a token is issued. The application requires a Base64-encoded signing key that decodes to at least 32 random bytes.

## Local PowerShell setup

In the same PowerShell window used to start the backend, generate a local key without displaying it or saving it in the repository:

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Start the backend from that PowerShell session. The environment variable is process-local and disappears when the window closes. Generate and configure a stable secret in your approved local secret store if you need tokens to remain verifiable across restarts. Do not place the secret in YAML, source control, screenshots, or chat.

The shared application configuration deliberately has no usable fallback. Startup fails when `JWT_SECRET` is absent, invalid Base64, or decodes to fewer than 32 bytes. Production must receive the secret through its deployment secret manager. A different key invalidates previously issued access tokens.

## CSRF and refresh cookies

CSRF checks are skipped for requests that carry a Bearer authorization header, except `POST /api/v1/auth/refresh` and `POST /api/v1/auth/logout`, and narrowly for `POST /api/v1/auth/register` and `POST /api/v1/auth/login`. Refresh and logout use an ambient Secure, HttpOnly cookie, so both remain CSRF-protected even if an Authorization header is present. `GET /api/v1/auth/csrf` sets a readable `XSRF-TOKEN` cookie and returns its token and the `X-XSRF-TOKEN` header name. The cookie shares Secure, SameSite, and Path settings with the refresh cookie; its HttpOnly flag is disabled for browser JavaScript.

## Customer authentication APIs

Registration accepts `name`, `email`, `phone`, and `password`. It always assigns `CUSTOMER`, and returns customer profile fields without a password hash. Login accepts `email` and `password` and returns the existing access-token response with `tokenType: "Bearer"` and `expiresInSeconds: 900`. On successful login, the server also sets the opaque refresh token in the configured HttpOnly cookie; only its SHA-256 hex hash is stored in MySQL. `POST /api/v1/auth/refresh` rotates a valid refresh token and returns a new access token. Reuse of a revoked, unexpired token revokes all active refresh sessions for that user. `POST /api/v1/auth/logout` revokes only the presented session token and is idempotent when it is absent, unknown, or already revoked.

## Refresh-token foundation (Phase 6.5, Batch 1)

The refresh-token lifetime binds the existing `app.jwt.refresh-ttl-days` setting, which defaults to seven days. Refresh-cookie defaults are `refresh_token`, HttpOnly, Secure, SameSite `Strict`, and Path `/api/v1/auth`. The configuration rejects insecure cookie settings. Repository interfaces now include pessimistic-locking lookups for a refresh-token row and a user row; they do not perform database operations until a later lifecycle service uses them. Successful login generates 32 random bytes, sends the URL-safe token only in the cookie, and stores only its lowercase SHA-256 hash.

Batch 1 supplied repository locking and cookie/TTL configuration; Batch 2 issued the initial login cookie; Batch 3 implements transactional rotation and replay protection. V1 already contains the required `refresh_tokens` table, so no migration is needed.
