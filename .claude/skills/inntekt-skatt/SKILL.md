---
skill: inntekt-skatt
description: Income lookup (Inntektskomponenten) and tax events (skattehendelser) integration
triggers:
  - inntekt
  - income
  - skattehendelser
  - tax events
  - skatteoppgjør
  - Inntektskomponenten
  - A-inntekt
  - årsavregning inntekt
---

# Inntekt og Skatt Skill

Expert knowledge of income data (Inntektskomponenten) and tax event (skattehendelser) integrations.

## Quick Reference

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `InntektService` | `integrasjon/src/main/kotlin/.../inntekt/` | Fetches income data from Inntektskomponenten |
| `InntektRestConsumer` | `integrasjon/src/main/kotlin/.../inntekt/` | REST client for Inntektskomponenten API |
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

```properties
melosys.integrasjon.inntekt.url=https://inntektskomponenten.intern.nav.no
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
    val identifikator: String,  // AktørID
    val gjelderPeriode: String  // Year (e.g., "2024")
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
4. Finds all fagsaker with trygdeavgift for this person
   ↓
5. For each sak: creates årsavregning behandling if needed
   ↓
6. Prosessinstans for AARSAVREGNING_BEHANDLING starts
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

### Check Stored Income Data

```sql
-- Find income saksopplysning for behandling
SELECT s.id, s.type, s.versjon, s.opprettet_tid, s.dokument
FROM saksopplysning s
WHERE s.behandling_id = :behandlingId
AND s.type = 'INNTK';

-- Parse income from JSON
SELECT id,
       JSON_VALUE(dokument, '$.arbeidsInntektMaanedListe[0].aarMaaned') as first_month
FROM saksopplysning
WHERE type = 'INNTK' AND behandling_id = :behandlingId;
```

### Trace Skattehendelse Processing

```sql
-- Find årsavregning created from skattehendelse
SELECT b.id, b.status, b.aarsakstype, br.aar
FROM behandling b
JOIN behandlingsresultat br ON br.behandling_id = b.id
WHERE b.fagsak_id = :fagsakId
AND b.behandlingstype = 'AARSAVREGNING'
ORDER BY b.opprettet_tid DESC;
```

### Check Prosessinstans for Årsavregning

```sql
SELECT pi.id, pi.status, pi.prosesstype, ps.steg
FROM prosessinstans pi
JOIN prosess_steg ps ON ps.prosessinstans_id = pi.id
WHERE pi.behandling_id = :behandlingId
AND pi.prosesstype = 'AARSAVREGNING_BEHANDLING'
ORDER BY ps.opprettet_tid;
```

## Integration with Trygdeavgift

Income data is used for:
1. **Foreløpig beregning** - Estimate avgift based on expected income
2. **Årsavregning** - Final calculation based on actual income from skatteoppgjør
3. **25% rule** - Check if foreign income is less than 25% of total

```kotlin
// TrygdeavgiftsberegningService uses income for calculations
val inntekt = saksopplysningService.hentInntekt(behandlingId)
```

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `melosys.skattehendelser` | melosys-skattehendelser | SkattehendelserConsumer | Tax settlement notifications |

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
