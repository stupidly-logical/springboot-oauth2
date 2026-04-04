# OAuth2 Authorization Server

A production-oriented OAuth2 Authorization Server built with **Spring Boot 3.3** and **Spring Security OAuth2 Authorization Server 1.3**. It implements the full OAuth2/OIDC specification including Authorization Code Flow (with PKCE), Client Credentials, Refresh Tokens, token introspection, and revocation — backed by JWT signing via persistent RSA key pairs and JDBC-backed state.

> **Status:** Development-ready. See [Production Checklist](#production-checklist) before deploying.

---

## Table of Contents

- [Purpose](#purpose)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Running Locally](#running-locally)
- [Endpoints](#endpoints)
- [Default Credentials](#default-credentials)
- [Testing](#testing)
- [Configuration Reference](#configuration-reference)
- [Production Checklist](#production-checklist)

---

## Purpose

This server acts as a standalone **OAuth2 Authorization Server** — the component responsible for:

- Authenticating resource owners (users)
- Issuing access tokens, refresh tokens, and ID tokens (OIDC)
- Exposing JWKS for resource servers to validate JWTs
- Managing token introspection and revocation

Resource servers and clients are configured separately and point to this server as their authority.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                   OAuth2 Authorization Server                    │
│                        (Port 9000)                               │
│                                                                  │
│  ┌──────────────────────┐   ┌──────────────────────────────┐    │
│  │  AuthorizationServer │   │     DefaultSecurityConfig    │    │
│  │       Config         │   │  (Form login, CORS, headers, │    │
│  │  - JWT signing (RSA) │   │   JDBC users, rate limiting) │    │
│  │  - Persistent keys   │   └──────────────────────────────┘    │
│  │  - OIDC support      │                                        │
│  │  - JDBC token store  │   ┌──────────────────────────────┐    │
│  └──────────────────────┘   │    RegisteredClientConfig    │    │
│                             │  (JDBC clients, PKCE, scopes,│    │
│  ┌──────────────────────┐   │   grant types, redirect URIs)│    │
│  │   ProviderConfig     │   └──────────────────────────────┘    │
│  │  - Issuer URI        │                                        │
│  │    (env-var driven)  │   ┌──────────────────────────────┐    │
│  └──────────────────────┘   │     OAuth2ErrorHandler       │    │
│                             │  (RFC 6749 error responses)  │    │
│  ┌──────────────────────┐   └──────────────────────────────┘    │
│  │  RateLimitingFilter  │                                        │
│  │  - 20 req/min per IP │                                        │
│  │    on /oauth2/token  │                                        │
│  └──────────────────────┘                                        │
└─────────────────────────────────────────────────────────────────┘
           │                              │
           ▼                              ▼
  Client Applications           Resource Servers
  (redirect to /oauth2/authorize) (validate JWT via /oauth2/jwks)
```

**Token flow:**
1. Client redirects user to `/oauth2/authorize` (with PKCE `code_challenge`)
2. User authenticates via form login
3. Server issues authorization code (Authorization Code Flow) or token directly (Client Credentials)
4. Client exchanges code for tokens at `/oauth2/token` (with PKCE `code_verifier`)
5. Resource server validates JWT using the public JWK from `/oauth2/jwks`

---

## Project Structure

```
oauth2-java/
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── AuthorizationServerApplication.java     # Entry point
│   │   │   └── config/
│   │   │       ├── AuthorizationServerConfig.java      # Core OAuth2/JWT config, JDBC token store
│   │   │       ├── DefaultSecurityConfig.java          # Security filter chain, CORS, JDBC users
│   │   │       ├── OAuth2ErrorHandler.java             # RFC 6749 error handling
│   │   │       ├── ProviderConfig.java                 # Issuer URI settings
│   │   │       ├── RateLimitingFilter.java             # Bucket4j rate limiter (token endpoint)
│   │   │       ├── RegisteredClientConfig.java         # JDBC OAuth2 client definitions
│   │   │       └── SecurityConfig.java                 # Password encoder bean
│   │   └── resources/
│   │       ├── application.properties                  # All configuration with env var overrides
│   │       └── application-dev.properties              # Dev overrides (verbose SQL logging)
│   └── test/
│       └── java/com/example/
│           └── AuthorizationServerApplicationTests.java # Context load test
├── .github/
│   └── workflows/
│       └── maven.yml                                   # CI: JDK 21, build + test
├── keys/                                               # RSA key pair (auto-generated, gitignored)
├── OAuth2-Authorization-Server-Tests.postman_collection.json
├── OAuth2-Server-Development.postman_environment.json
├── OAuth2-Server-Production.postman_environment.json
├── POSTMAN_TESTING_GUIDE.md
└── pom.xml
```

### Key classes

| Class | Responsibility |
|---|---|
| `AuthorizationServerConfig` | OAuth2 authorization server filter chain, persistent RSA JWT signing, JWK set, OIDC, JDBC authorization/consent stores, custom JWT claims |
| `DefaultSecurityConfig` | Default HTTP security (form login, CORS, security headers, rate limiting, JDBC-backed users) |
| `RegisteredClientConfig` | JDBC-backed OAuth2 client registry; seeds `test-client` with PKCE enabled on startup |
| `ProviderConfig` | Sets the `issuer` URI used in server metadata and tokens (env-var driven) |
| `OAuth2ErrorHandler` | Maps OAuth2 and Spring Security exceptions to RFC 6749-compliant JSON error responses |
| `RateLimitingFilter` | Bucket4j rate limiter: 20 requests/minute per IP on `POST /oauth2/token` |
| `SecurityConfig` | Provides the `BCryptPasswordEncoder` bean |

---

## Prerequisites

- Java 21+
- Maven 3.6+

---

## Running Locally

```bash
# Clone and enter the project
git clone <repo-url>
cd oauth2-java

# Run with Maven
mvn spring-boot:run

# Or build and run the JAR
mvn clean package
java -jar target/*.jar

# Run in dev mode (enables verbose SQL logging)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The server starts at **http://localhost:9000**.

Check it is up:
```bash
curl http://localhost:9000/actuator/health
# {"status":"UP"}

curl http://localhost:9000/.well-known/oauth-authorization-server
# Returns server metadata JSON
```

RSA keys are auto-generated on first startup and saved to `keys/jwt.private.pem` / `keys/jwt.public.pem`. Subsequent restarts reuse the same key pair, so existing tokens remain valid.

---

## Endpoints

### OAuth2 / OIDC

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/oauth2/authorize` | Authorization endpoint (redirect users here) |
| `POST` | `/oauth2/token` | Token endpoint (exchange code or client credentials) |
| `POST` | `/oauth2/introspect` | Token introspection (RFC 7662) |
| `POST` | `/oauth2/revoke` | Token revocation (RFC 7009) |
| `GET` | `/oauth2/jwks` | JWK Set — public keys for JWT validation |
| `GET` | `/.well-known/oauth-authorization-server` | Server metadata (RFC 8414) |
| `GET` | `/.well-known/openid-configuration` | OIDC discovery |

### Management

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/actuator/health` | Health check |
| `GET` | `/actuator/info` | Application info |
| `GET` | `/actuator/metrics` | Metrics |
| `GET` | `/h2-console` | H2 DB console **(dev only — disable in production)** |

---

## Default Credentials

> These are for local development only. Never use in production.

**OAuth2 Client:**

| Setting | Value |
|---------|-------|
| Client ID | `test-client` |
| Client Secret | `secret` (override with `OAUTH2_CLIENT_SECRET`) |
| Grant types | `authorization_code`, `refresh_token`, `client_credentials` |
| Scopes | `openid`, `profile`, `read`, `write` |
| PKCE | Required for Authorization Code Flow |
| Redirect URIs | `http://localhost:9000/login/oauth2/code/test-client`, `http://localhost:9000/authorized` |

To add a redirect URI without changing code, set `OAUTH2_EXTRA_REDIRECT_URI=https://your-app.example.com/callback`.

**Test Users:**

| Username | Password | Roles |
|----------|----------|-------|
| `user` | `password` | USER |
| `admin` | `admin` | USER, ADMIN |

---

## Testing

### Quick test — Client Credentials Flow

```bash
curl -s -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "test-client:secret" \
  -d "grant_type=client_credentials&scope=read" | jq .
```

### Quick test — Authorization Code Flow

Authorization Code Flow requires **PKCE**. Use a PKCE-capable tool (Postman, a browser-based OAuth2 playground, or a client library) for end-to-end testing.

1. Generate a code verifier and code challenge (base64url(sha256(verifier)))
2. Open in browser:
   ```
   http://localhost:9000/oauth2/authorize
     ?response_type=code
     &client_id=test-client
     &redirect_uri=http://localhost:9000/authorized
     &scope=openid+read
     &code_challenge=<YOUR_CODE_CHALLENGE>
     &code_challenge_method=S256
   ```
3. Log in as `user` / `password`
4. Exchange the code:
   ```bash
   curl -s -X POST http://localhost:9000/oauth2/token \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -u "test-client:secret" \
     -d "grant_type=authorization_code&code=<CODE>&redirect_uri=http://localhost:9000/authorized&code_verifier=<YOUR_CODE_VERIFIER>" | jq .
   ```

### Postman

Import `OAuth2-Authorization-Server-Tests.postman_collection.json` and `OAuth2-Server-Development.postman_environment.json`. See [POSTMAN_TESTING_GUIDE.md](POSTMAN_TESTING_GUIDE.md) for full usage.

---

## Configuration Reference

All settings in `application.properties` can be overridden with environment variables:

| Environment Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `9000` | Server port |
| `SERVER_CONTEXT_PATH` | `/` | Servlet context path |
| `OAUTH2_ISSUER_URI` | `http://localhost:9000` | Token issuer URI (use `https://` in production) |
| `OAUTH2_CLIENT_SECRET` | `secret` | OAuth2 client secret for `test-client` |
| `OAUTH2_EXTRA_REDIRECT_URI` | *(empty)* | Additional allowed redirect URI for `test-client` |
| `JWT_KEY_PATH` | `keys/jwt` | Path prefix for RSA key files (`.private.pem` / `.public.pem` appended) |
| `CORS_ALLOWED_ORIGINS` | *(empty)* | Comma-separated additional CORS origin patterns (e.g. `https://*.example.com`) |
| `DATABASE_URL` | `jdbc:h2:mem:authdb` | JDBC URL |
| `DATABASE_DRIVER` | `org.h2.Driver` | JDBC driver class |
| `DATABASE_USERNAME` | `sa` | DB username |
| `DATABASE_PASSWORD` | *(empty)* | DB password |
| `DB_MAX_POOL_SIZE` | `10` | HikariCP max pool size |
| `DB_MIN_IDLE` | `5` | HikariCP min idle connections |
| `DB_CONNECTION_TIMEOUT` | `20000` | HikariCP connection timeout (ms) |
| `H2_CONSOLE_ENABLED` | `true` | Enable H2 web console (set `false` in production) |
| `LOGGING_LEVEL_ROOT` | `INFO` | Root log level |
| `ACTUATOR_ENDPOINTS` | `health,info,metrics` | Exposed actuator endpoints |
| `ACTUATOR_HEALTH_SHOW_DETAILS` | `when-authorized` | Health details visibility |

---

## Production Checklist

- [x] **Persistent JWT keys** — RSA key pair saved to `keys/jwt.{private,public}.pem`; reloaded on restart.
- [x] **Persist OAuth2 state** — Authorization codes, tokens, and consents use `JdbcOAuth2AuthorizationService`.
- [x] **Persist registered clients** — `JdbcRegisteredClientRepository` backed by the configured datasource.
- [x] **JDBC user store** — `JdbcUserDetailsManager`; replace seed users with your own provisioning.
- [x] **Env-var secrets** — Client secret driven by `OAUTH2_CLIENT_SECRET`; no hardcoded secrets.
- [x] **PKCE enforced** — `requireProofKey(true)` set on `test-client`; required for Authorization Code Flow.
- [x] **Rate limiting** — 20 requests/minute per IP on `POST /oauth2/token` via Bucket4j.
- [x] **Security headers** — HSTS, Content-Security-Policy, X-Content-Type-Options, Referrer-Policy.
- [x] **Updated dependencies** — Spring Boot 3.3.6, Spring Authorization Server 1.3.3, Java 21.
- [x] **CI aligned** — GitHub Actions workflow uses JDK 21 matching `pom.xml`.
- [x] **Basic test coverage** — Spring context load test in CI.
- [ ] **Switch to PostgreSQL** — Set `DATABASE_URL`, `DATABASE_DRIVER`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`. Update `JPA_DATABASE_PLATFORM` to `org.hibernate.dialect.PostgreSQLDialect`.
- [ ] **Disable H2 console** — Set `H2_CONSOLE_ENABLED=false` (or unset; default is `true`).
- [ ] **HTTPS only** — Enforce TLS termination; set `OAUTH2_ISSUER_URI` to your `https://` domain.
- [ ] **Restrict CORS** — Set `CORS_ALLOWED_ORIGINS` to your specific production origins; the `localhost:*` wildcard is always included as a fallback.
- [ ] **Restrict actuator** — Secure `/actuator/**` behind authentication or expose only on a management port.
- [ ] **Key storage** — Move RSA keys from filesystem to a secrets manager (Vault, AWS Secrets Manager, etc.) for cloud deployments.

---

## License

MIT
