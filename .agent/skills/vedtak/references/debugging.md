# Vedtak Debugging Guide

## Table of Contents
1. [Quick Diagnostics](#quick-diagnostics)
2. [SQL Investigation Queries](#sql-investigation-queries)
3. [Log Patterns](#log-patterns)
4. [Step-by-Step Troubleshooting](#step-by-step-troubleshooting)
5. [Common Issues and Solutions](#common-issues-and-solutions)
6. [Admin Endpoints](#admin-endpoints)

## Quick Diagnostics

### Checklist for Vedtak Issues

1. [ ] What is the behandling status?
2. [ ] Is there a prosessinstans for this behandling?
3. [ ] What is the prosessinstans status and last step?
4. [ ] Are there any duplicate prosessinstanser?
5. [ ] What does the behandlingsresultat look like?
6. [ ] Are there MEDL periods registered?
7. [ ] Is there an invoice series created?

### Quick Status Check

```sql
-- All-in-one status check
SELECT
    b.id as behandling_id,
    b.status as behandling_status,
    f.saksnummer,
    p.id as prosess_id,
    p.type as prosess_type,
    p.status as prosess_status,
    p.sist_utforte_steg,
    p.registrert_dato,
    p.endret_dato
FROM behandling b
JOIN fagsak f ON f.id = b.fagsak_id
LEFT JOIN prosessinstans p ON p.behandling_id = b.id
    AND p.type LIKE 'IVERKSETT_VEDTAK%'
WHERE b.id = :behandlingId
ORDER BY p.registrert_dato DESC;
```

## SQL Investigation Queries

### Find Vedtak Prosessinstans by Behandling

```sql
SELECT id, type, status, sist_utforte_steg,
       laas_referanse, registrert_dato, endret_dato
FROM prosessinstans
WHERE behandling_id = :behandlingId
AND type LIKE 'IVERKSETT_VEDTAK%'
ORDER BY registrert_dato DESC;
```

### Find Vedtak by Saksnummer

```sql
SELECT p.*, b.id as behandling_id
FROM prosessinstans p
JOIN behandling b ON p.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE f.saksnummer = :saksnummer
AND p.type LIKE 'IVERKSETT_VEDTAK%'
ORDER BY p.registrert_dato DESC;
```

### Find Failed Vedtak Processes

```sql
SELECT p.id, p.type, p.sist_utforte_steg, p.endret_dato,
       f.saksnummer, b.id as behandling_id
FROM prosessinstans p
JOIN behandling b ON p.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE p.type LIKE 'IVERKSETT_VEDTAK%'
AND p.status = 'FEILET'
ORDER BY p.endret_dato DESC
FETCH FIRST 20 ROWS ONLY;
```

### Find Stuck Vedtak (Running > 1 hour)

```sql
SELECT p.id, p.type, p.sist_utforte_steg,
       f.saksnummer, b.id as behandling_id,
       ROUND((SYSDATE - p.endret_dato) * 24, 2) as hours_stuck
FROM prosessinstans p
JOIN behandling b ON p.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE p.type LIKE 'IVERKSETT_VEDTAK%'
AND p.status = 'UNDER_BEHANDLING'
AND p.endret_dato < SYSDATE - INTERVAL '1' HOUR
ORDER BY p.endret_dato ASC;
```

### Check Behandlingsresultat State

```sql
SELECT
    br.id,
    br.type as resultat_type,
    br.fastsatt_av_land,
    br.vedtak_metadata_id,
    vm.vedtakstype,
    vm.vedtaksdato
FROM behandlingsresultat br
LEFT JOIN vedtak_metadata vm ON br.vedtak_metadata_id = vm.id
WHERE br.behandling_id = :behandlingId;
```

### Check Membership Periods

```sql
SELECT
    mp.id, mp.fom, mp.tom,
    mp.innvilgelsesresultat,
    mp.medlemskapstype,
    mp.bestemmelse
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.id
WHERE br.behandling_id = :behandlingId
ORDER BY mp.fom;
```

### Check Lovvalgsperioder

```sql
SELECT
    lp.id, lp.fom, lp.tom,
    lp.innvilgelsesresultat,
    lp.lovvalgsland
FROM lovvalgsperiode lp
JOIN behandlingsresultat br ON lp.behandlingsresultat_id = br.id
WHERE br.behandling_id = :behandlingId
ORDER BY lp.fom;
```

### Check Invoice Series

```sql
SELECT
    fs.id, fs.fakturaserie_referanse,
    fs.opprettet_dato, fs.status
FROM fakturaserie fs
JOIN behandlingsresultat br ON fs.behandlingsresultat_id = br.id
WHERE br.behandling_id = :behandlingId;
```

### Find Prosessinstans with Data

```sql
SELECT p.id, p.type, pd.key, pd.verdi
FROM prosessinstans p
JOIN prosessinstans_data pd ON pd.prosessinstans_id = p.id
WHERE p.behandling_id = :behandlingId
AND p.type LIKE 'IVERKSETT_VEDTAK%';
```

## Log Patterns

### Vedtak Start
```
INFO  FtrlVedtakService - Fatter vedtak for (FTRL) sak: {saksnummer} behandling: {behandlingId}
INFO  ProsessinstansService - Opprettet prosessinstans {} med type IVERKSETT_VEDTAK_FTRL
```

### Step Execution
```
INFO  ProsessinstansBehandler - Starter steg {} for prosessinstans {}
INFO  LagreMedlemsperiodeMedl - Lagrer medlemskapsperiode til MEDL for behandling {}
INFO  OpprettFakturaserie - Oppretter fakturaserie for behandling {}
INFO  ProsessinstansBehandler - Fullført steg {} for prosessinstans {}
```

### Success
```
INFO  AvsluttFagsakOgBehandling - Avslutter sak og behandling {}
INFO  ProsessinstansBehandler - Prosessinstans {} behandlet ferdig
```

### Error Patterns
```
ERROR LagreMedlemsperiodeMedl - Feil ved lagring til MEDL: {}
ERROR OpprettFakturaserie - Kunne ikke opprette fakturaserie: {}
ERROR ProsessinstansBehandler - Steg {} feilet for prosessinstans {}: {}
WARN  SendMeldingOmVedtak - Feature toggle MELOSYS_SEND_MELDING_OM_VEDTAK er av
```

### Search in Kibana/Logs
```
# Find all logs for a vedtak saga
prosessinstans_id: "{uuid}"

# Find vedtak errors
level: ERROR AND message: *vedtak*

# Find MEDL issues in vedtak
message: *MEDL* AND prosess_type: IVERKSETT_VEDTAK*

# Find invoice issues
message: *faktura* AND behandling_id: {id}
```

## Step-by-Step Troubleshooting

### 1. Vedtak Stuck at LAGRE_MEDLEMSKAPSPERIODE_MEDL

**Symptoms**: Prosessinstans status=UNDER_BEHANDLING, sist_utforte_steg=null or empty

**Investigation**:
1. Check MEDL API status
2. Look for MEDL-related errors in logs
3. Verify behandlingsresultat has valid medlemskapsperioder

```sql
-- Check if periods exist
SELECT COUNT(*) FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.id
WHERE br.behandling_id = :behandlingId;
```

**Resolution**:
- If MEDL API was down: Restart the prosessinstans
- If periods missing: Fix behandlingsresultat, then restart

### 2. Vedtak Stuck at OPPRETT_FAKTURASERIE

**Symptoms**: sist_utforte_steg=LAGRE_*_MEDL, stuck for hours

**Investigation**:
1. Check faktureringskomponenten connectivity
2. Look for duplicate fakturaserier
3. Verify trygdeavgiftsperioder exist

```sql
-- Check for existing fakturaserie
SELECT * FROM fakturaserie fs
JOIN behandlingsresultat br ON fs.behandlingsresultat_id = br.id
WHERE br.behandling_id = :behandlingId;
```

**Resolution**:
- If fakturaserie already exists: Mark step complete, continue saga
- If external API issue: Wait for recovery, restart

### 3. Vedtak Stuck at SEND_VEDTAKSBREV_INNLAND

**Symptoms**: sist_utforte_steg=OPPRETT_FAKTURASERIE

**Investigation**:
1. Check Dokgen/Joark connectivity
2. Verify brevbestilling was valid
3. Check for journalpost creation

**Resolution**:
- Letter already sent: Continue saga manually
- Template error: Fix template, restart

### 4. Vedtak Stuck at SEND_VEDTAK_UTLAND (EØS only)

**Symptoms**: sist_utforte_steg=SEND_VEDTAKSBREV_INNLAND

**Investigation**:
1. Check RINA connectivity
2. Verify BUC/SED creation
3. Check for myndighet being resolved

```sql
-- Check myndighet
SELECT m.* FROM myndighet m
JOIN behandlingsresultat br ON m.behandlingsresultat_id = br.id
WHERE br.behandling_id = :behandlingId;
```

**Resolution**:
- RINA down: Wait and restart
- Missing myndighet: Add myndighet, restart

### 5. Behandling Stuck in IVERKSETTER_VEDTAK

**Symptoms**: behandling.status = IVERKSETTER_VEDTAK for > 2 hours

**Investigation**:
1. Find prosessinstans for this behandling
2. Check if prosessinstans is FERDIG but behandling not updated

```sql
SELECT b.status, p.status, p.sist_utforte_steg
FROM behandling b
LEFT JOIN prosessinstans p ON p.behandling_id = b.id
    AND p.type LIKE 'IVERKSETT_VEDTAK%'
WHERE b.id = :behandlingId;
```

**Resolution**:
- Saga FERDIG but behandling not: Manually update behandling status
- Saga FEILET: Fix issue and restart
- No saga found: Create new vedtak saga

## Common Issues and Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| Duplicate vedtak | Double-click | Cancel one, verify data |
| MEDL timeout | API slow | Restart after API recovers |
| Missing myndighet | EØS without institution | Add myndighet manually |
| Invoice exists | Retry after timeout | Mark step complete |
| Letter generation fail | Template issue | Fix template, retry |
| RINA connection fail | External system | Wait and retry |
| Feature toggle off | Config | Enable toggle in Unleash |

## Admin Endpoints

### List Active Vedtak Sagas
```
GET /api/admin/prosessinstanser?type=IVERKSETT_VEDTAK&status=UNDER_BEHANDLING
```

### Restart Failed Saga
```
POST /api/admin/prosessinstanser/{id}/restart
```

### Get Saga Details
```
GET /api/admin/prosessinstanser/{id}
```

### Health Check
```
GET /api/internal/isReady
```

### Metrics
```
GET /api/internal/prometheus
# Look for:
# prosessinstanser_opprettet{type="IVERKSETT_VEDTAK_*"}
# prosessinstanser_steg_utført{type="IVERKSETT_VEDTAK_*", status="success|failure"}
