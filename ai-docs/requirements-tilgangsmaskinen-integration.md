# Krav - Tilgangsmaskinen Integration

## Oppgave ID

MELOSYS-7371 - Erstatte ABAC med Tilgangsmaskinen

## Overordnet Mål

Erstatte det eksisterende ABAC-baserte tilgangskontrollsystemet med NAVs nye Tilgangsmaskinen-løsning for å forbedre sikkerhet og standardisere tilgangskontroll.

## Bakgrunn

Det nåværende ABAC-systemet har kjente sårbarheter og bruker annen teknologi enn resten av applikasjonen. Tilgangsmaskinen er NAVs organisasjonsovergripende løsning for tilgangskontroll som skal erstatte ABAC.

## ⚠️ KRITISK ENDRING - KORREKT API SPESIFIKASJON

**NB!** Den opprinnelige implementeringen var basert på feil API-informasjon. Nedenstående er den **korrekte** API-spesifikasjonen basert på offisiell OpenAPI dokumentasjon.

## API Spesifikasjon - Tilgangsmaskinen (KORREKT)

### Base URL

```
https://tilgangsmaskin.intern.nav.no
```

### Primære Endepunkter

#### 1. Kjerneregler (Anbefalt for Melosys)

-   **URL**: `POST /api/v1/kjerne`
-   **Content-Type**: `application/json`
-   **Request Body**: Bare en string (fnr/dnr)
-   **Beskrivelse**: Evaluerer kjerne-regelsett for en bruker

#### 2. Komplett regelsett

-   **URL**: `POST /api/v1/komplett`
-   **Content-Type**: `application/json`
-   **Request Body**: Bare en string (fnr/dnr)
-   **Beskrivelse**: Evaluerer komplett regelsett for en bruker

#### 3. Bulk OBO (On-Behalf-Of)

-   **URL**: `POST /api/v1/bulk/obo`
-   **Content-Type**: `application/json`
-   **Request Body**: Array av `BrukerIdOgRegelsett`
-   **Beskrivelse**: Bulk-evaluering for mange brukere

### Request Format (KORREKT)

**For enkelt-bruker endepunkter (`/api/v1/kjerne`, `/api/v1/komplett`):**

```json
"12345678901"
```

Bare fnr/dnr som string - **IKKE** et objekt med fnr og roller!

**For bulk endepunkter:**

```json
[
    {
        "brukerId": "12345678901",
        "type": "KJERNE_REGELTYPE"
    }
]
```

### Response Format (KORREKT)

**Suksess (204 No Content)**: Tilgang innvilget - tom response body

**Avvist (403 Forbidden)**: Tilgang nektet med ProblemDetail format

```json
{
    "type": "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
    "title": "AVVIST_STRENGT_FORTROLIG_ADRESSE",
    "status": 403,
    "instance": "Z990883/03508331575",
    "brukerIdent": "03508331575",
    "navIdent": "Z990883",
    "traceId": "444290be30ed4fdd9a849654bad9dc1b",
    "begrunnelse": "Du har ikke tilgang til brukere med strengt fortrolig adresse",
    "kanOverstyres": false
}
```

### Regeltyper

-   **KJERNE_REGELTYPE**: Grunnleggende tilgangsregler
-   **KOMPLETT_REGELTYPE**: Alle tilgangsregler inkludert utvidede
-   **OVERSTYRBAR_REGELTYPE**: Regler som kan overstyres

### Avvisningskoder (title felt)

-   `AVVIST_STRENGT_FORTROLIG_ADRESSE`
-   `AVVIST_STRENGT_FORTROLIG_UTLAND`
-   `AVVIST_AVDØD`
-   `AVVIST_PERSON_UTLAND`
-   `AVVIST_SKJERMING`
-   `AVVIST_FORTROLIG_ADRESSE`
-   `AVVIST_UKJENT_BOSTED`
-   `AVVIST_GEOGRAFISK`
-   `AVVIST_HABILITET`

### Feilhåndtering

-   **204**: Tilgang innvilget
-   **403**: Tilgang nektet (med ProblemDetail)
-   **404**: Person ikke funnet i PDL
-   **500**: Teknisk feil i Tilgangsmaskinen
-   **401**: Autentiseringsfeil

## Funksjonelle Krav (OPPDATERT)

### 1. Tilgangsmaskinen Consumer/Producer

-   **F001**: Implementere Consumer for Tilgangsmaskinen som bruker Azure tokens med OBO (On-Behalf-Of) flow
-   **F002**: Implementere ConsumerProducer pattern som følger eksisterende mønstre i kodebasen
-   **F003**: Full request/response typing med Kotlin data classes
-   **F004**: Støtte endepunkt `/api/v1/kjerne` (anbefalt) eller `/api/v1/komplett`
-   **F005**: Håndtere korrekt request format (bare fnr/dnr som string)
-   **F006**: Håndtere ProblemDetail response format for feil

### 2. PDL Integration for Identmapping

-   **F007**: Hente fnr/dnr fra PDL siden databasen kun inneholder aktørId
-   **F008**: Konvertere aktørId til folkeregisterident via eksisterende `PersondataService.hentFolkeregisterident()`
-   **F009**: Håndtere tilfeller hvor aktørId ikke kan mappes til fnr/dnr

### 3. Caching

-   **F010**: Implementere 1-time caching for tilgangskontrollkall
-   **F011**: Cache-nøkler skal være basert på bruker + regeltype
-   **F012**: Bruke Spring Cache med Caffeine som eksisterende implementasjon

### 4. Feature Toggle og Separation of Concerns

-   **F013**: Legge ny løsning bak feature toggle `AKSESSKONTROLL_TILGANGSMASKINEN`
-   **F014**: **Når toggle er skrudd AV** → bruk ABAC (dagens løsning)
-   **F015**: **Når toggle er skrudd PÅ** → bruk Tilgangsmaskinen
-   **F016**: Lage egen `TilgangsmaskinenService` separert fra ABAC-kode
-   **F017**: Beholde eksisterende ABAC-system inntil ny løsning er produksjonstestet

### 5. Kompatibilitet

-   **F018**: Bevare eksisterende API for `Aksesskontroll` interfacet
-   **F019**: Samme feilhåndtering som ABAC (kast `SikkerhetsbegrensningException`)
-   **F020**: Samme audit-logging som eksisterende implementasjon

## Tekniske Krav (OPPDATERT)

### 1. Arkitektur

-   **T001**: Følge eksisterende Consumer/Producer pattern
-   **T002**: Implementere i `integrasjon` modulen som andre eksterne integrasjoner
-   **T003**: Bruke WebClient for HTTP-kall
-   **T004**: Implementere Azure-autentisering med eksisterende filter

### 2. Kode og Språk

-   **T005**: Alt kode skal skrives på norsk (unntatt tekniske termer)
-   **T006**: Sterkt typet - bruk Kotlin data classes for alle DTOs
-   **T007**: Følg eksisterende kodestandarder og mønstre

### 3. Konfigasjon

-   **T008**: Azure AD client registrering i `application.properties`
-   **T009**: Tilgangsmaskinen URL: `https://tilgangsmaskin.intern.nav.no`
-   **T010**: Cache-konfigurasjon i `application.properties`

### 4. Testing

-   **T012**: Enhetstester for alle nye komponenter
-   **T013**: Integrasjonstester med wiremock
-   **T014**: Tester for feature toggle funksjonalitet

## Eksisterende Systemanalyse

### ABAC Implementasjon

-   **PepImpl.java**: Hovedklasse for ABAC tilgangskontroll
    -   Implementerer `Pep` interface med `sjekkTilgangTilFnr()` og `sjekkTilgangTilAktoerId()`
    -   Kaster `SikkerhetsbegrensningException` ved manglende tilgang
    -   Logger tilgangsbeslutninger
-   **BrukertilgangKontroll.java**: Validerer tilgang via Pep interface
    -   `validerTilgangTilAktørID()` og `validerTilgangTilFolkeregisterIdent()`
-   **AksesskontrollImpl.java**: Orkestrerer tilgangskontroll
    -   Implementerer `Aksesskontroll` interface
    -   Håndterer audit-logging
    -   Konverterer behandling-IDs til aktørIDs
-   **AbacDefaultConfig.java**: ABAC konfigurasjon

### PDL Integration

-   **PersondataService.java**:
    -   `@Cacheable("folkeregisterIdent")` for `hentFolkeregisterident()` og `finnFolkeregisterident()`
    -   `@Cacheable("aktoerID")` for `hentAktørIdForIdent()`
    -   8-timers cache TTL (480 minutter)
-   **PDLConsumer**: Interface for PDL integrasjon
-   **PDLConsumerImpl**: GraphQL-basert implementasjon
-   **Eksisterende metoder**: `hentFolkeregisterident()`, `finnFolkeregisterident()`

### Azure Autentisering

-   **SpringSubjectHandler.java**: Håndterer Azure AD tokens
    -   `getOidcTokenString()` for å hente brukertoken
    -   Detekterer M2M-tokens via `idtyp` claim
-   **AzureContextExchangeFilter.java**: Azure autentisering for WebClient
    -   Arver fra `GenericContextExchangeFilter`
    -   Støtter både brukertoken (OBO) og systemtoken (client_credentials)
-   **GenericContextExchangeFilter.java**: Base klasse for autentiseringsfiltre
    -   `getCorrectToken()` basert på `ThreadLocalAccessInfo.shouldUseSystemToken()`
-   **Eksisterende Azure konfigurasjoner**: PDL, oppgave, SAF, MEDL, etc.

### Feature Toggles

-   **ToggleName.kt**: Inneholder allerede `AKSESSKONTROLL_TILGANGSMASKINEN = "akesskontroll.tilgangsmaskinen"`
-   **Unleash konfigurert**:
    -   `FeatureToggleConfigNais` for produksjon
    -   `FeatureToggleConfigLocal` for lokal utvikling
    -   `FeatureToggleConfigTest` for testing
-   **Mønstre**:
    -   `@ConditionalOnProperty` for conditional bean creation
    -   `unleash.isEnabled(ToggleName.KONSTANT)` for runtime checking
    -   `ByUserIdStrategy` for brukerbasert toggling

### Caching

-   **Spring Cache**: Konfigurert med Caffeine i `ApplicationConfig.java`
-   **Konfigurasjon**:
    ```properties
    spring.cache.type=caffeine
    spring.cache.cache-names=aktoerID,folkeregisterIdent,kodeverk
    spring.cache.caffeine.spec.maximumSize=2048
    spring.cache.caffeine.spec.expireAfterWrite=480m
    ```
-   **Eksisterende cache-bruk**: PersondataService, KodeverkRegister

### Consumer/Producer Mønstre

Alle integrasjoner følger samme mønster:

-   **ConsumerProducer**-klasse i `@Configuration`
-   WebClient.Builder med filters:
    -   `GenericAuthFilterFactory.getAzureFilter(clientName)`
    -   `CorrelationIdOutgoingFilter`
    -   `errorFilter("Beskrivelse")`
-   Azure client registreringer i `application.properties`
-   **Eksempler**: `PDLConsumerProducer`, `OppgaveConsumerProducer`, `EessiConsumerProducerConfig`

## Implementasjonsplan

### Fase 1: Grunnleggende Infrastruktur

1. **Opprett Tilgangsmaskinen Consumer og Producer klasser**
    - `TilgangsmaskinenConsumerProducer.kt` i `integrasjon` modul
    - `TilgangsmaskinenConsumer.kt` med WebClient
    - DTOs for request/response
2. **Implementer Azure autentisering**
    - Azure client registrering i `application.properties`
    - Bruk eksisterende `GenericAuthFilterFactory.getAzureFilter()`
3. **Implementer grunnleggende DTOs**
    - `TilgangsmaskinenRequest` med fnr og roller
    - `TilgangsmaskinenResponse` for feilmeldinger
    - Kotlin data classes med norske navn

### Fase 2: Tilgangskontroll Service

1. **Implementer TilgangsmaskinenService klasse (separert fra ABAC)**
    - Egen service for Tilgangsmaskinen-logikk
    - Injiseres i ny TilgangsmaskineAksesskontroll implementasjon
    - Separation of concerns for enkel fjerning av ABAC senere
2. **Integrer med PDL for identmapping**
    - Bruk eksisterende `PersondataService.hentFolkeregisterident()`
    - Håndter mapping fra aktørId til fnr/dnr
3. **Implementer feilhåndtering og logging**
    - Samme audit-logging som ABAC
    - Kast `SikkerhetsbegrensningException` ved manglende tilgang
4. **Legg til caching**
    - 1-times cache for tilgangskontroll
    - Cache-nøkkel basert på bruker + ressurs

### Fase 3: Feature Toggle og Implementasjonsvalg

1. **Implementer feature toggle logikk**
    - Conditional bean creation basert på `AKSESSKONTROLL_TILGANGSMASKINEN`
    - Toggle PÅ = Tilgangsmaskinen, toggle AV = ABAC
2. **Opprett TilgangsmaskineAksesskontroll implementasjon**
    - Ny implementasjon av `Aksesskontroll` interface
    - Bruker `TilgangsmaskinenService` (separation of concerns)
    - Samme interface som eksisterende `AksesskontrollImpl`
3. **Konfigurer conditional beans**
    - `@ConditionalOnProperty` for å velge implementasjon
    - Kun én `Aksesskontroll` bean active om gangen
4. **Sikre bakoverkompatibilitet**
    - Samme interface og feilhåndtering
    - Identisk audit-logging

### Fase 4: Testing og Dokumentasjon

1. **Skriv enhetstester**
    - Mock Tilgangsmaskinen responses
    - Test feature toggle logikk
    - Test feilhåndtering
2. **Skriv integrasjonstester**
    - WireMock for Tilgangsmaskinen
    - Test hele flyten fra aktørId til tilgangskontroll
3. **Oppdater dokumentasjon**
    - Teknisk dokumentasjon
    - Deployment guide

## Identifiserte Risikoer

-   **R001**: Manglende fnr/dnr mapping for enkelte aktørIder
    -   _Mitigering_: Håndter med feilmelding, toggle tilbake til ABAC hvis nødvendig
-   **R002**: Forskjell i autorisasjonslogikk mellom ABAC og Tilgangsmaskinen
    -   _Mitigering_: Grundig testing og gradvis utrulling via feature toggle
-   **R003**: Performance impact av ekstra PDL-kall
    -   _Mitigering_: 1-times caching av PDL-kall
-   **R004**: Cache invalidering ved rolleendringer
    -   _Mitigering_: Kort cache TTL (1 time)
-   **R005**: Tilgangsmaskinen tilgjengelighet
    -   _Mitigering_: Monitoring og mulighet for toggle tilbake til ABAC

## Avhengigheter

-   **Azure AD**: Konfigurert med riktige scopes for Tilgangsmaskinen
-   **PDL**: Tilgjengelig for identmapping
-   **Tilgangsmaskinen**: Produksjonsklar API
-   **Unleash**: Feature toggle `AKSESSKONTROLL_TILGANGSMASKINEN` konfigurert

## Akseptansekriterier

1. Ny tilgangskontroll fungerer identisk med ABAC
2. Performance er tilsvarende eller bedre enn ABAC
3. Feature toggle velger korrekt implementasjon (PÅ=Tilgangsmaskinen, AV=ABAC)
4. Separation of concerns: TilgangsmaskinenService er separert fra ABAC-kode
5. Alle eksisterende tester passerer
6. Feilhåndtering og logging fungerer som før
7. Caching reduserer API-kall til Tilgangsmaskinen og PDL
8. Enkel å fjerne ABAC-kode senere grunnet god separasjon

## Teknisk Gjeld og Forbedringer

-   **Fjerne ABAC-avhengigheter** etter full migrering
-   **Standardisere tilgangskontroll** på tvers av NAV
-   **Forbedre sikkerhet** ved bruk av moderne autentiseringsmekanismer
-   **Forenkle arkitektur** ved å fjerne ABAC-spesifikk kode
