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
-- Check stored income saksopplysning.
-- Physical columns are opplysning_type and registrert_dato (the JPA field is `type`).
SELECT s.id, s.opplysning_type, s.versjon, s.registrert_dato,
       LENGTH(s.dokument) as doc_size
FROM saksopplysning s
WHERE s.behandling_id = :behandlingId
AND s.opplysning_type = 'INNTK';

-- Check if any income data exists in document
SELECT s.id,
       JSON_VALUE(s.dokument, '$.arbeidsInntektMaanedListe[0].aarMaaned') as first_month,
       JSON_QUERY(s.dokument, '$.arbeidsInntektMaanedListe') as months
FROM saksopplysning s
WHERE s.behandling_id = :behandlingId
AND s.opplysning_type = 'INNTK';
```

### 2. Skattehendelse Not Processed

**Symptom**: Tax event received but no årsavregning created

**Possible Causes**:
- Feature toggle `MELOSYS_SKATTEHENDELSE_CONSUMER` disabled
- No fagsak with trygdeavgift for this person
- Person doesn't have aktørId in any fagsak

**Debug Steps**:
```sql
-- Find årsavregning fagsaker/behandlinger for a sak.
-- fagsak PK is saksnummer; behandling joins via saksnummer; type is beh_type.
-- The reconciliation year (aar) is in the aarsavregning table,
-- keyed by behandlingsresultat_id (= behandlingsresultat.behandling_id).
SELECT f.saksnummer, f.gsak_saksnummer,
       b.id as behandling_id, b.status, aa.aar
FROM fagsak f
JOIN behandling b ON b.saksnummer = f.saksnummer
JOIN behandlingsresultat br ON br.behandling_id = b.id
JOIN aarsavregning aa ON aa.behandlingsresultat_id = br.behandling_id
WHERE b.beh_type = 'ÅRSAVREGNING'
AND f.saksnummer = :saksnummer
ORDER BY aa.aar DESC;

-- Check if årsavregning already exists for year
SELECT b.id, b.status, b.beh_type, aa.aar
FROM behandling b
JOIN behandlingsresultat br ON br.behandling_id = b.id
JOIN aarsavregning aa ON aa.behandlingsresultat_id = br.behandling_id
WHERE b.saksnummer = :saksnummer
AND b.beh_type = 'ÅRSAVREGNING'
AND aa.aar = :year;
```

> Note: the person/aktør lookup is done in code via `FagsakService.hentFagsakerMedAktør`
> (the aktør lives in the `aktoer` table, not as a column on `fagsak`).

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
AND s.opplysning_type = 'INNTK';
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
AND s.opplysning_type = 'INNTK'
AND JSON_VALUE(inntekt.value, '$.opptjeningsland') IS NOT NULL;
```

## SQL Queries

### Find Recent Income Lookups

```sql
SELECT s.id, s.behandling_id, s.registrert_dato, s.versjon,
       f.gsak_saksnummer
FROM saksopplysning s
JOIN behandling b ON s.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE s.opplysning_type = 'INNTK'
AND s.registrert_dato > SYSDATE - 7
ORDER BY s.registrert_dato DESC;
```

### Find Årsavregning Behandlinger

```sql
-- aar and beregnet_avgift_belop live in the aarsavregning table
-- (keyed by behandlingsresultat_id = behandlingsresultat.behandling_id).
SELECT b.id, b.status, b.registrert_dato,
       aa.aar, aa.beregnet_avgift_belop,
       f.gsak_saksnummer
FROM behandling b
JOIN behandlingsresultat br ON br.behandling_id = b.id
JOIN aarsavregning aa ON aa.behandlingsresultat_id = br.behandling_id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE b.beh_type = 'ÅRSAVREGNING'
ORDER BY b.registrert_dato DESC
FETCH FIRST 20 ROWS ONLY;
```

### Trace Årsavregning Prosessinstans

```sql
-- prosessinstans (PK uuid) stores prosess_type and the last completed step inline.
-- prosessinstans_hendelser holds the per-step history (steg, type, melding).
SELECT pi.uuid, pi.prosess_type, pi.sist_fullfort_steg AS naavaerende_steg,
       h.steg AS hendelse_steg, h.type, h.melding, h.registrert_dato
FROM prosessinstans pi
JOIN prosessinstans_hendelser h ON h.prosessinstans_id = pi.uuid
WHERE pi.behandling_id = :behandlingId
AND pi.prosess_type IN ('OPPRETT_NY_BEHANDLING_AARSAVREGNING', 'IVERKSETT_VEDTAK_AARSAVREGNING')
ORDER BY h.registrert_dato;
```

### Check Skattehendelse Processing Status

```sql
-- Requires access to application logs since events are not persisted to DB
-- In Kibana: app:melosys-api AND "SkattehendelserConsumer" AND aktørId
```

## Integration Points

### Inntektskomponenten API

```
Base URL: ${inntekt.rest.url}  (from env INNTEKT_REST_V1_ENDPOINTURL; base already ends in /rs/api/v1)
Endpoint: POST /hentinntektliste
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
// Topic configuration (default value)
kafka.aiven.skattehendelser.topic = teammelosys.skattehendelser.v1

// Consumer group
kafka.aiven.skattehendelser.groupid = teammelosys.skattehendelser-consumer
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

```yaml
# Inntektskomponenten (application-nais.yml)
inntekt:
  rest:
    url: ${INNTEKT_REST_V1_ENDPOINTURL}

# Kafka skattehendelser (application-nais.yml)
kafka:
  aiven:
    skattehendelser:
      groupid: teammelosys.skattehendelser-consumer
      topic: ${KAFKA_AIVEN_SKATTEHENDELSER:teammelosys.skattehendelser.v1}
```

## Related Skills

- **trygdeavgift**: Uses income for avgift calculation and årsavregning
- **saksflyt**: OPPRETT_NY_BEHANDLING_AARSAVREGNING / IVERKSETT_VEDTAK_AARSAVREGNING saga
- **behandlingsresultat**: Stores årsavregning results
