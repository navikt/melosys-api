---
name: integrasjon
description: |
  External integrations overview for melosys-api including REST/SOAP services, Kafka topics, databases, and support services. Use when adding new integrations, debugging external calls, understanding data flow, or troubleshooting connectivity. Triggers: "integration", "external service", "consumer", "kafka topic", "REST client".
---

# External Integrations

Melosys-api integrates with numerous NAV internal services and external systems. All integrations are in the `integrasjon` module and follow consistent patterns.

## REST/SOAP Services

### Person & Organization Data

| Service | Consumer Class | Purpose | Auth |
|---------|---------------|---------|------|
| **PDL** | `PDLConsumerImpl` | Person data (GraphQL) | TokenX |
| **Aareg** | `ArbeidsforholdConsumer` | Employment records | TokenX |
| **EREG** | `OrganisasjonRestConsumer` | Organization data (Brønnøysund) | None |
| **Inntekt** | `InntektRestConsumer` | Income from Skatteetaten | TokenX |

### Document & Archive

| Service | Consumer Class | Purpose | Auth |
|---------|---------------|---------|------|
| **SAF** | `SafConsumer` | Document retrieval (GraphQL) | Azure AD |
| **Joark** | `JournalpostapiConsumer` | Document archiving | Azure AD |
| **Dokgen** | `DokgenConsumer` | PDF generation | Azure AD |
| **Doksys** | `DokumentproduksjonConsumer` | Legacy doc production (SOAP) | STS |
| **Distribuer** | `DistribuerJournalpostConsumer` | Document distribution | Azure AD |

### Case Management

| Service | Consumer Class | Purpose | Auth |
|---------|---------------|---------|------|
| **Oppgave** | `OppgaveConsumer` | Task management (Gosys) | Azure AD |
| **Sak** | `BasicAuthSakConsumer` | Case system | Basic Auth |
| **Medl** | `MedlemskapRestConsumer` | Membership register | TokenX |

### EU/EESSI

| Service | Consumer Class | Purpose | Auth |
|---------|---------------|---------|------|
| **eux-rina-api** | `EessiConsumerImpl` | EESSI/RINA integration | Azure AD |

### Financial & Tax

| Service | Consumer Class | Purpose | Auth |
|---------|---------------|---------|------|
| **Fakturering** | `FaktureringskomponentenConsumer` | Billing component | Azure AD |
| **Trygdeavgift** | `TrygdeavgiftConsumer` | Social security calculations | Azure AD |
| **Utbetaling** | `UtbetaldataRestConsumer` | Payment data | TokenX |

### Support Services

| Service | Consumer Class | Purpose | Auth |
|---------|---------------|---------|------|
| **Kodeverk** | `KodeverkConsumerImpl` | Reference data | None |
| **Tilgangsmaskinen** | `TilgangsmaskinenConsumer` | Access control (ABAC) | Azure AD |
| **Azure AD** | `AzureAdConsumer` | User/group lookup | Azure AD |
| **STS** | `SecurityTokenServiceConsumer` | Token exchange (legacy) | Basic |

### Application Intake

| Service | Consumer Class | Purpose | Auth |
|---------|---------------|---------|------|
| **Altinn** | `SoknadMottakConsumer` | A1 application forms | TokenX |
| **Inngangsvilkår** | `InngangsvilkaarConsumer` | Entry requirements | Azure AD |

## Kafka Topics

### Inbound (Consumers)

| Topic | Consumer Class | Purpose |
|-------|---------------|---------|
| `eessi-basis-sed-mottatt` | `EessiMeldingConsumer` | Received SED documents |
| `eessi-basis-sed-sendt` | `EessiMeldingConsumer` | Sent SED documents |
| `eux-rina-events` | `EessiMeldingConsumer` | RINA events |
| `oppgave-endret` | - | Task changes |
| `soknad-mottatt` | `SoknadMottattConsumer` | Altinn applications |
| `skattehendelser` | `SkattehendelserConsumer` | Tax events |
| `manglende-fakturabetaling` | `ManglendeFakturabetalingConsumer` | Overdue invoices |

### Outbound (Producers)

| Topic | Producer Class | Purpose |
|-------|---------------|---------|
| `melosys-hendelser` | `KafkaMelosysHendelseProducer` | Melosys events |
| `popp-hendelser` | `KafkaPensjonsopptjeningHendelseProducer` | Pension events |

## Databases

| Database | Type | Module |
|----------|------|--------|
| Melosys-DB | Oracle | app (Flyway) |
| Melosys-eessi-db | PostgreSQL | External |

## Authentication Patterns

### TokenX (User-context)
```kotlin
// For services requiring user token exchange
@Value("\${token.x.client.id}") clientId: String
webClient.post()
    .header("Authorization", "Bearer ${tokenService.exchangeToken(...)}")
```

### Azure AD (Service-to-service)
```kotlin
// Machine-to-machine with Azure AD
azureAdTokenProvider.getToken(scope)
webClient.post()
    .header("Authorization", "Bearer $token")
```

### STS (Legacy SOAP)
```kotlin
// For legacy SOAP services
securityTokenServiceConsumer.exchangeSamlToken(...)
```

## Module Structure

```
integrasjon/src/main/kotlin/no/nav/melosys/integrasjon/
├── aareg/           # Employment records
├── azuread/         # Azure AD integration
├── dokgen/          # PDF generation
├── doksys/          # Legacy doc (SOAP)
├── eessi/           # EESSI/RINA
├── ereg/            # Organization lookup
├── faktureringskomponenten/
├── felles/          # Shared utilities
├── hendelser/       # Kafka producers
├── inngangsvilkar/  # Entry requirements
├── inntekt/         # Income data
├── joark/           # Document archive (SAF + Journalpost)
├── kafka/           # Kafka config
├── kodeverk/        # Reference data
├── medl/            # Membership register
├── popp/            # Pension producer
├── sak/             # Case system
├── soknadmottak/    # Altinn intake
├── tilgangsmaskinen/# Access control
├── trygdeavgift/    # Tax calculations
└── utbetaling/      # Payment data
```

## Common Patterns

### Creating a New REST Consumer
```kotlin
// 1. Create Consumer class
open class MyServiceConsumer(private val webClient: WebClient) {
    fun getData(id: String): MyResponse =
        webClient.get()
            .uri("/api/v1/data/{id}", id)
            .retrieve()
            .bodyToMono(MyResponse::class.java)
            .block() ?: throw IntegrasjonException("No response")
}

// 2. Create Config (WebClient factory)
@Configuration
class MyServiceConsumerConfig(
    @Value("\${myservice.url}") private val url: String
) : WebClientConfig {
    @Bean
    fun myServiceConsumer(tokenProvider: AzureAdTokenProvider) =
        MyServiceConsumer(
            createWebClient(url, tokenProvider, "my-service-scope")
        )
}
```

### Error Handling
```kotlin
// Use IntegrasjonException for external failures
throw IntegrasjonException("Failed to call MyService: ${response.statusCode}")

// Retry with Spring Retry
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
fun callService() { ... }
```

### Caching
```kotlin
// Cache external responses with Caffeine
@Cacheable(value = ["tilgang"], key = "#saksbehandlerIdent + #fnr")
fun hentTilgang(saksbehandlerIdent: String, fnr: String): Tilgang
```

## Local Development

All external services are mocked via `melosys-docker-compose`:
- Mock server: `http://localhost:8083`
- Swagger: `http://localhost:8083/swagger-ui/`
- Kafka UI: `http://localhost:8087`

## Troubleshooting

| Issue | Check |
|-------|-------|
| 401/403 errors | Token scope, audience, expiry |
| Connection refused | VPN, firewall, service URL |
| Timeout | Increase timeout, check service health |
| Deserialization | Response format, missing fields |

See individual skill files for detailed service documentation:
- `/person` - PDL patterns
- `/eessi-eux` - EESSI/RINA integration
- `/oppgave` - Task management
- `/journalforing` - Document archiving
- `/medl` - Membership register
