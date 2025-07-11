# Framdrift - Tilgangsmaskinen Integration

## Oppgave ID

MELOSYS-7371 - Erstatte ABAC med Tilgangsmaskinen

## ✅ FERDIG - TILGANGSMASKINEN INTEGRASJON FULLFØRT

**Status**: 🎉 **FULLFØRT MED SIKKERHETSFORBEDRINGER**
**Dato**: 11. januar 2025
**Resultat**: Tilgangsmaskinen-integrasjon implementert med korrekt API og alle sikkerhetsproblemer løst

### Hva ble gjort?

Den opprinnelige implementeringen var basert på feil informasjon om Tilgangsmaskinen API. Etter mottak av korrekt OpenAPI-spesifikasjon ble hele implementeringen oppdatert, og kritiske sikkerhetsproblemer ble identifisert og løst.

**FEIL API (opprinnelig implementert):**

-   Endepunkt: `/api/tilgang/personer`
-   Request: `{"fnr": "string", "roller": ["string"]}`
-   Custom response format

**KORREKT API (nå implementert):**

-   Endepunkt: `/api/v1/kjerne` eller `/api/v1/komplett`
-   Request: Bare `"fnr"` som string
-   Standard RFC7807 ProblemDetail respons format

## 🛡️ Kritiske Sikkerhetsforbedringer Løst

### ✅ Problem 1: Cache Sikkerhetssårbarhet (LØST)

**Problem**: Cache-nøkkel `#fnr + '_' + #regeltype` ville tillate alle innloggede brukere å få tilgang!
**Løsning**: Lagt til bruker-ID i cache-nøkkel:

```kotlin
@Cacheable(value = ["tilgangsmaskinen"], key = "#fnr + '_' + T(no.nav.melosys.sikkerhet.context.SubjectHandler).getInstance().getUserID()")
```

### ✅ Problem 2: Feil Regeltype (LØST)

**Problem**: Brukte varierende regeltyper
**Løsning**: Bruker kun `KOMPLETT_REGELTYPE` for komplett tilgangskontroll

### ✅ Problem 3: Duplikat Caching (LØST)

**Problem**: Privat metode `hentFnrFraPdl` hadde `@Cacheable` som ga konflikter
**Løsning**: Fjernet `@Cacheable` fra privat metode - PersondataService cache er tilstrekkelig

### ✅ Problem 4: Feil Feilmeldinger (LØST)

**Problem**: 404-feil antok "Person ikke funnet i PDL"
**Løsning**: Generisk melding "Ressurs ikke funnet" siden vi ikke kontrollerer Tilgangsmaskinen-feilmeldinger

## 📋 Fullført - API Korreksjon Oppgaver

### ✅ Fase 1: Consumer og Producer (Fullført)

1. **TilgangsmaskinenConsumerProducer.kt** - Oppdatert med korrekt base URL: `https://tilgangsmaskin.intern.nav.no`
2. **TilgangsmaskinenConsumer.kt** - Oppdatert interface med `sjekkTilgang(fnr: String, regeltype: RegelType)`
3. **Nye DTOs basert på korrekt API:**
    - `RegelType.kt` enum (KJERNE_REGELTYPE, KOMPLETT_REGELTYPE, OVERSTYRBAR_REGELTYPE)
    - `TilgangsmaskinenProblemDetail.kt` med RFC7807 format
    - `AvvisningsKode.kt` enum med offisielle koder
    - Oppdatert `TilgangsmaskinenRequest.kt` (forenklet til bare brukerIdent)

### ✅ Fase 2: Implementasjon (Fullført)

4. **TilgangsmaskinenConsumerImpl.kt** - Implementert med:
    - POST til `/api/v1/kjerne` eller `/api/v1/komplett`
    - Sender bare fnr som string i request body
    - Håndterer ProblemDetail format ved 403-respons
    - Korrekt mapping av regeltyper til endepunkter

### ✅ Fase 3: Konfigurasjon (Fullført)

5. **Azure AD konfigurasjon:**

    - Korrekt client registrering i `application.properties`
    - Scope: `api://${NAIS_CLUSTER_NAME}.teamarenanav.${APP_NAME_TILGANGSMASKINEN}/.default`
    - Korrekt URL-konfigurasjon for alle miljøer

6. **Cache konfigurasjon:**
    - Cache navn: `tilgangsmaskinen` (korrigert fra `tilgangsmaskine`)
    - 1-times TTL: `expireAfterWrite=60m`
    - 🛡️ **Sikker cache nøkkel**: `fnr + '_' + brukerID` (beskytter mot tverrgående tilgang)

### ✅ Fase 4: Service og Aksesskontroll (Fullført)

7. **TilgangsmaskinenService.kt** - Implementert med sikkerhetsforbedringer:

    - Bruker kun `KOMPLETT_REGELTYPE` for full tilgangskontroll
    - 🛡️ **Bruker-spesifikk cache**: Hindrer tverrgående tilgang mellom brukere
    - Metodesignatur: `sjekkTilgangTilFnr(fnr: String)` og `sjekkTilgangTilAktørId(aktørId: String)`
    - Bruker eksisterende PDL-cache for aktørId → fnr mapping (uten duplikat)

8. **TilgangsmaskinenAksesskontroll.kt** - Ferdig implementert:

    - Implementerer samme `Aksesskontroll` interface som ABAC
    - Audit logging bevart
    - Skrivetilgang og tilordning-logikk bevart
    - Identisk oppførsel som `AksesskontrollImpl.java`

9. **TilgangsmaskinenAksesskontrollConfig.kt** - Feature toggle konfigurasjon:
    - Velger mellom `TilgangsmaskinenAksesskontroll` og `AksesskontrollImpl` (ABAC)
    - Basert på `ToggleName.AKSESSKONTROLL_TILGANGSMASKINEN`
    - Graceful fallback til ABAC hvis toggle er deaktivert

### ✅ Fase 5: Testing (Fullført)

10. **TilgangsmaskinenConsumerImplTest.kt** - Comprehensive unit tests:
    -   Test av alle regeltyper og endepunkter
    -   Test av 204 (suksess), 403 (avslag), 404/500 (feil) responser
    -   Test av ProblemDetail parsing
    -   Test av request format og endepunkt-mapping

## 🛠️ Tekniske Detaljer

### API Endepunkter

-   **Kjerne regelsett**: `POST /api/v1/kjerne` (grunnleggende kontroller)
-   **Komplett regelsett**: `POST /api/v1/komplett` (anbefalt for Melosys - brukes nå)
-   **Overstyrbar regelsett**: `POST /api/v1/komplett` (fallback til komplett)

### Request Format

```http
POST /api/v1/komplett
Content-Type: application/json

"12345678901"
```

### Response Format

**Suksess (204 No Content):**

-   Ingen response body
-   Tilgang innvilget

**Avslag (403 Forbidden):**

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

### Avvisningskoder

Støttede avvisningskoder i `title` feltet:

-   `AVVIST_STRENGT_FORTROLIG_ADRESSE`
-   `AVVIST_STRENGT_FORTROLIG_UTLAND`
-   `AVVIST_AVDØD`
-   `AVVIST_PERSON_UTLAND`
-   `AVVIST_SKJERMING`
-   `AVVIST_FORTROLIG_ADRESSE`
-   `AVVIST_UKJENT_BOSTED`
-   `AVVIST_GEOGRAFISK`
-   `AVVIST_HABILITET`

## 🎯 Klar for Produksjonssetting

### ✅ Forutsetninger for Testing (Ferdig)

1. ✅ **Feature toggle**: `AKSESSKONTROLL_TILGANGSMASKINEN` konfigurert og klar
2. ✅ **Azure AD**: App registrering konfigurert med riktig scope
3. ✅ **Sikkerhet**: Alle sikkerhetsproblemer løst
4. ✅ **Build**: Kompilerer uten feil
5. ✅ **Testing**: Enhetstester implementert

### Anbefalte Utrulling Steg

1. **Q1/Q2**: Aktiver feature toggle og test grunnleggende tilgangskontroll
2. **Integrasjonstester**: Kjør fullstendige integrasjonstester
3. **Ytelsestesting**: Verifiser cache-oppførsel og response-tider
4. **Prod**: Gradvis utrulling med A/B testing

### Miljøvariabler som Trengs

```properties
# Tilgangsmaskinen konfigurasjon
TILGANGSMASKINEN_URL=https://tilgangsmaskin.intern.nav.no
APP_NAME_TILGANGSMASKINEN=tilgangsmaskinen

# Feature toggle
# Settes i Unleash UI: AKSESSKONTROLL_TILGANGSMASKINEN=true/false
```

## 💡 Recommendations / Future Work

### Forbedringer

1. **Monitoring**: Legg til metrics for Tilgangsmaskinen-kall (responstid, suksessrate, cache hit rate)
2. **Alerting**: Sett opp alerting for høy feilrate eller lange responstider
3. **Graceful degradation**: Vurder fallback til ABAC ved Tilgangsmaskinen-feil
4. **Cache tuning**: Monitorer cache-oppførsel og juster TTL om nødvendig
5. **Service tests**: Legg til tester for TilgangsmaskinenService og TilgangsmaskinenAksesskontroll

### ABAC Utfasing (Etter Produksjonssuksess)

1. **Slett ABAC-kode**: Etter vellykket produksjonsdrift kan ABAC-implementeringen fjernes
2. **Feature toggle cleanup**: Fjern toggle-logikk og gjør TilgangsmaskinenAksesskontroll permanent
3. **Dependency cleanup**: Fjern ABAC-dependencies fra pom.xml

### Teknisk Gjeld

1. **Kotlin migrering**: Vurder å migrere resten av tilgangskontroll-koden til Kotlin
2. **Test coverage**: Legg til flere integrasjonstester med reelle Tilgangsmaskinen-scenarios
3. **Documentation**: Oppdater team-dokumentasjon med nye tilgangskontroll-flyt

## ⚠️ Viktige Avgjørelser

### Sikkerhet Avgjørelser

-   **Cache sikkerhet**: Bruker-spesifikke cache-nøkler for å hindre tverrgående tilgang
-   **Regeltype**: Kun KOMPLETT_REGELTYPE for maksimal sikkerhet
-   **Feilhåndtering**: Generiske feilmeldinger for å ikke avsløre systemdetaljer
-   **Graceful degradation**: Fallback til ABAC ved deaktivering av feature toggle

### Arkitektur Avgjørelser

-   **Separasjon av bekymringer**: TilgangsmaskinenService håndterer kun API-kommunikasjon
-   **Cache strategi**: Kombinert cache på fnr+brukerID for optimal ytelse og sikkerhet
-   **Feilhåndtering**: Samme exception-typer som ABAC for kompatibilitet
-   **Feature toggle**: Runtime-valg mellom ABAC og Tilgangsmaskinen uten restart

### Implementasjons Avgjørelser

-   **Default regeltype**: KOMPLETT_REGELTYPE for komplett tilgangskontroll
-   **PDL integration**: Gjenbruker eksisterende cache og feilhåndtering
-   **Request format**: Bare fnr som string, ingen rolle-spesifisering
-   **Response parsing**: Graceful fallback ved ProblemDetail parsing-feil

---

**Status**: ✅ **FERDIG OG SIKRET** - Tilgangsmaskinen-integrasjon klar for testing og produksjonssetting med alle sikkerhetsproblemer løst
