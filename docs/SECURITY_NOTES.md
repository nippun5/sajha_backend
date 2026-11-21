# Security gates before external production use

- [x] Signup never grants admin privileges; server owns roles.
- [x] BCrypt password hashing; no plaintext password persistence.
- [x] JWT secret required at process startup and must have >=32 decoded bytes.
- [x] Short-lived JWT access tokens; random refresh tokens hashed in DB, rotation and replay-family revocation.
- [x] Phone account remains unverified until a real provider approves the code; missing SMS secrets fail closed.
- [x] Default deny for all unimplemented/admin routes.
- [x] Login/register per-instance throttles and database OTP throttles.
- [ ] Build/test against Maven dependency graph on developer machine; run actual MySQL smoke test.
- [ ] Shared rate limiting across replicas and IP-level gateway policy.
- [ ] Validate HTTPS/CA/truststore against Aiven and Render in staging.
- [ ] Integrate Google, Facebook, Apple with server-side token validation and provider account linking.
- [ ] Forgot/reset password with account-enumeration protection and single-use reset tokens.
- [ ] Admin session authentication, MFA, CSRF, lockout and audit events.
- [ ] Secure phone verification provider fraud defenses, abuse alerts and quotas.
- [ ] Verified-contact enforcement for every created product/room/job/business listing.
- [ ] Environment-separated secrets, rotation, secret scanning and mobile secure storage.
- [ ] Audit authorization across all service methods and persistent revocation of admin access.
- [ ] Review privacy/deletion retention, monitoring, backups, observability and vulnerability scans.

Not ready for public production until remaining gates are closed.
