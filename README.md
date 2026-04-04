# OAuth2 Authorization Server

A production-oriented OAuth2 Authorization Server built with **Spring Boot 3** and **Spring Security OAuth2 Authorization Server 1.1**. It implements the full OAuth2/OIDC specification including Authorization Code Flow, Client Credentials, Refresh Tokens, token introspection, and revocation — backed by JWT signing via RSA key pairs.

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
│  │       Config         │   │  (Form login, CORS, users)   │    │
│  │  - JWT signing (RSA) │   └──────────────────────────────┘    │
│  │  - OIDC support      │                                        │
│  │  - Custom claims     │   ┌──────────────────────────────┐    │
│  └──────────────────────┘   │    RegisteredClientConfig    │    │
│                             │  (Clients, scopes, grant     │    │
│  ┌──────────────────────┐   │   types, redirect URIs)      │    │
│  │   ProviderConfig     │   └──────────────────────────────┘    │
│  │  - Issuer URI        │                                        │
│  │    (localhost:9000)  │   ┌──────────────────────────────┐    │
│  └──────────────────────┘   │     OAuth2ErrorHandler       │    │
│                             │  (RFC 6749 error responses)  │    │
│                             └──────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
           │                              │
           ▼                              ▼
  Client Applications           Resource Servers
  (redirect to /oauth2/authorize) (validate JWT via /oauth2/jwks)
```

**Token flow:**
1. Client redirects user to `/oauth2/authorize`
2. User authenticates via form login
3. Server issues authorization code (Authorization Code Flow) or token directly (Client Credentials)
4. Client exchanges code for tokens at `/oauth2/token`
5. Resource server validates JWT using the public JWK from `/oauth2/jwks`

---

## Project Structure

```
oauth2-java/
├── src/
│   └── main/
│       ├── java/com/example/
│       │   ├── AuthorizationServerApplication.java     # Entry point
│       │   └── config/
│       │       ├── AuthorizationServerConfig.java      # Core OAuth2/JWT config
│       │       ├── DefaultSecurityConfig.java          # Security filter chain, CORS, users
│       │       ├── OAuth2ErrorHandler.java             # RFC 6749 error handling
│       │       ├── ProviderConfig.java                 # Issuer URI settings
│       │       ├── RegisteredClientConfig.java         # OAuth2 client definitions
│       │       └── SecurityConfig.java                 # Password encoder bean
│       └── resources/
│           └── application.properties                  # All configuration with env var overrides
├── .github/
│   └── workflows/
│       └── maven.yml                                   # CI: build + dependency graph
├── OAuth2-Authorization-Server-Tests.postman_collection.json
├── OAuth2-Server-Development.postman_environment.json
├── OAuth2-Server-Production.postman_environment.json
├── POSTMAN_TESTING_GUIDE.md
└── pom.xml
```

### Key classes

| Class | Responsibility |
|---|---|
| `AuthorizationServerConfig` | Configures the OAuth2 authorization server security filter chain, RSA JWT signing, JWK set, OIDC, and custom JWT claims |
| `DefaultSecurityConfig` | Default HTTP security (form login, CORS, permit-list for public endpoints, in-memory users) |
| `RegisteredClientConfig` | Defines allowed OAuth2 clients: grant types, scopes, redirect URIs, token TTLs |
| `ProviderConfig` | Sets the `issuer` URI used in server metadata and tokens |
| `OAuth2ErrorHandler` | Maps OAuth2 and Spring Security exceptions to RFC 6749-compliant JSON error responses |
| `SecurityConfig` | Provides the `BCryptPasswordEncoder` bean |

---

## Prerequisites

- Java 17+
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
```

The server starts at **http://localhost:9000**.

Check it is up:
```bash
curl http://localhost:9000/actuator/health
# {"status":"UP"}

curl http://localhost:9000/.well-known/oauth-authorization-server
# Returns server metadata JSON
```

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
| `GET` | `/h2-console` | H2 DB console **(dev only)** |

---

## Default Credentials

> These are for local development only. Never use in production.

**OAuth2 Client:**

| Setting | Value |
|---------|-------|
| Client ID | `test-client` |
| Client Secret | `secret` |
| Grant types | `authorization_code`, `refresh_token`, `client_credentials` |
| Scopes | `openid`, `profile`, `read`, `write` |
| Redirect URIs | `http://localhost:9000/login/oauth2/code/test-client`, `http://localhost:9000/authorized`, `https://oauth.pstmn.io/v1/callback` |

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

### Quick test — Authorization Code Flow (manual)

1. Open in browser: `http://localhost:9000/oauth2/authorize?response_type=code&client_id=test-client&redirect_uri=http://localhost:9000/authorized&scope=openid+read`
2. Log in as `user` / `password`
3. Copy the `code` query parameter from the redirect
4. Exchange it:
```bash
curl -s -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "test-client:secret" \
  -d "grant_type=authorization_code&code=<CODE>&redirect_uri=http://localhost:9000/authorized" | jq .
```

### Postman

Import `OAuth2-Authorization-Server-Tests.postman_collection.json` and `OAuth2-Server-Development.postman_environment.json`. See [POSTMAN_TESTING_GUIDE.md](POSTMAN_TESTING_GUIDE.md) for full usage.

---

## Configuration Reference

All settings in `application.properties` can be overridden with environment variables:

| Environment Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `9000` | Server port |
| `OAUTH2_CLIENT_ID` | `test-client` | OAuth2 client ID |
| `OAUTH2_CLIENT_SECRET` | *(bcrypt of "secret")* | OAuth2 client secret (BCrypt encoded) |
| `OAUTH2_ISSUER_URI` | `http://localhost:9000` | Token issuer URI |
| `DATABASE_URL` | H2 in-memory | JDBC URL |
| `DATABASE_USERNAME` | `sa` | DB username |
| `DATABASE_PASSWORD` | *(empty)* | DB password |
| `HIKARI_MAX_POOL_SIZE` | `10` | Max DB connection pool size |
| `HIKARI_MIN_IDLE` | `5` | Min idle DB connections |

---

## Production Checklist

The following **must** be addressed before production deployment:

- [ ] **Persistent JWT keys** — RSA key pair is currently generated in-memory on every startup, invalidating all existing tokens on restart. Load from a keystore or secrets manager.
- [ ] **Persist OAuth2 state** — Authorization codes, tokens, and consents are in-memory. Use `JdbcOAuth2AuthorizationService` with a real database.
- [ ] **Persist registered clients** — Use `JdbcRegisteredClientRepository` instead of in-memory.
- [ ] **Replace test users** — Wire a real `UserDetailsService` backed by a database or LDAP.
- [ ] **Rotate secrets** — Change client secret, use environment variables; never commit secrets.
- [ ] **Enable PKCE** — Set `requireProofKey(true)` for public clients.
- [ ] **Switch to PostgreSQL** — Configure `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`.
- [ ] **Disable H2 console** — Set `spring.h2.console.enabled=false`.
- [ ] **HTTPS only** — Enforce TLS; set `OAUTH2_ISSUER_URI` to your `https://` domain.
- [ ] **Restrict CORS** — Replace `localhost:*` wildcard with specific production origins.
- [ ] **Add rate limiting** — Protect `/oauth2/token` and `/oauth2/authorize` from brute force.
- [ ] **Security headers** — Add HSTS, Content-Security-Policy, X-Content-Type-Options.
- [ ] **Add tests** — No unit or integration tests exist yet.
- [ ] **Update dependencies** — Spring Boot 3.1.0 is outdated; upgrade to 3.3.x or later.
- [ ] **Fix CI JDK version** — Workflow uses JDK 17 but `pom.xml` targets Java 21.

---

## License

MIT
