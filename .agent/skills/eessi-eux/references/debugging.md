# EESSI Debugging Guide

## SQL Queries

### SED Document Analysis

```sql
-- All SED documents for a fagsak
SELECT
    sd.id,
    sd.sed_type,
    sd.rina_saksnummer,
    sd.rina_dokument_id,
    sd.journalpost_id,
    sd.opprettet_tidspunkt,
    f.saksnummer
FROM sed_dokument sd
JOIN fagsak f ON sd.fagsak_id = f.id
WHERE f.saksnummer = :saksnummer
ORDER BY sd.opprettet_tidspunkt DESC;

-- Find SED by RINA case number
SELECT sd.*, f.saksnummer
FROM sed_dokument sd
JOIN fagsak f ON sd.fagsak_id = f.id
WHERE sd.rina_saksnummer = :rinaSaksnummer;

-- Find behandlinger created from incoming SED
SELECT
    b.id as behandling_id,
    b.status,
    b.mottatt_dato,
    sd.sed_type,
    sd.rina_saksnummer
FROM behandling b
JOIN sed_dokument sd ON b.sed_dokument_id = sd.id
WHERE sd.sed_type IN ('A001', 'A003');
```

### Case Relationships

```sql
-- In melosys-eessi database (if accessible)
-- Check saksrelasjon table for BUC to NAV case mapping
SELECT *
FROM saksrelasjon
WHERE gsak_saksnummer = :gsakSaksnummer
   OR rina_saksnummer = :rinaSaksnummer;
```

### Prosessinstans for SED Operations

```sql
-- Check SED sending prosessinstans
SELECT
    pi.id,
    pi.type,
    pi.status,
    pi.sist_utforte_steg,
    pi.feil_melding,
    pi.opprettet_tidspunkt
FROM prosessinstans pi
JOIN behandling b ON pi.behandling_id = b.id
WHERE b.id = :behandlingId
AND pi.type LIKE 'IVERKSETT_VEDTAK%'
ORDER BY pi.opprettet_tidspunkt DESC;

-- Check SED mottak prosessinstans
SELECT
    pi.id,
    pi.type,
    pi.status,
    pi.data,
    pi.feil_melding
FROM prosessinstans pi
WHERE pi.type = 'SED_MOTTAK'
AND pi.created_date > SYSDATE - 7
ORDER BY pi.opprettet_tidspunkt DESC;
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
AND pi.type LIKE 'IVERKSETT_VEDTAK%';
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
grep "EessiConsumer" application.log | grep -E "ERROR|Exception"
```

## Troubleshooting Flowchart

```
SED sending failed?
├── Check prosessinstans status
│   └── FEILET → Check feil_melding
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
