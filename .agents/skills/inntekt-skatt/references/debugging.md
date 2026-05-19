# Inntekt og Skatt Debugging

## Common Issues

### 1. No Income Data Returned

**Symptom**: Empty income response for a period

**Possible Causes**:
- Period before January 2015 (no data available)
- Wrong aktørId used
- Inntektskomponenten service unavailable

**Debug Steps**:
```sql
-- Check stored income saksopplysning
SELECT s.id, s.type, s.versjon, s.opprettet_tid,
       LENGTH(s.dokument) as doc_size
FROM saksopplysning s
WHERE s.behandling_id = :behandlingId
AND s.type = 'INNTK';

-- Check if any income data exists in document
SELECT s.id,
       JSON_VALUE(s.dokument, '$.arbeidsInntektMaanedListe[0].aarMaaned') as first_month,
       JSON_QUERY(s.dokument, '$.arbeidsInntektMaanedListe') as months
FROM saksopplysning s
WHERE s.behandling_id = :behandlingId
AND s.type = 'INNTK';
```

### 2. Skattehendelse Not Processed

**Symptom**: Tax event received but no årsavregning created

**Possible Causes**:
- Feature toggle `MELOSYS_SKATTEHENDELSE_CONSUMER` disabled
- No fagsak with trygdeavgift for this person
- Person doesn't have aktørId in any fagsak

**Debug Steps**:
```sql
-- Find fagsaker for person with trygdeavgift
SELECT f.id, f.gsak_saksnr, f.aktor_id,
       tap.id as avgiftsperiode_id, tap.aar
FROM fagsak f
JOIN behandling b ON b.fagsak_id = f.id
JOIN behandlingsresultat br ON br.behandling_id = b.id
JOIN trygdeavgiftsperiode tap ON tap.behandlingsresultat_id = br.id
WHERE f.aktor_id = :aktorId
ORDER BY tap.aar DESC;

-- Check if årsavregning already exists for year
SELECT b.id, b.status, b.behandlingstype, br.aar
FROM behandling b
JOIN behandlingsresultat br ON br.behandling_id = b.id
WHERE b.fagsak_id = :fagsakId
AND b.behandlingstype = 'AARSAVREGNING'
AND br.aar = :year;
```

### 3. Income Amounts Mismatch

**Symptom**: Calculated trygdeavgift doesn't match expected based on income

**Debug Steps**:
```sql
-- Get all income entries for behandling
SELECT
    JSON_VALUE(inntekt.value, '$.inntektType') as type,
    JSON_VALUE(inntekt.value, '$.beloep') as beloep,
    JSON_VALUE(inntekt.value, '$.utbetaltIMaaned') as maaned,
    JSON_VALUE(inntekt.value, '$.virksomhet.identifikator') as arbeidsgiver
FROM saksopplysning s,
     JSON_TABLE(s.dokument, '$.arbeidsInntektMaanedListe[*]'
         COLUMNS (
             NESTED PATH '$.arbeidsInntektInformasjon.inntektListe[*]'
             COLUMNS (
                 value FORMAT JSON PATH '$'
             )
         )
     ) inntekt
WHERE s.behandling_id = :behandlingId
AND s.type = 'INNTK';
```

### 4. Wrong Earning Country

**Symptom**: Income attributed to wrong country

**Debug Steps**:
```sql
-- Check opptjeningsland in income
SELECT
    JSON_VALUE(inntekt.value, '$.opptjeningsland') as land,
    JSON_VALUE(inntekt.value, '$.beloep') as beloep
FROM saksopplysning s,
     JSON_TABLE(s.dokument, '$.arbeidsInntektMaanedListe[*]'
         COLUMNS (
             NESTED PATH '$.arbeidsInntektInformasjon.inntektListe[*]'
             COLUMNS (
                 value FORMAT JSON PATH '$'
             )
         )
     ) inntekt
WHERE s.behandling_id = :behandlingId
AND s.type = 'INNTK'
AND JSON_VALUE(inntekt.value, '$.opptjeningsland') IS NOT NULL;
```

## SQL Queries

### Find Recent Income Lookups

```sql
SELECT s.id, s.behandling_id, s.opprettet_tid, s.versjon,
       f.gsak_saksnr
FROM saksopplysning s
JOIN behandling b ON s.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE s.type = 'INNTK'
AND s.opprettet_tid > SYSDATE - 7
ORDER BY s.opprettet_tid DESC;
```

### Find Årsavregning Behandlinger

```sql
SELECT b.id, b.status, b.opprettet_tid,
       br.aar, br.beregnet_avgift_beloep,
       f.gsak_saksnr
FROM behandling b
JOIN behandlingsresultat br ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE b.behandlingstype = 'AARSAVREGNING'
ORDER BY b.opprettet_tid DESC
FETCH FIRST 20 ROWS ONLY;
```

### Trace Årsavregning Prosessinstans

```sql
SELECT pi.id, pi.status, ps.steg, ps.status as steg_status,
       ps.opprettet_tid, ps.feilmelding
FROM prosessinstans pi
JOIN prosess_steg ps ON ps.prosessinstans_id = pi.id
WHERE pi.behandling_id = :behandlingId
AND pi.prosesstype = 'AARSAVREGNING_BEHANDLING'
ORDER BY ps.opprettet_tid;
```

### Check Skattehendelse Processing Status

```sql
-- Requires access to application logs since events are not persisted to DB
-- In Kibana: app:melosys-api AND "SkattehendelserConsumer" AND aktørId
```

## Integration Points

### Inntektskomponenten API

```
Base URL: ${melosys.integrasjon.inntekt.url}
Endpoint: POST /api/v1/hentinntektliste
Auth: Azure AD token (scope: api://[env].inntektskomponenten/.default)

Request:
{
  "ident": {"identifikator": "aktørId", "aktoerType": "AKTOER_ID"},
  "ainntektsfilter": "MedlemskapA-inntekt",
  "formaal": "Medlemskap",
  "maanedFom": "2024-01",
  "maanedTom": "2024-12"
}
```

### Kafka Consumer

```kotlin
// Topic configuration
kafka.aiven.skattehendelser.topic = melosys.skattehendelser

// Consumer group
spring.kafka.consumer.group-id = melosys-api-skattehendelser
```

## Logging

Key log messages to search for:

```
# Inntekt lookup
"Henter inntekt for aktør"
"Inntekt hentet for periode"

# Skattehendelse processing
"Mottatt skattehendelse"
"Oppretter årsavregning for fagsak"
"Fant ingen fagsaker med trygdeavgift for aktør"
```

## Feature Toggles

| Toggle | Purpose |
|--------|---------|
| `MELOSYS_SKATTEHENDELSE_CONSUMER` | Enable/disable skattehendelse Kafka consumer |

## Configuration Properties

```properties
# Inntektskomponenten
melosys.integrasjon.inntekt.url=https://inntektskomponenten.intern.nav.no

# Kafka skattehendelser
kafka.aiven.skattehendelser.topic=melosys.skattehendelser
spring.kafka.consumer.group-id=melosys-api-skattehendelser
```

## Related Skills

- **trygdeavgift**: Uses income for avgift calculation and årsavregning
- **saksflyt**: AARSAVREGNING_BEHANDLING saga
- **behandlingsresultat**: Stores årsavregning results
