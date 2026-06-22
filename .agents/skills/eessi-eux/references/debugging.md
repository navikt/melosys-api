# EESSI Debugging Guide

## SQL Queries

> **Important:** melosys-api does NOT store SEDs or the BUC↔NAV-case relationship in its
> own Oracle DB. There is no `sed_dokument` table, and no `saksrelasjon` table —
> `saksrelasjon` exists here only as `SaksrelasjonDto` and as `PROSESS_STEG` name
> strings (`OPPDATER_SAKSRELASJON`). The saksrelasjon data lives in the **melosys-eessi**
> service's own database. From melosys-api, trace SED activity through the saga tables
> below, and look up the BUC↔case relationship at runtime via
> `EessiClient.hentSakForGsakSaksnummer` / `hentSakForRinasaksnummer`.

### Prosessinstans for SED Operations

The `Prosessinstans` entity (`saksflyt-api/.../domain/Prosessinstans.kt`) maps to table
`prosessinstans`. NB column names: `prosess_type` (not `type`), `sist_fullfort_steg`
(not `sist_utforte_steg`), `registrert_dato`/`endret_dato` (no `opprettet_tidspunkt`),
`behandling_id` (FK), and the PK is `uuid`. Step errors are recorded in
`prosessinstans_hendelser`, not in a column on `prosessinstans`.

```sql
-- SED sending saga runs for a behandling
SELECT
    pi.uuid,
    pi.prosess_type,
    pi.status,
    pi.sist_fullfort_steg,
    pi.endret_dato
FROM prosessinstans pi
JOIN behandling b ON pi.behandling_id = b.id
WHERE b.id = :behandlingId
AND pi.prosess_type LIKE 'IVERKSETT_VEDTAK%'
ORDER BY pi.endret_dato DESC;

-- SED mottak saga runs (last 7 days)
-- SED-mottak prosess_type values include ARBEID_FLERE_LAND_NY_SAK,
-- ANMODNING_OM_UNNTAK_MOTTAK_NY_SAK, REGISTRERING_UNNTAK_NY_SAK, etc.
SELECT
    pi.uuid,
    pi.prosess_type,
    pi.status,
    pi.sist_fullfort_steg,
    pi.endret_dato
FROM prosessinstans pi
WHERE pi.prosess_type LIKE '%MOTTAK%'
AND pi.endret_dato > SYSDATE - 7
ORDER BY pi.endret_dato DESC;

-- Error details for a failed saga run live in prosessinstans_hendelser
SELECT h.registrert_dato, h.steg, h.type, h.melding
FROM prosessinstans_hendelser h
WHERE h.prosessinstans_id = :prosessinstansUuid
ORDER BY h.registrert_dato DESC;
```

## Common Error Scenarios

### 1. SED Not Sent

**Symptom**: Vedtak iverksatt but no SED sent to other country.

**Check**:
```sql
-- Is mottakerinstitusjoner set?
SELECT pi.data
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.prosess_type LIKE 'IVERKSETT_VEDTAK%';
-- Look for EESSI_MOTTAKERE in data JSON
```

**Common Causes**:
- No mottakerinstitusjoner selected (falls back to brev)
- Country not EESSI-ready for BUC type
- EessiService error during send

### 2. Wrong BUC Type

**Symptom**: SED sent but with wrong BUC.

**Check**:
```kotlin
// BucType.fraBestemmelse() logic
val bucType = BucType.fraBestemmelse(bestemmelse)
```

**Common Causes**:
- Bestemmelse not mapped in BucType.kt
- Wrong bestemmelse set on behandlingsresultat

### 3. Institution Not Found

**Symptom**: `FunksjonellException: Finner ingen gyldig mottakerinstitusjon`

**Check**:
```kotlin
// In EessiService.validerOgAvklarMottakerInstitusjonerForBuc()
val institusjoner = hentEessiMottakerinstitusjoner(bucType, landkoder)
```

**Common Causes**:
- Country not connected to EESSI
- Wrong land code
- BUC type not supported by country

### 4. Incoming SED Not Processed

**Symptom**: Kafka message received but no behandling created.

**Check**:
```bash
# Check Kafka consumer logs
grep "EessiMeldingConsumer" application.log | grep -E "ERROR|WARN|Mottatt"

# Check prosessinstans for SED_MOTTAK
```

**Common Causes**:
- SED type not supported
- Missing saksrelasjon
- Routing error in SedRuter

### 5. BUC Already Closed

**Symptom**: Cannot send SED on existing BUC.

**Check**:
```kotlin
val erÅpen = eessiService.erBucAapen(arkivsakID)
```

**Common Causes**:
- BUC was closed in RINA
- Timeout reached
- Manual closure

## Log Patterns

```bash
# SED sending
grep "EessiService" application.log | grep -E "opprettOgSendSed|sendSed"

# BUC operations
grep "Buc opprettet med id" application.log

# Kafka consumption
grep "EessiMeldingConsumer" application.log | grep "Mottatt ny melding"

# SED routing
grep "SedRuter" application.log

# EUX API errors
grep "EessiClient" application.log | grep -E "ERROR|Exception"
```

## Troubleshooting Flowchart

```
SED sending failed?
├── Check prosessinstans status
│   └── FEILET → Check prosessinstans_hendelser (steg + melding) for the error
├── Check mottakerinstitusjoner in prosessdata
│   └── Empty → Country not EESSI-ready or no recipients selected
├── Check BucType mapping
│   └── Wrong BUC → Check bestemmelse on behandlingsresultat
├── Check EUX API response
│   └── Error → Check melosys-eessi logs
└── Check BUC status
    └── Closed → Cannot send more SEDs

Incoming SED not processed?
├── Check Kafka consumer received message
│   └── No → Check Kafka connectivity
├── Check SedRuter logged routing
│   └── No router → SED type not supported
├── Check prosessinstans created
│   └── No → Error during prosessinstans creation
└── Check behandling created
    └── No → Check SedRuter logic
```

## Code Entry Points

| Scenario | Entry Point |
|----------|-------------|
| Send SED on vedtak | `AbstraktSendUtland.sendUtland()` |
| Create BUC and SED | `EessiService.opprettOgSendSed()` |
| Receive SED (Kafka) | `EessiMeldingConsumer.mottaMeldingAiven()` |
| Route incoming SED | `SedRuter.rutSedTilBehandling()` |
| Get institutions | `EessiService.hentEessiMottakerinstitusjoner()` |
| Generate PDF | `EessiService.genererSedPdf()` |
| Close BUC | `EessiService.lukkBuc()` |

## Testing EESSI Locally

With melosys-docker-compose:

```bash
# Mock EESSI service runs on melosys-mock
# Endpoints are stubbed to return success responses

# To test SED sending manually:
# 1. Create a behandling with lovvalgsbestemmelse
# 2. Set mottakerinstitusjoner on prosessinstans
# 3. Trigger iverksett vedtak
```
