# OAuth2 Authorization Server - Postman Testing Guide

## Overview
This directory contains comprehensive Postman collections and environments for testing the OAuth2 Authorization Server. The collection uses advanced Postman features including variable abstraction, automatic token extraction, and complete OAuth2 flow testing.

## Files Included

### 1. Collection File
- **`OAuth2-Authorization-Server-Tests.postman_collection.json`** - Main test collection

### 2. Environment Files
- **`OAuth2-Server-Development.postman_environment.json`** - Development environment
- **`OAuth2-Server-Production.postman_environment.json`** - Production environment template

## Quick Start

### 1. Import Collection and Environment
1. Open Postman
2. Click **Import** → **Upload Files**
3. Import the collection file: `OAuth2-Authorization-Server-Tests.postman_collection.json`
4. Import the environment file: `OAuth2-Server-Development.postman_environment.json`
5. Select the imported environment in the top-right dropdown

### 2. Start Your OAuth2 Server
```bash
cd /path/to/oauth2-java
mvn spring-boot:run
```

### 3. Run Tests
- **Full Collection**: Click **Run** button in collection
- **Individual Requests**: Click on any request and hit **Send**
- **Folder Tests**: Right-click on folders to run specific test groups

## Collection Structure

### 📁 1. Server Discovery
Tests server metadata and endpoint discovery:
- **OAuth2 Server Metadata** - Discovers server capabilities
- **OIDC Configuration** - Gets OpenID Connect configuration
- **JWK Set** - Retrieves JSON Web Keys for token verification

### 📁 2. Client Credentials Flow
Tests machine-to-machine authentication:
- **Get Access Token** - Requests token using client credentials
- **Introspect Token** - Validates token metadata and status

### 📁 3. Authorization Code Flow
Tests user authorization flow (requires manual browser interaction):
- **Authorization Request** - Initiates user authorization
- **Exchange Code for Token** - Exchanges auth code for tokens
- **Refresh Access Token** - Uses refresh token to get new access token

### 📁 4. Token Management
Tests token lifecycle management:
- **Revoke Access Token** - Revokes an active access token
- **Revoke Refresh Token** - Revokes a refresh token
- **Verify Token After Revocation** - Confirms token is inactive

### 📁 5. Error Scenarios
Tests error handling and validation:
- **Invalid Client Credentials** - Tests authentication errors
- **Invalid Grant Type** - Tests unsupported grant type error
- **Invalid Scope** - Tests scope validation error

### 📁 6. Health & Monitoring
Tests operational endpoints:
- **Health Check** - Verifies server health status
- **Application Info** - Gets application metadata
- **Metrics** - Retrieves application metrics

## Environment Variables

### Core Configuration
| Variable | Description | Default Value |
|----------|-------------|---------------|
| `base_url` | OAuth2 server base URL | `http://localhost:9000` |
| `client_id` | OAuth2 client identifier | `test-client` |
| `client_secret` | OAuth2 client secret | `secret` |
| `redirect_uri` | OAuth2 redirect URI | `https://oauth.pstmn.io/v1/callback` |

### Scope Configuration
| Variable | Description | Default Value |
|----------|-------------|---------------|
| `default_scope` | Scopes for client credentials | `read write` |
| `authorization_scope` | Scopes for authorization code | `openid profile read write` |

### Auto-Managed Variables
These are automatically set by test scripts:
- `client_credentials_access_token` - Token from client credentials flow
- `authorization_access_token` - Token from authorization code flow
- `refresh_token` - Refresh token from auth flow
- `id_token` - ID token from OIDC flow
- `authorization_endpoint` - Auto-discovered authorization endpoint
- `token_endpoint` - Auto-discovered token endpoint
- `jwks_uri` - Auto-discovered JWK Set URI

## Advanced Features

### 🔄 Automatic Token Management
- **Token Extraction**: Automatically extracts and stores tokens from responses
- **Token Reuse**: Automatically uses stored tokens in subsequent requests
- **Expiry Tracking**: Tracks token expiration times

### 🧪 Comprehensive Testing
- **Status Code Validation**: Verifies expected HTTP status codes
- **Response Structure Testing**: Validates JSON response structure
- **Error Handling**: Tests various error scenarios
- **Performance Testing**: Checks response times

### 🌐 Environment Abstraction
- **Multi-Environment Support**: Separate configs for dev/prod
- **Variable Substitution**: Uses variables throughout requests
- **Secret Management**: Properly handles sensitive data

### 📊 Automatic Discovery
- **Endpoint Discovery**: Automatically discovers OAuth2 endpoints
- **Metadata Extraction**: Extracts server capabilities
- **Dynamic Configuration**: Adapts to server configuration

## Testing Workflows

### 🚀 Full Automated Test (Recommended)
1. Start your OAuth2 server
2. Run the entire collection:
   - Right-click collection → **Run collection**
   - Select environment and click **Run**
3. Review test results in the runner

### 🔧 Manual Testing Workflow

#### Client Credentials Flow
1. **Server Discovery** → Run "OAuth2 Server Metadata"
2. **Client Credentials** → Run "Get Access Token (Client Credentials)"
3. **Token Validation** → Run "Introspect Token (Client Credentials)"

#### Authorization Code Flow (Manual Steps Required)
1. **Authorization Request**:
   - Copy the generated URL from "Authorization Request (Browser)"
   - Open URL in browser
   - Login with credentials (user/password)
   - Copy authorization code from redirect URL
   - Set `authorization_code` environment variable
2. **Token Exchange** → Run "Exchange Code for Token"
3. **Token Refresh** → Run "Refresh Access Token"

### 🔍 Error Testing
Run requests in the "Error Scenarios" folder to test:
- Invalid client authentication
- Unsupported grant types
- Invalid scope requests

## Troubleshooting

### Common Issues

#### ❌ "Connection refused" errors
- **Cause**: OAuth2 server is not running
- **Solution**: Start server with `mvn spring-boot:run`

#### ❌ "Invalid client" errors
- **Cause**: Wrong client credentials in environment
- **Solution**: Verify `client_id` and `client_secret` variables

#### ❌ "Invalid redirect URI" errors
- **Cause**: Redirect URI mismatch
- **Solution**: Ensure `redirect_uri` matches server configuration

#### ❌ Authorization code flow fails
- **Cause**: Manual step not completed or expired code
- **Solution**: Complete browser authorization and quickly use the code

### Debug Tips
1. **Check Environment**: Ensure correct environment is selected
2. **View Variables**: Check current variable values in environment tab
3. **Console Logs**: Open Postman console for detailed request/response logs
4. **Server Logs**: Check OAuth2 server logs for detailed error information

## Production Usage

### Setting Up Production Environment
1. Import `OAuth2-Server-Production.postman_environment.json`
2. Update these variables:
   - `base_url` → Your production server URL
   - `client_id` → Your production client ID
   - `client_secret` → Your production client secret
   - `redirect_uri` → Your production redirect URI
3. Adjust scopes as needed for production security

### Security Considerations
- **Never commit production secrets** to version control
- **Use Postman Vault** for sensitive production variables
- **Limit scopes** in production environment
- **Use HTTPS** for all production endpoints

## Collection Customization

### Adding New Requests
1. Duplicate existing request as template
2. Update URL, method, and parameters
3. Add appropriate tests in **Tests** tab
4. Use environment variables for configuration

### Modifying Tests
Edit the **Tests** tab JavaScript code:
```javascript
pm.test("Your test name", function () {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('expected_field');
});
```

### Custom Variables
Add new variables in environment files:
```json
{
    "key": "your_variable",
    "value": "your_value",
    "type": "default",
    "enabled": true
}
```

## Integration with CI/CD

### Newman (Postman CLI)
Install Newman for command-line testing:
```bash
npm install -g newman

# Run collection
newman run OAuth2-Authorization-Server-Tests.postman_collection.json \
  -e OAuth2-Server-Development.postman_environment.json \
  --reporters cli,json \
  --reporter-json-export results.json
```

### GitHub Actions Example
```yaml
- name: Run OAuth2 Tests
  run: |
    newman run OAuth2-Authorization-Server-Tests.postman_collection.json \
      -e OAuth2-Server-Development.postman_environment.json \
      --reporters junit \
      --reporter-junit-export oauth2-test-results.xml
```

## Support

### Documentation
- [Postman Documentation](https://learning.postman.com/)
- [OAuth2 RFC](https://tools.ietf.org/html/rfc6749)
- [OpenID Connect Specification](https://openid.net/connect/)

### Issues
If you encounter issues with the collection:
1. Check server logs for errors
2. Verify environment variable configuration
3. Test individual requests before running full collection
4. Consult OAuth2 server documentation

---

## Collection Features Summary

✅ **Complete OAuth2 Flow Testing**  
✅ **Automatic Token Extraction & Reuse**  
✅ **Environment Variable Abstraction**  
✅ **Error Scenario Testing**  
✅ **OIDC Support**  
✅ **Health & Monitoring Tests**  
✅ **Production-Ready Configuration**  
✅ **CI/CD Integration Support**  

This collection provides comprehensive testing coverage for your OAuth2 Authorization Server with minimal manual configuration required!