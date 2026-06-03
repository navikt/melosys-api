---
name: inntekt-skatt
description: |
  Expert knowledge of income and tax integrations in melosys-api.
  Use when: (1) Understanding Inntektskomponenten/A-inntekt lookups,
  (2) Debugging skattehendelser Kafka processing,
  (3) Understanding årsavregning triggered by skatteoppgjør,
  (4) Investigating income data for trygdeavgift calculations.
---

# Inntekt og Skatt Skill

Expert knowledge of income data (Inntektskomponenten) and tax event (skattehendelser) integrations.

## Quick Reference

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `InntektService` | `integrasjon/src/main/kotlin/.../inntekt/` | Fetches income data from Inntektskomponenten |
| `InntektClient` | `integrasjon/src/main/kotlin/.../inntekt/` | WebClient REST client for Inntektskomponenten API (`POST /hentinntektliste`) |
| `InntektClientConfig` | `integrasjon/src/main/kotlin/.../inntekt/` | Configures the `inntektClient` bean (base URL, Azure auth filter) |
| `InntektKonverter` | `integrasjon/src/main/kotlin/.../inntekt/` | Converts API response to domain model |
| `SkattehendelserConsumer` | `service/src/main/kotlin/.../aarsavregning/` | Kafka consumer for tax events |
| `ÅrsavregningService` | `service/src/main/kotlin/.../aarsavregning/` | Annual reconciliation processing |

### Data Sources

| Source | Service | Data | Use Case |
|--------|---------|------|----------|
| **Inntektskomponenten** | A-inntekt REST API | Monthly income by employer | Saksopplysninger, trygdeavgift |
| **Skattehendelser** | melosys-skattehendelser Kafka | Tax settlement notifications | Trigger årsavregning |

## Inntektskomponenten Integration

### API Request

```kotlin
InntektRequest(
    ainntektsfilter = "MedlemskapA-inntekt",
    formaal = "Medlemskap",
    ident = Aktoer(aktørId, AktoerType.AKTOER_ID),
    maanedFom = YearMonth.of(2024, 1),
    maanedTom = YearMonth.of(2024, 12)
)
```

### Configuration

```yaml
# application-nais.yml binds the base URL from an env var
inntekt:
  rest:
    url: ${INNTEKT_REST_V1_ENDPOINTURL}
# e.g. q2: https://team-inntekt-proxy.dev-fss-pub.nais.io/proxy/inntektskomponenten-q2/rs/api/v1
```

### Data Limitations

- Data available from **January 2015** onwards
- Queries before 2015 return empty response automatically
- Uses filter `MedlemskapA-inntekt` for membership-relevant income

### Income Types

| Type | Description |
|------|-------------|
| `LOENNSINNTEKT` | Salary/wages |
| `NAERINGSINNTEKT` | Self-employment income |
| `PENSJON_ELLER_TRYGD` | Pension or social security |
| `YTELSE_FRA_OFFENTLIGE` | Public benefits |

### Key Fields in Response

```kotlin
data class Inntekt(
    val inntektType: InntektType,
    val beloep: BigDecimal,           // Amount
    val utbetaltIMaaned: YearMonth,   // Payment month
    val opptjeningsland: String?,     // Earning country (ISO-2)
    val opptjeningsperiodeFom: LocalDate?,
    val opptjeningsperiodeTom: LocalDate?,
    val skattemessigBosattLand: String?,  // Tax residence
    val virksomhet: Aktoer?,          // Employer org number
    val beskrivelse: String?          // Income description
)
```

## Skattehendelser Integration

### Kafka Configuration

```kotlin
@KafkaListener(
    topics = ["\${kafka.aiven.skattehendelser.topic}"],
    containerFactory = "aivenSkattehendelserListenerContainerFactory"
)
```

### Skattehendelse Message

```kotlin
data class Skattehendelse(
    val gjelderPeriode: String,  // Year (e.g., "2024"), parsed with .toInt()
    val identifikator: String,   // Person identifier (used as aktørId in fagsak lookup)
    val hendelsetype: String,
)
```

### Processing Flow

```
1. melosys-skattehendelser receives tax settlement from Skatteetaten
   ↓
2. Publishes event to Kafka topic
   ↓
3. SkattehendelserConsumer receives event
   ↓
4. Finds all fagsaker with trygdeavgift for this person/year
   ↓
5. For each sak: creates årsavregning behandling if needed
   ↓
6. Prosessinstans with prosess_type OPPRETT_NY_BEHANDLING_AARSAVREGNING starts
   (later IVERKSETT_VEDTAK_AARSAVREGNING iverksetter vedtaket)
```

### Feature Toggle

```kotlin
if (unleash.isEnabled(ToggleName.MELOSYS_SKATTEHENDELSE_CONSUMER)) {
    // Process skattehendelse
}
```

## Domain Model

### Saksopplysning for Income

```kotlin
Saksopplysning().apply {
    type = SaksopplysningType.INNTK
    kildesystem = SaksopplysningKildesystem.INNTK
    versjon = "REST 1.0"
    dokument = InntektDokument().apply {
        arbeidsInntektMaanedListe = [...]
    }
}
```

### InntektDokument Structure

```
InntektDokument
├── arbeidsInntektMaanedListe: List<ArbeidsInntektMaaned>
    ├── aarMaaned: YearMonth
    ├── avvikListe: List<Avvik>
    └── arbeidsInntektInformasjon
        ├── inntektListe: List<Inntekt>
        └── arbeidsforholdListe: List<ArbeidsforholdFrilanser>
```

## Common Debugging

See [references/debugging.md](references/debugging.md) for symptom-based debug
steps, log messages to search for, and the full set of diagnostic SQL queries.

### Check Stored Income Data

```sql
-- Find income saksopplysning for behandling.
-- Physical columns are opplysning_type and registrert_dato (the JPA field is `type`).
SELECT s.id, s.opplysning_type, s.versjon, s.registrert_dato, s.dokument
FROM saksopplysning s
WHERE s.behandling_id = :behandlingId
AND s.opplysning_type = 'INNTK';

-- Parse income from JSON
SELECT id,
       JSON_VALUE(dokument, '$.arbeidsInntektMaanedListe[0].aarMaaned') as first_month
FROM saksopplysning
WHERE opplysning_type = 'INNTK' AND behandling_id = :behandlingId;
```

### Trace Skattehendelse Processing

```sql
-- Find årsavregning created from skattehendelse.
-- behandling joins fagsak via saksnummer; behandling type is beh_type ('ÅRSAVREGNING').
-- The reconciliation year (aar) lives in the aarsavregning table,
-- keyed by behandlingsresultat_id (= behandlingsresultat.behandling_id).
SELECT b.id, b.status, b.beh_type, aa.aar
FROM behandling b
JOIN behandlingsresultat br ON br.behandling_id = b.id
JOIN aarsavregning aa ON aa.behandlingsresultat_id = br.behandling_id
WHERE b.saksnummer = :saksnummer
AND b.beh_type = 'ÅRSAVREGNING'
ORDER BY b.registrert_dato DESC;
```

### Check Prosessinstans for Årsavregning

```sql
-- prosessinstans stores prosess_type and the current steg inline (PK is uuid).
-- prosess_steg is just a (kode, navn) lookup table.
SELECT pi.uuid, pi.prosess_type, pi.steg, pi.endret_dato
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.prosess_type IN ('OPPRETT_NY_BEHANDLING_AARSAVREGNING', 'IVERKSETT_VEDTAK_AARSAVREGNING')
ORDER BY pi.endret_dato DESC;
```

## Integration with Trygdeavgift

Income data feeds the avgift calculation in two contexts:
1. **Foreløpig/forskudd beregning** - Estimate avgift based on expected income
2. **Årsavregning** - Final calculation based on actual income from skatteoppgjør

Within both contexts, two calculation rules can cap or replace the ordinary
rate-based amount (folketrygdloven § 23-3 fjerde ledd):
- **25%-regelen** - The avgift cannot exceed 25% of the part of income that
  exceeds a minstebeløp; if that is lower than ordinary `sats × inntekt`, the
  25% result is used. (`Avgiftsberegningstype.TJUEFEM_PROSENT_REGEL`.)
- **Minstebeløp** - Income below the minstebeløp gives no avgift
  (`Avgiftsberegningstype.MINSTEBELOEP`).

> Note: this trygdeavgift 25%-regel is unrelated to the lovvalg 25%-rule
> (forordning 987/2009 art. 14 nr. 8, "vesentlig del" of work) despite the shared name.

Income from a behandling is read via `Behandling.hentInntektDokument()`;
`InntektService.hentInntektListe(personID, fom, tom)` fetches fresh data from
Inntektskomponenten and returns a `Saksopplysning`.

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `teammelosys.skattehendelser.v1` | melosys-skattehendelser | SkattehendelserConsumer (group `teammelosys.skattehendelser-consumer`) | Tax settlement notifications |

## External Systems

### Inntektskomponenten
- REST API for A-inntekt (income reported by employers)
- Part of NAV's income registry
- Documentation: Internal NAV confluence

### melosys-skattehendelser
- Separate service that polls Skatteetaten for tax events
- Uses Sigrun/Skatteetaten APIs internally
- Publishes simplified events to Kafka for melosys-api

## Related Skills

- **trygdeavgift**: Uses income for avgift calculation
- **behandlingsresultat**: Stores årsavregning results
- **saksflyt**: Årsavregning saga processing

## External Documentation

- [Informasjonsbehov trygdeavgift](https://confluence.adeo.no/spaces/TEESSI/pages/456855459) - Data sources for tax calculations
