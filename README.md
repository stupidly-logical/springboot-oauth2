# OAuth2 Authorization Server

A Spring Boot OAuth2 Authorization Server implementation with Spring Security OAuth2 Authorization Server.

## Features

- ✅ OAuth2 Authorization Code Flow
- ✅ Client Credentials Flow  
- ✅ Refresh Token Support
- ✅ OpenID Connect (OIDC) Support
- ✅ Token Introspection
- ✅ Token Revocation
- ✅ JWK Set Endpoint
- ✅ H2 Database for Development
- ✅ BCrypt Password Encoding
- ✅ Actuator Health Checks

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+

### Running the Application

1. Clone the repository
2. Navigate to the project directory
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```
4. The server will start on `http://localhost:9000`

### Default Configuration

**OAuth2 Client:**
- Client ID: `test-client`
- Client Secret: `secret` (BCrypt encoded)
- Redirect URIs: 
  - `http://localhost:9000/login/oauth2/code/test-client`
  - `http://localhost:9000/authorized`
  - `https://oauth.pstmn.io/v1/callback` (for Postman)

**Test Users:**
- Username: `user`, Password: `password` (Role: USER)
- Username: `admin`, Password: `admin` (Role: USER, ADMIN)

## Available Endpoints

### OAuth2 Endpoints
- **Authorization**: `GET /oauth2/authorize`
- **Token**: `POST /oauth2/token`
- **Token Introspection**: `POST /oauth2/introspect`
- **Token Revocation**: `POST /oauth2/revoke`
- **JWK Set**: `GET /oauth2/jwks`
- **Server Metadata**: `GET /.well-known/oauth-authorization-server`

### Management Endpoints
- **Health Check**: `GET /actuator/health`
- **Application Info**: `GET /actuator/info`
- **H2 Database Console**: `GET /h2-console` (dev only)

## Testing with Postman

Import the provided Postman collection: `OAuth2-Authorization-Server.postman_collection.json`

### Quick Test - Client Credentials Flow

```bash
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "test-client:secret" \
  -d "grant_type=client_credentials&scope=read"
```

## Security Considerations

### Development vs Production

**Current Configuration (Development):**
- H2 in-memory database
- H2 console enabled
- Debug logging enabled
- In-memory user store

**For Production:**
- Use persistent database (PostgreSQL, MySQL)
- Disable H2 console
- Use environment variables for secrets
- Implement proper user management
- Enable PKCE for public clients
- Use HTTPS only
- Configure proper CORS policies

### Environment Variables (Production)

```bash
export OAUTH2_CLIENT_SECRET=your-secure-secret
export JWT_SIGNING_KEY=your-jwt-signing-key
export DATABASE_URL=your-database-url
export DATABASE_USERNAME=your-db-username
export DATABASE_PASSWORD=your-db-password
```

## Development

### Project Structure
```
src/
├── main/
│   ├── java/com/example/
│   │   ├── AuthorizationServerApplication.java
│   │   └── config/
│   │       ├── AuthorizationServerConfig.java
│   │       ├── DefaultSecurityConfig.java
│   │       ├── ProviderConfig.java
│   │       └── SecurityConfig.java
│   └── resources/
│       └── application.properties
```

### Building
```bash
mvn clean package
```

### Running Tests
```bash
mvn test
```

## Issues Fixed

This implementation fixes several common OAuth2 server issues:

1. ✅ **Duplicate Dependencies**: Removed duplicate OAuth2 authorization server dependencies
2. ✅ **Missing Plugin Version**: Added proper Maven plugin versions
3. ✅ **Insecure Client Secret**: Replaced `{noop}` with BCrypt encoding
4. ✅ **Port Mismatch**: Fixed redirect URI port configuration
5. ✅ **Missing User Authentication**: Added proper user details service
6. ✅ **Production Readiness**: Added comprehensive configuration options

## License

This project is licensed under the MIT License.
