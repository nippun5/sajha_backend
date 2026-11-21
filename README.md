# SaJHa Backend — Phase 4A Foundation

**Java 21 · Spring Boot 4.1.3 · MySQL 8.4 · Flyway · Spring Security**

This is a **new, separate backend**. It does not modify `sajha_app` (Flutter) or `sajha_admin` (React). It implements the identity/health slice of the **Phase 3C v1 API contract**, not the entire contract.

## Implemented in Phase 4A

- `GET /api/v1/health` and `/actuator/health`
- `POST /api/v1/auth/register`: email/password, required E.164 phone, **unverified initially**
- `POST /api/v1/auth/login`: BCrypt password verification, generic invalid-credential response
- `GET /api/v1/me`: authenticated account, roles and verified contact numbers
- `POST /api/v1/auth/refresh`: 15-minute HS256 JWT access token + rotating random refresh token, whose SHA-256 hash is persisted in MySQL. Reuse revokes the session family.
- `POST /api/v1/auth/logout`: revokes the submitted refresh session
- `POST /api/v1/me/phone/challenges` and `/phone/verify`: Twilio Verify SMS ownership proof **only if** correctly configured. Without credentials, the endpoints fail closed with `503 SMS_NOT_CONFIGURED`. No dummy verification codes, no admin override.
- MySQL migration `V1__identity_foundation.sql`, default USER role only, basic rate limits, automated unit tests, Dockerfile and local MySQL Compose.
- **All admin and unimplemented content routes denied**. No administrator can be created through signup.

**Not implemented yet:** Google/Facebook/Apple exchange, password reset, admin cookie/MFA/CSRF, marketplace CRUD, image storage, business approval, reviews or frontend API adapters. These routes are not "mock success" endpoints. Do **not** switch your working Flutter or admin apps away from their demo repositories yet.

## 1. Prerequisites (Mac)

Install **Java 21**, Maven 3.9+ and Docker Desktop (or provide your own MySQL 8.4-compatible database).

```bash
java -version
mvn -version
# macOS Homebrew, only if missing:
brew install openjdk@21 maven
```

If you use a remote Aiven MySQL database instead of Docker, do not run `docker compose` and set DB variables to Aiven's connection details using TLS (below).

## 2. Start local MySQL

Inside the extracted `sajha_backend` project directory:

```bash
docker compose up -d
```

Default Docker-only sample credentials are defined in `compose.yaml` and `application.yml`; **never use these credentials in Aiven or Render**. Database listens on **127.0.0.1:3307**.

## 3. Set required JWT secret

```bash
export JWT_SECRET_BASE64="$(openssl rand -base64 48)"
```

Save this secret in a password manager for ongoing local sessions; changing it invalidates previously issued access tokens. Do not commit the secret into Git. For production use a secret provided through Render environment variables. Never reuse a key across development and production.

Optional live OTP service (not required to run registration/login):

```bash
export TWILIO_ACCOUNT_SID='AC...'
export TWILIO_AUTH_TOKEN='your-secret'
export TWILIO_VERIFY_SERVICE_SID='VA...'
```

**SMS verification can incur provider charges** and may require configuring allowed regions/test recipients. Without provider credentials, `phoneVerified` remains `false`, and future posting APIs must deny access.

## 4. Run tests and start backend

```bash
mvn clean test
mvn spring-boot:run
```

On first successful startup, Flyway creates database tables. Hibernate runs `ddl-auto=validate`, so unexpected model/schema drift should cause startup failure rather than silently altering the database.

Check (in a second terminal):

```bash
curl http://localhost:8080/api/v1/health
# expected: {"status":"UP"}
```

Or run the scripted smoke tests after startup (requires `jq`):

```bash
bash scripts/smoke.sh
```

**Caution:** The smoke test stores a test account in your currently configured database; run it only on a local or disposable development database.

## 5. Register/login manually

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Sample User","email":"sample@example.test","password":"MyExamplePassword123!","phone":"+13145550123"}'
```

`201` returns `accessToken`, `refreshToken`, `tokenType`, `expiresIn` and `user` with `phoneVerified:false`. Replace these placeholders with development-only values. Treat returned tokens as secrets: don't share terminal logs or commit token-bearing API responses.

Use the bearer `accessToken` in `Authorization: Bearer ...` to call `/api/v1/me`. Mobile must ultimately save refresh tokens in Android Keystore/iOS Keychain-backed secure storage, not ordinary SharedPreferences or logs.

## 6. Phone verification (optional live provider)

After setting Twilio env vars, start with the phone number **you control**:

```bash
curl -X POST http://localhost:8080/api/v1/me/phone/challenges \
  -H 'Authorization: Bearer ACCESS_TOKEN' -H 'Content-Type: application/json' \
  -d '{"phone":"+13145550123"}'
```

Copy `challengeId` from the response. Twilio sends SMS. Enter the actual code:

```bash
curl -X POST http://localhost:8080/api/v1/me/phone/verify \
  -H 'Authorization: Bearer ACCESS_TOKEN' -H 'Content-Type: application/json' \
  -d '{"challengeId":"CHALLENGE_UUID","code":"ACTUAL_SMS_CODE"}'
```

Account `phoneVerified` becomes true **only when you verify the same number registered with the account**. Additional numbers become verified contact numbers without silently changing the registration phone.

## 7. Aiven MySQL (later)

Create a MySQL database/user in Aiven with only needed privileges. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. Aiven enforces secure connections; configure the **JDBC driver's identity-verified TLS** and trust the Aiven CA with a Java truststore; don't use `trustServerCertificate=true`, `sslMode=DISABLED` or hardcoded secrets. Example URL shape:

```
DB_URL=jdbc:mysql://HOST:PORT/sajha?sslMode=VERIFY_IDENTITY&connectionTimeZone=UTC
```

This URL requires a correctly configured truststore for the Aiven CA. Keep Flyway migrations controlled and back up database before production migrations.

## 8. Render deployment (later)

The included multistage `Dockerfile` builds Java 21 Spring Boot and runs it with a non-root UID. On Render, create a **Docker Web Service**, set `PORT` to 10000 or let Render provide it, set DB and JWT environment variables, and use the service health-check path `/actuator/health`. Do **not** deploy publicly before real phone verification, shared rate-limiting, monitoring, password reset, and appropriate provider/auth security reviews.

Use a separate `sajha_backend` repository. Do not put `.env` or private certificates into Git.

## Security/production caveats

- Current login/register rate limiter uses an **in-process map** and is **not adequate for multiple Render replicas or persistent brute-force defense**. Move it to a shared store/gateway (Redis) plus observability and IP controls before launch.
- Phone OTP rate limiting lives in MySQL per account/phone. Provider-side protections should also be enabled.
- No email verification, forgotten-password flow, social providers, anti-spam/abuse systems, admin login, account deletion or content moderation APIs yet. These must be completed before a public production deployment.
- Bearer mobile routes disable CSRF because tokens are sent in Authorization headers, **not cookies**. The future admin portal will use a separate cookie-based chain with CSRF and MFA.
- JWT access tokens remain valid for up to 15 minutes after logout, while refresh tokens are revoked. Token/key rotation and incident response policies are future hardening work.
- The real Aiven/Render connection and a full Maven build have **not been verified in the package creation environment** (Maven/dependency network unavailable). Run `mvn clean test`, `mvn package` and the smoke script locally and share errors for targeted fixes.

## What comes next

**Phase 4B:** Connect Flutter credential authentication and phone verification; add media/listing repositories and shared DTOs. **Phase 4C:** build business submission and admin review APIs with server-side roles, MFA, CSRF, audited manual payments. **Phase 4D:** reviews and moderation.

Contract mapping: `docs/API_SCOPE.md`. See `docs/SECURITY_NOTES.md` for an implementation checklist.
