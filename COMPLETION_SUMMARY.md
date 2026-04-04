# OAuth2 Authorization Server — Change History

This document summarises the changes made across the project's development history, from initial scaffolding through production hardening.

---

## Phase 1 — Initial Implementation

The initial commit provided a Spring Boot skeleton with stub configuration classes. The following work completed the core OAuth2 server:

| File | Change |
|---|---|
| `RegisteredClientConfig.java` | Implemented `test-client` with auth code, client credentials, refresh token, OIDC scopes |
| `AuthorizationServerConfig.java` | Added OAuth2 security filter chain, RSA JWT signing, JWK Set endpoint, OIDC, token customizer |
| `OAuth2ErrorHandler.java` | Created RFC 6749-compliant global exception handler |
| `DefaultSecurityConfig.java` | Added form login, CORS, in-memory user details |
| `SecurityConfig.java` | Removed duplicate bean definitions; kept `BCryptPasswordEncoder` |
| `application.properties` | Added comprehensive env-var-driven configuration |

---

## Phase 2 — Production Hardening (PRs #2–#7)

Six pull requests brought the server to a production-ready baseline:

### PR #2 — Documentation
- Rewrote README with architecture diagram, endpoint table, env var reference, production checklist

### PR #6 — CI and Dependencies
- GitHub Actions workflow added (`maven.yml`), aligned to **JDK 21**
- Spring Boot upgraded `3.1.0` → **3.3.6** (EOL → supported)
- Spring Authorization Server upgraded `1.1.2` → **1.3.3**
- `maven-compiler-plugin` source/target updated to 21
- `spring-security-test` given explicit version (`6.3.4`) — no parent BOM present
- Added `AuthorizationServerApplicationTests.java` (context load test)

### PR #5 — Observability and Error Handling
- `OAuth2ErrorHandler`: stack traces removed from HTTP responses; only logged server-side
- Profile-aware development mode detection using `Environment.acceptsProfiles()` instead of `System.getProperty`
- `application.properties`: SQL logging defaults changed from `DEBUG`/`TRACE` → **`WARN`**
- `application-dev.properties` (new): re-enables verbose SQL logging when `spring.profiles.active=dev`

### PR #3 — Security Critical
- `AuthorizationServerConfig`: RSA key pair now **persisted to PEM files** (`keys/jwt.{private,public}.pem`); reloaded on restart
- `RegisteredClientConfig`: client secret moved to `OAUTH2_CLIENT_SECRET` env var; **PKCE enforced** (`requireProofKey(true)`); Postman redirect URI removed; additional URI configurable via `OAUTH2_EXTRA_REDIRECT_URI`
- `DefaultSecurityConfig`: CORS origins driven by `CORS_ALLOWED_ORIGINS` env var
- `application.properties`: added `jwt.key-path`, `oauth2.client.secret`, `oauth2.extra-redirect-uri`, `cors.allowed-origins`

### PR #4 — JDBC Persistence
- Replaced all in-memory stores:
  - `JdbcRegisteredClientRepository` (clients)
  - `JdbcOAuth2AuthorizationService` (tokens, auth codes)
  - `JdbcOAuth2AuthorizationConsentService` (consents)
  - `JdbcUserDetailsManager` (users)
- `spring.sql.init` schema initialization enabled; `spring.jpa.defer-datasource-initialization=true` ensures schemas exist before Hibernate
- All test-data seeding moved to `ApplicationRunner` beans (executes after full context init, avoiding schema-not-ready errors)

### PR #7 — Hardening
- `RateLimitingFilter` (new): Bucket4j-backed, 20 requests/minute per IP on `POST /oauth2/token`; returns HTTP 429 on exhaustion
- `DefaultSecurityConfig`: added HSTS, Content-Security-Policy, X-Content-Type-Options, Referrer-Policy headers; wired `RateLimitingFilter` before `UsernamePasswordAuthenticationFilter`
- `ProviderConfig`: issuer URI driven by `OAUTH2_ISSUER_URI` env var
- `application.properties`: added `oauth2.issuer-uri`

---

## Current State

| Area | Status |
|---|---|
| Spring Boot version | 3.3.6 |
| Java version | 21 |
| JWT key persistence | PEM files (auto-generated, reloaded on restart) |
| Client/token/consent storage | JDBC (H2 in dev, configurable) |
| User storage | JDBC (`JdbcUserDetailsManager`) |
| PKCE | Enforced on `test-client` |
| Rate limiting | 20 req/min per IP on `/oauth2/token` |
| Security headers | HSTS, CSP, X-Content-Type-Options, Referrer-Policy |
| Secrets | All via environment variables |
| CI | GitHub Actions, JDK 21, Maven build + test |

## Remaining Before Production

- Switch datasource to PostgreSQL
- Disable H2 console (`H2_CONSOLE_ENABLED=false`)
- Set `OAUTH2_ISSUER_URI` to an `https://` domain
- Set `CORS_ALLOWED_ORIGINS` to specific production origins
- Move RSA keys to a secrets manager
- Secure or restrict the `/actuator` endpoints
