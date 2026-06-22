---
name: security
description: |
  Expert knowledge of authentication and authorization patterns in melosys-api.
  Use when: (1) Understanding Azure AD/OIDC authentication,
  (2) Debugging Tilgangsmaskinen access control,
  (3) Understanding SubjectHandler and user context,
  (4) Configuring OAuth2 clients for service-to-service calls,
  (5) Understanding audit logging patterns.
---

# Security Skill

Expert knowledge of authentication and authorization in melosys-api.

## Quick Reference

### Authentication Methods

| Method | Purpose | Usage |
|--------|---------|-------|
| **Azure AD OIDC** | User authentication | Frontend users (saksbehandler) |
| **OAuth2 Client Credentials** | Service-to-service | Internal NAV services |
| **JWT Bearer** | On-behalf-of | User context propagation |
| **mock-oauth2-server** | Local development | Ports 8082, 8086 |

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `SubjectHandler` | sikkerhet/ | Get current user context |
| `TilgangsmaskinenService` | service/.../tilgangsmaskinen/ | Access control to person data |
| `AuditLogger` | sikkerhet/.../logging/ | CEF audit logging |
| `OAuth2 Config` | application.yml | OAuth2 client config |

## SubjectHandler

Access current user context from anywhere in the application.

```kotlin
// Get current user
val userId = SubjectHandler.getSaksbehandlerIdent()
val userName = SubjectHandler.getInstance().getUserName()
val groups = SubjectHandler.getInstance().getGroups()

// Get user or system user fallback
val userOrSystem = SubjectHandler.getUserIDOrSystemUser()

// Get OIDC token for propagation
val token = SubjectHandler.getInstance().getOidcTokenString()
```

### System User

`SubjectHandler` is a Java class; `SYSTEMBRUKER` is a public static field (not a Kotlin companion object).

```java
// SubjectHandler.java
public static String SYSTEMBRUKER = "srvmelosys";
```

## Tilgangsmaskinen (Access Control)

Replaces legacy ABAC system for access control to person data.

```kotlin
@Service
class TilgangsmaskinenService(
    private val tilgangsmaskinenClient: TilgangsmaskinenClient,
    private val persondataService: PersondataService
) {
    @Cacheable(value = ["tilgangsmaskinen"], key = "#fnr + '_' + T(no.nav.melosys.sikkerhet.context.SubjectHandler).getInstance().getUserID()")
    fun sjekkTilgangTilFnr(fnr: String): Boolean {
        return tilgangsmaskinenClient.sjekkTilgang(fnr, RegelType.KOMPLETT_REGELTYPE)
    }

    @Cacheable(value = ["tilgangsmaskinen"], key = "#aktørId + '_' + T(no.nav.melosys.sikkerhet.context.SubjectHandler).getInstance().getUserID()")
    fun sjekkTilgangTilAktørId(aktørId: String): Boolean {
        val fnr = hentFnrFraPdl(aktørId)
        return tilgangsmaskinenClient.sjekkTilgang(fnr, RegelType.KOMPLETT_REGELTYPE)
    }
}
```

### Usage in Services

Inject the `Aksesskontroll` interface (implemented by `TilgangsmaskinenAksesskontroll`).
Its methods include `auditAutoriser(behandlingID, kontekst)`, `auditAutoriserSkriv(behandlingID, kontekst)`,
`auditAutoriserAktørID(aktørID, kontekst)` and `autoriserSakstilgang(saksnummer)`.

```kotlin
@Service
class MyService(
    private val aksesskontroll: Aksesskontroll
) {
    fun doSomething(behandlingID: Long) {
        aksesskontroll.auditAutoriser(behandlingID, "MyService.doSomething")
        // Throws SikkerhetsbegrensningException if access denied
    }
}
```

## OAuth2 Client Configuration

### Client Types

| Grant Type | Use Case |
|------------|----------|
| `client_credentials` | Service-to-service (no user context) |
| `urn:ietf:params:oauth:grant-type:jwt-bearer` | On-behalf-of (with user context) |

### Configuration Structure

```yaml
no.nav.security.jwt:
  client:
    registration:
      pdl:  # Client name
        authentication:
          client-auth-method: client_secret_basic
          client-id: ${AZURE_APP_CLIENT_ID}
          client-secret: ${AZURE_APP_CLIENT_SECRET}
        grant-type: urn:ietf:params:oauth:grant-type:jwt-bearer
        scope: api://${NAIS_CLUSTER_NAME}.pdl.pdl-api/.default
        token-endpoint-url: ${AZURE_OPENID_CONFIG_TOKEN_ENDPOINT}
```

### Registered Clients

The authoritative list lives in the `registration:` block of `app/src/main/resources/application.yml`
(it drifts as integrations are added). As of this writing:

| Client | Grant Type | Target |
|--------|------------|--------|
| `aareg` | jwt-bearer | Aareg (arbeidsforhold) |
| `faktureringskomponenten` | jwt-bearer | Billing |
| `felleskodeverk` | jwt-bearer | Felles kodeverk |
| `graph` | client_credentials | Microsoft Graph |
| `inntekt` | client_credentials | Inntektskomponenten |
| `medl` | jwt-bearer | MEDL |
| `melosys-eessi` | jwt-bearer | EESSI |
| `oppgave` | jwt-bearer | Oppgave |
| `pdl` | jwt-bearer | PDL |
| `saf` | jwt-bearer | SAF |
| `sokos-utbetaldata` | jwt-bearer | Utbetalingsdata |
| `tilgangsmaskinen` | jwt-bearer | Tilgangsmaskinen |
| `melosys-soknad-mottak` | jwt-bearer | Søknad mottak |
| `melosys-skjema` | jwt-bearer | Skjema API |
| `dokarkiv` | jwt-bearer | Joark |
| `dokdistfordeling` | jwt-bearer | Document distribution |

## Audit Logging

CEF (Common Event Format) logging for security events.

```kotlin
@Component
class AuditLogger {
    fun log(auditEvent: AuditEvent) {
        audit.info { createCommonEventFormatString(auditEvent) }
    }
}

data class AuditEvent(
    val type: AuditEventType,
    val sourceUserId: String,
    val destinationUserId: String,
    val message: String? = null,
    val sourceProcessName: String? = null
)

enum class AuditEventType {
    READ, UPDATE
}
```

### Log Format

```
CEF:0|melosys|Auditlog|1.0|audit:read|Medlemskap og lovvalg|INFO|
suid=Z123456 duid=12345678901 end=1703318400000 requestMethod=GET
request=/api/fagsak/123 sproc=FagsakController msg=Hentet fagsak
```

## Token Validation

### NAIS Configuration

```yaml
no.nav.security.jwt:
  issuer:
    aad:
      accepted-audience: ${AZURE_APP_CLIENT_ID}
      discoveryurl: ${AZURE_APP_WELL_KNOWN_URL}
```

### Token Claims

| Claim | Description |
|-------|-------------|
| `NAVident` | Saksbehandler ID (e.g., Z123456) |
| `name` | User's full name |
| `groups` | AD group memberships |
| `aud` | Audience (client ID) |

## Local Development

### mock-oauth2-server

Ports:
- `8082`: Main OAuth server
- `8086`: Alternative port

### Configuration

```yaml
# application-local-mock.yml
AZURE_APP_CLIENT_ID: '"lol"'
AZURE_APP_CLIENT_SECRET: '"lol"'
AZURE_OPENID_CONFIG_TOKEN_ENDPOINT: http://host.docker.internal:8082/isso/token
```

### Test Token

```kotlin
@EnableMockOAuth2Server
class MyIT {
    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    val token = mockOAuth2Server.issueToken(
        issuerId = "aad",
        subject = "Z123456",
        claims = mapOf("NAVident" to "Z123456")
    ).serialize()
}
```

## Admin Endpoints Security

Admin endpoints are `@Protected` (token-support) controllers, additionally gated by
`ApiKeyInterceptor` (`frontend-api/.../tjenester/gui/config/ApiKeyInterceptor.kt`).

### API Key Validation

`ApiKeyInterceptor` compares the request header `X-MELOSYS-ADMIN-APIKEY` (constant `API_KEY_HEADER`)
against the configured `Melosys-admin.apikey` value and returns HTTP 403 on mismatch.
In nais the key comes from the `MELOSYS_ADMIN_API_KEY` env var (`application-nais.yml`:
`apikey: ${MELOSYS_ADMIN_API_KEY}`).

```kotlin
@Component
class ApiKeyInterceptor(
    @Value("\${Melosys-admin.apikey}") private val apiKey: String
) : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (request.getHeader(API_KEY_HEADER) != apiKey) {
            response.status = 403
            response.writer.write("Invalid API key")
            return false
        }
        return true
    }

    companion object {
        const val API_KEY_HEADER = "X-MELOSYS-ADMIN-APIKEY"
    }
}
```

## Error Handling

### SikkerhetsbegrensningException

Thrown when access is denied (in `TilgangsmaskinenAksesskontroll`, package `no.nav.melosys.exception`):

```kotlin
throw SikkerhetsbegrensningException("Tilgangsmaskinen: Brukeren har ikke tilgang til ressurs")
```

### TilgangsmaskinenException

Thrown on technical errors:

```kotlin
throw TilgangsmaskinenException("Feil ved tilgangssjekk", cause)
```

## Caching

Access control results are cached:

```kotlin
@Cacheable(
    value = ["tilgangsmaskinen"],
    key = "#fnr + '_' + T(no.nav.melosys.sikkerhet.context.SubjectHandler).getInstance().getUserID()"
)
fun sjekkTilgangTilFnr(fnr: String): Boolean
```

Cache key includes both the person ID and the user ID to ensure user-specific results.

## References

- [`references/debugging.md`](references/debugging.md) — step-by-step debugging for token validation, access-denied, OAuth2 client, and mock-OAuth issues.

## Related Skills

- **person**: PDL integration uses OAuth2
- **journalforing**: SAF/Joark integration
- **oppgave**: Oppgave API authentication
- **eessi-eux**: EESSI/EUX authentication
