---
name: integrasjon
description: |
  External integrations overview for melosys-api including REST/SOAP services, Kafka topics, databases, and support services. Use when adding new integrations, debugging external calls, understanding data flow, or troubleshooting connectivity. Triggers: "integration", "external service", "consumer", "kafka topic", "REST client".
---

# External Integrations

Melosys-api integrates with numerous NAV internal services and external systems. Outbound REST/SOAP clients and the Kafka producers live in the `integrasjon` module and follow consistent patterns. Inbound Kafka consumers live in the `service` module (see the Kafka Topics section). The class naming convention is `*Client` / `*ClientConfig` / `*ClientProducer` for REST/SOAP, plus `*Service` / `*Fasade` wrappers — not `*Consumer`.

## REST/SOAP Services

### Person & Organization Data

| Service | Client/Service class | Purpose | Auth |
|---------|---------------|---------|------|
| **PDL** | `PDLClient` / `PDLClientImpl` | Person data (GraphQL) | Azure AD |
| **Aareg** | `ArbeidsforholdClient` | Employment records | Azure AD |
| **EREG** | `OrganisasjonRestClient` | Organization data (Brønnøysund) | None |
| **Inntekt** | `InntektClient` (`InntektService` wrapper) | Income from Skatteetaten | Azure AD |

### Document & Archive

| Service | Client/Service class | Purpose | Auth |
|---------|---------------|---------|------|
| **SAF** | `SafClient` | Document retrieval (GraphQL) | Azure AD |
| **Joark** | `JoarkFasade` / `JoarkService` | Document archiving | Azure AD |
| **Dokgen** | `DokgenClient` | PDF generation | Azure AD |
| **Doksys** | `DoksysFasade` / `DoksysService` (`DokumentproduksjonClient`) | Legacy doc production (SOAP) | STS |
| **Distribuer** | `DistribuerJournalpostClient` | Document distribution | Azure AD |

### Case Management

| Service | Client/Service class | Purpose | Auth |
|---------|---------------|---------|------|
| **Oppgave** | `OppgaveFasade` / `OppgaveFasadeImpl` | Task management (Gosys) | Azure AD |
| **Sak** | `BasicAuthSakClient` | Case system | Basic Auth |
| **Medl** | `MedlemskapClient` (`MedlService` wrapper) | Membership register | Azure AD |

### EU/EESSI

| Service | Client/Service class | Purpose | Auth |
|---------|---------------|---------|------|
| **eux-rina-api** | `EessiClient` | EESSI/RINA integration | Azure AD |

### Financial & Tax

| Service | Client/Service class | Purpose | Auth |
|---------|---------------|---------|------|
| **Fakturering** | `FaktureringskomponentenClient` | Billing component | Azure AD |
| **Trygdeavgift** | `TrygdeavgiftClient` | Social security calculations (melosys-trygdeavgift-beregning) | None (in-cluster) |
| **Utbetaling** | `UtbetaltdataClient` | Payment data | Azure AD |

### Support Services

| Service | Client/Service class | Purpose | Auth |
|---------|---------------|---------|------|
| **Kodeverk** | `KodeverkClient` (`KodeverkRegisterImpl` wrapper) | Reference data | Azure AD |
| **Tilgangsmaskinen** | `TilgangsmaskinenClient` | Access control (ABAC) | Azure AD |
| **Azure AD** | `AzureAdClient` (`AzureAdService` wrapper) | User/group lookup + token issuing | Azure AD |
| **STS** | `SecurityTokenServiceClient` | Token exchange (legacy) | Basic |

### Application Intake

| Service | Client/Service class | Purpose | Auth |
|---------|---------------|---------|------|
| **Altinn** | `SoknadMottakClient` | A1 application forms | Azure AD |
| **Inngangsvilkår** | `InngangsvilkaarClient` | Entry requirements | None (in-cluster) |

## Kafka Topics

### Inbound (Consumers)

The Kafka consumer classes live in the `service` module (`service/src/main/.../no/nav/melosys/service/...`), not `integrasjon`. Topic names below are the configured `teammelosys.*` topics (see `app/src/main/resources/application-nais.yml`).

| Topic | Consumer Class | Module path | Purpose |
|-------|---------------|-------------|---------|
| `teammelosys.eessi.v1` | `EessiMeldingConsumer` | `service/eessi/kafka` | Received/sent SED + RINA events (single topic) |
| `teammelosys.soknad-mottak.v1` | `SoknadMottattConsumer` | `service/soknad` | Altinn applications |
| `teammelosys.skjema.innsendt.v1` | `DigitalSøknadMottattConsumer` | `service/soknad` | Digital A1 skjema (melosys-skjema-api) |
| `teammelosys.skattehendelser.v1` | `SkattehendelserConsumer` | `service/avgift/aarsavregning` | Tax events |
| `teammelosys.manglende-fakturabetaling.v1` | `ManglendeFakturabetalingConsumer` | `service/avgift` | Overdue invoices |

### Outbound (Producers)

The producer classes live in the `integrasjon` module.

| Topic | Producer Class | Purpose |
|-------|---------------|---------|
| `teammelosys.melosys-hendelser.v1` | `KafkaMelosysHendelseProducer` | Melosys events |
| `teammelosys.popp-hendelser.v1` | `KafkaPensjonsopptjeningHendelseProducer` | Pension (POPP) events |

## Databases

| Database | Type | Module |
|----------|------|--------|
| Melosys-DB | Oracle | app (Flyway) |
| Melosys-eessi-db | PostgreSQL | External |

## Authentication Patterns

Service-to-service auth is Azure AD (on-behalf-of / client-credentials), attached as a WebClient filter. There is no TokenX usage in the `integrasjon` module — auth is wired through `GenericAuthFilterFactory.getAzureFilter(clientName)` (Kotlin configs) or `AzureContextExchangeFilter` (Java configs). The `clientName` maps to an Azure scope configured per service.

### Azure AD (Service-to-service)
```kotlin
// In a *ClientConfig: attach the Azure auth filter for this client
@Bean
fun myServiceClient(
    webClientBuilder: WebClient.Builder,
    genericAuthFilterFactory: GenericAuthFilterFactory,
    correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
) = MyServiceClient(
    webClientBuilder
        .baseUrl(url)
        .filter(genericAuthFilterFactory.getAzureFilter(CLIENT_NAME))
        .filter(correlationIdOutgoingFilter)
        .filter(errorFilter("Kall mot MyService feilet."))
        .build()
)
```

### STS (Legacy SOAP)
```java
// SecurityTokenServiceClient (reststs/) fetches a SAML/OIDC token using Basic auth
securityTokenServiceClient.getResponseForSamlToken()
```

### No auth (in-cluster)
Some in-cluster sidecar/team-internal calls (e.g. `InngangsvilkaarClient`, `TrygdeavgiftClient`) attach no auth filter at all — only the correlation-id and error filters.

## Module Structure

The `integrasjon` module has BOTH a Kotlin and a Java source tree. Newer/migrated clients are Kotlin; some older clients (PDL, SAF/Joark, Doksys, Oppgave, STS, dokumentmottak) are still Java.

```
integrasjon/src/main/kotlin/no/nav/melosys/integrasjon/
├── aareg/           # Employment records
├── azuread/         # Azure AD client (token issuing) + AzureAdService
├── dokgen/          # PDF generation
├── doksys/          # Legacy doc (SOAP) + distribuerjournalpost/
├── eessi/           # EESSI/RINA
├── ereg/            # Organization lookup
├── faktureringskomponenten/
├── felles/          # Shared utilities (GenericAuthFilterFactory, error/MDC filters)
├── hendelser/       # Kafka producer (melosys-hendelser)
├── inngangsvilkar/  # Entry requirements
├── inntekt/         # Income data
├── joark/           # Document archive
├── kafka/           # Kafka config
├── kodeverk/        # Reference data (impl/)
├── medl/            # Membership register
├── melosysskjema/   # melosys-skjema-api client
├── popp/            # Pension (POPP) producer
├── sak/             # Case system
├── soknadmottak/    # Altinn intake
├── tilgangsmaskinen/# Access control
├── trygdeavgift/    # Tax calculations
└── utbetaling/      # Payment data

integrasjon/src/main/java/no/nav/melosys/integrasjon/
├── dokgen/          # DokgenClient
├── doksys/          # Dokumentproduksjon (SOAP)
├── dokumentmottak/  # Document intake
├── eessi/
├── felles/          # AzureContextExchangeFilter, BasicAuthAware
├── joark/           # JoarkFasade/JoarkService + saf/ (SafClient)
├── oppgave/         # OppgaveFasade + konsument/
├── pdl/             # PDLClient/PDLClientImpl (GraphQL)
└── reststs/         # SecurityTokenServiceClient (STS)
```

## Common Patterns

### Creating a New REST Client
Pattern (trimmed) based on `OrganisasjonRestClient` / `OrganisasjonRestClientConfig`:
```kotlin
// 1. The client wraps an injected WebClient
open class MyServiceClient(private val webClient: WebClient) {
    fun hentData(id: String): MyResponse =
        webClient.get().uri("/api/v1/data/{id}", id)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono<MyResponse>()
            .block() ?: throw TekniskException("MyService respons er null")
}

// 2. A @Configuration builds the WebClient bean with the auth + error filters
@Configuration
class MyServiceClientConfig(
    @Value("\${myservice.url}") private val url: String,
    private val genericAuthFilterFactory: GenericAuthFilterFactory,
) {
    @Bean
    fun myServiceClient(
        webClientBuilder: WebClient.Builder,
        correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
    ) = MyServiceClient(
        webClientBuilder
            .baseUrl(url)
            .filter(genericAuthFilterFactory.getAzureFilter(CLIENT_NAME)) // omit if in-cluster/no auth
            .filter(correlationIdOutgoingFilter)
            .filter(errorFilter("Kall mot MyService feilet."))
            .build()
    )

    companion object { private const val CLIENT_NAME = "my-service" }
}
```

### Error Handling
```kotlin
// The shared errorFilter (no.nav.melosys.integrasjon.felles) maps non-2xx responses
// to the right exception; clients throw TekniskException / IkkeFunnetException
// (no.nav.melosys.exception) on null/unexpected bodies. IntegrasjonException also exists.
.filter(errorFilter("Henting av organisasjon fra ereg feilet"))

// Retry with Spring Retry — used as a bare class/interface-level annotation
@Retryable
open class InntektClient(private val webClient: WebClient) { ... }
```

### Caching
```kotlin
// Cache external responses with @Cacheable. Cache names actually in use:
// "kodeverk" (KodeverkRegisterImpl, integrasjon) and "tilgangsmaskinen"
// (TilgangsmaskinenService, service module).
@Cacheable("kodeverk")
fun hentKodeverk(...): ...
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
