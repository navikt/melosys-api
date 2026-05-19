# Security Debugging Guide

## Common Issues

### 1. Token Validation Failed

**Symptom**: `401 Unauthorized` or `JwtTokenValidatorException`

**Cause**: Invalid token, wrong audience, or expired token

**Debug**:
```bash
# Decode token (jwt.io or similar)
echo $TOKEN | cut -d. -f2 | base64 -d | jq

# Check claims
{
  "aud": "should-match-AZURE_APP_CLIENT_ID",
  "NAVident": "Z123456",
  "exp": 1703318400  // Check not expired
}
```

### 2. Access Denied

**Symptom**: `ManglerTilgangException`

**Cause**: Tilgangsmaskinen denied access

**Debug**:
```kotlin
// Check what's being evaluated
log.info("Checking access for fnr=$fnr, user=${SubjectHandler.getSaksbehandlerIdent()}")

// Verify cache
@Cacheable key = "fnr_userId"
```

### 3. OAuth2 Client Error

**Symptom**: `OAuth2ClientException` or `401` on service call

**Cause**: Wrong client config or expired credentials

**Debug**:
```yaml
# Check config
no.nav.security.jwt.client.registration.<client-name>:
  client-id: ${AZURE_APP_CLIENT_ID}  # Verify env var set
  scope: api://.../.default  # Verify scope correct
```

### 4. No User Context

**Symptom**: `SubjectHandler.getUserID()` returns null

**Cause**: Not in HTTP request context or system job

**Solution**:
```kotlin
// Use fallback
val user = SubjectHandler.getUserIDOrSystemUser()

// Or check context
if (SubjectHandler.getInstance().getUserID() == null) {
    // Running as system, not user
}
```

### 5. Mock OAuth Server Issues

**Symptom**: Tests fail with auth errors

**Cause**: Mock server not configured

**Solution**:
```kotlin
@EnableMockOAuth2Server
abstract class ComponentTestBase

// Issue test token
val token = mockOAuth2Server.issueToken(
    issuerId = "aad",
    subject = "Z123456"
)
```

## Log Patterns

### Successful Authentication

```
TokenValidationContextHolder: Token validated successfully for issuer aad
```

### Token Validation Error

```
JwtTokenValidatorException: Token validation failed: Invalid audience
```

### Access Control

```
TilgangsmaskinenService: Sjekker tilgang til fnr via Tilgangsmaskinen
TilgangsmaskinenConsumer: Access check result: true/false
```

### Audit Log

```
CEF:0|melosys|Auditlog|1.0|audit:read|Medlemskap og lovvalg|INFO|
suid=Z123456 duid=12345678901 end=1703318400000
```

## Environment Variables

### Azure AD

```bash
AZURE_APP_CLIENT_ID      # Application client ID
AZURE_APP_CLIENT_SECRET  # Application secret
AZURE_APP_WELL_KNOWN_URL # OIDC discovery URL
AZURE_OPENID_CONFIG_TOKEN_ENDPOINT  # Token endpoint
```

### NAIS Cluster

```bash
NAIS_CLUSTER_NAME     # dev-fss, prod-fss
NAIS_CLUSTER_NAME_GCP # dev-gcp, prod-gcp
```

## Testing Security

### Integration Test with Auth

```kotlin
@EnableMockOAuth2Server
class MyIT : ComponentTestBase() {

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Test
    fun `should require authentication`() {
        val token = mockOAuth2Server.issueToken(
            issuerId = "aad",
            subject = "Z123456",
            claims = mapOf(
                "NAVident" to "Z123456",
                "name" to "Test User"
            )
        ).serialize()

        mockMvc.perform(
            get("/api/endpoint")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk)
    }
}
```

### Unit Test with Mocked SubjectHandler

```kotlin
@BeforeEach
fun setup() {
    val mockHandler = mockk<SubjectHandler> {
        every { getUserID() } returns "Z123456"
        every { getUserName() } returns "Test User"
    }
    SubjectHandler.set(mockHandler)
}

@AfterEach
fun cleanup() {
    // Reset to real handler
    SubjectHandler.set(SpringSubjectHandler(SpringTokenValidationContextHolder()))
}
```

## Tilgangsmaskinen API

### Request

```http
POST /api/v1/tilgang
Content-Type: application/json

{
  "fnr": "12345678901",
  "regelType": "KOMPLETT_REGELTYPE"
}
```

### Response

```json
{
  "harTilgang": true
}
```

### Error Response

```json
{
  "feilmelding": "Bruker har ikke tilgang",
  "harTilgang": false
}
```

## Cache Configuration

### Tilgangsmaskinen Cache

```kotlin
// Cache name: tilgangsmaskinen
// Key: fnr_userId
// TTL: Configured in application.yml

@Cacheable(
    value = ["tilgangsmaskinen"],
    key = "#fnr + '_' + T(...).getInstance().getUserID()"
)
```

### Clear Cache

```kotlin
@CacheEvict(value = ["tilgangsmaskinen"], allEntries = true)
fun clearCache()
```

## Related Skills

- **person**: PDL requires auth
- **oppgave**: Oppgave API auth
- **testing**: Mock OAuth setup
