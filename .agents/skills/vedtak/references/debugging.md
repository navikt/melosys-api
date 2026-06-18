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
    p.uuid as prosess_id,
    p.prosess_type,
    p.status as prosess_status,
    p.sist_fullfort_steg,
    p.registrert_dato,
    p.endret_dato
FROM behandling b
JOIN fagsak f ON f.saksnummer = b.saksnummer
LEFT JOIN prosessinstans p ON p.behandling_id = b.id
    AND p.prosess_type LIKE 'IVERKSETT_VEDTAK%'
WHERE b.id = :behandlingId
ORDER BY p.registrert_dato DESC;
```

## SQL Investigation Queries

### Find Vedtak Prosessinstans by Behandling

```sql
SELECT uuid, prosess_type, status, sist_fullfort_steg,
       sed_laas_referanse, registrert_dato, endret_dato
FROM prosessinstans
WHERE behandling_id = :behandlingId
AND prosess_type LIKE 'IVERKSETT_VEDTAK%'
ORDER BY registrert_dato DESC;
```

### Find Vedtak by Saksnummer

```sql
SELECT p.*, b.id as behandling_id
FROM prosessinstans p
JOIN behandling b ON p.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE f.saksnummer = :saksnummer
AND p.prosess_type LIKE 'IVERKSETT_VEDTAK%'
ORDER BY p.registrert_dato DESC;
```

### Find Failed Vedtak Processes

```sql
SELECT p.uuid, p.prosess_type, p.sist_fullfort_steg, p.endret_dato,
       f.saksnummer, b.id as behandling_id
FROM prosessinstans p
JOIN behandling b ON p.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE p.prosess_type LIKE 'IVERKSETT_VEDTAK%'
AND p.status = 'FEILET'
ORDER BY p.endret_dato DESC
FETCH FIRST 20 ROWS ONLY;
```

### Find Stuck Vedtak (Running > 1 hour)

```sql
SELECT p.uuid, p.prosess_type, p.sist_fullfort_steg,
       f.saksnummer, b.id as behandling_id,
       ROUND((SYSDATE - p.endret_dato) * 24, 2) as hours_stuck
FROM prosessinstans p
JOIN behandling b ON p.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE p.prosess_type LIKE 'IVERKSETT_VEDTAK%'
AND p.status = 'UNDER_BEHANDLING'
AND p.endret_dato < SYSDATE - INTERVAL '1' HOUR
ORDER BY p.endret_dato ASC;
```

### Check Behandlingsresultat State

```sql
-- behandlingsresultat shares its PK with behandling (behandling_id, via @MapsId).
-- vedtak_metadata is 1:1 with behandlingsresultat on behandlingsresultat_id.
SELECT
    br.behandling_id,
    br.resultat_type,
    br.fastsatt_av_land,
    br.fakturaserie_referanse,
    vm.vedtak_type,
    vm.vedtak_dato
FROM behandlingsresultat br
LEFT JOIN vedtak_metadata vm ON vm.behandlingsresultat_id = br.behandling_id
WHERE br.behandling_id = :behandlingId;
```

### Check Membership Periods

```sql
-- medlemskapsperiode joins behandlingsresultat via behandlingsresultat_id;
-- behandlingsresultat uses a shared PK (behandling_id) via @MapsId.
SELECT
    mp.id, mp.fom_dato, mp.tom_dato,
    mp.innvilgelse_resultat,
    mp.medlemskapstype,
    mp.bestemmelse
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.behandling_id
WHERE br.behandling_id = :behandlingId
ORDER BY mp.fom_dato;
```

### Check Lovvalgsperioder

```sql
-- lovvalg_periode joins behandlingsresultat via beh_resultat_id;
-- behandlingsresultat uses a shared PK (behandling_id) via @MapsId.
SELECT
    lp.id, lp.fom_dato, lp.tom_dato,
    lp.innvilgelse_resultat,
    lp.lovvalgsland
FROM lovvalg_periode lp
JOIN behandlingsresultat br ON lp.beh_resultat_id = br.behandling_id
WHERE br.behandling_id = :behandlingId
ORDER BY lp.fom_dato;
```

### Check Invoice Series

There is no `fakturaserie` table in melosys-api. The invoice series itself lives in the external
faktureringskomponenten; melosys-api only stores the reference in
`behandlingsresultat.fakturaserie_referanse`.

```sql
SELECT br.behandling_id, br.fakturaserie_referanse
FROM behandlingsresultat br
WHERE br.behandling_id = :behandlingId;
```

### Prosessinstans Data

There is no separate `prosessinstans_data` table. Saga data lives in an inline serialized
`Properties` blob in the `prosessinstans.data` column (CLOB), and is read/written through the
`ProsessDataKey` API on the `Prosessinstans` entity (`getData(ProsessDataKey)` /
`setData(ProsessDataKey, ...)`), not via SQL. To inspect raw data:

```sql
SELECT uuid, prosess_type, data
FROM prosessinstans
WHERE behandling_id = :behandlingId
AND prosess_type LIKE 'IVERKSETT_VEDTAK%';
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

**Symptoms**: Prosessinstans status=UNDER_BEHANDLING, sist_fullfort_steg=null or empty

**Investigation**:
1. Check MEDL API status
2. Look for MEDL-related errors in logs
3. Verify behandlingsresultat has valid medlemskapsperioder

```sql
-- Check if periods exist
SELECT COUNT(*) FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.behandling_id
WHERE br.behandling_id = :behandlingId;
```

**Resolution**:
- If MEDL API was down: Restart the prosessinstans
- If periods missing: Fix behandlingsresultat, then restart

### 2. Vedtak Stuck at OPPRETT_FAKTURASERIE

**Symptoms**: sist_fullfort_steg=LAGRE_*_MEDL, stuck for hours

**Investigation**:
1. Check faktureringskomponenten connectivity
2. Look for duplicate fakturaserier
3. Verify trygdeavgiftsperioder exist

```sql
-- Check whether a fakturaserie reference is already stored
SELECT br.behandling_id, br.fakturaserie_referanse
FROM behandlingsresultat br
WHERE br.behandling_id = :behandlingId;
-- (the actual fakturaserie lives in faktureringskomponenten, not melosys-api)
```

**Resolution**:
- If fakturaserie already exists: Mark step complete, continue saga
- If external API issue: Wait for recovery, restart

### 3. Vedtak Stuck at SEND_VEDTAKSBREV_INNLAND

**Symptoms**: sist_fullfort_steg=OPPRETT_FAKTURASERIE

**Investigation**:
1. Check Dokgen/Joark connectivity
2. Verify brevbestilling was valid
3. Check for journalpost creation

**Resolution**:
- Letter already sent: Continue saga manually
- Template error: Fix template, restart

### 4. Vedtak Stuck at SEND_VEDTAK_UTLAND (EØS only)

**Symptoms**: sist_fullfort_steg=SEND_VEDTAKSBREV_INNLAND

**Investigation**:
1. Check RINA connectivity
2. Verify BUC/SED creation
3. Check that the foreign authority (myndighet) was resolved in the AVKLAR_MYNDIGHET step.
   This is resolved/stored during the saga (see `AvklarMyndighet`), not in a dedicated
   `myndighet` table — inspect the step logs and the prosessinstans `data` blob rather than SQL.

**Resolution**:
- RINA down: Wait and restart
- Missing myndighet: Resolve myndighet, restart

### 5. Behandling Stuck in IVERKSETTER_VEDTAK

**Symptoms**: behandling.status = IVERKSETTER_VEDTAK for > 2 hours

**Investigation**:
1. Find prosessinstans for this behandling
2. Check if prosessinstans is FERDIG but behandling not updated

```sql
SELECT b.status, p.status, p.sist_fullfort_steg
FROM behandling b
LEFT JOIN prosessinstans p ON p.behandling_id = b.id
    AND p.prosess_type LIKE 'IVERKSETT_VEDTAK%'
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

Exposed by `ProsessinstansAdminController` (`saksflyt/.../kontroll/ProsessinstansAdminController.java`),
base path `/admin/prosessinstanser` (no `/api` prefix — that prefix only applies to
`no.nav.melosys.tjenester.gui` controllers). All `/admin/**` endpoints are guarded by the
`ApiKeyInterceptor`. Logic lives in `ProsessinstansAdminService`.

### List Failed Prosessinstanser
```
GET /admin/prosessinstanser/feilede
```

### List Locked (fastlåste) Prosessinstanser
```
GET /admin/prosessinstanser/laaste
```

### Get Saga Details
```
GET /admin/prosessinstanser/{uuid}
```

### Restart Specific Prosessinstanser (uuids in body)
```
POST /admin/prosessinstanser/restart
{ "uuids": ["<uuid>", ...] }
```

### Restart All Failed Prosessinstanser
```
POST /admin/prosessinstanser/feilede/restart
```

### Skip Current Step and Restart
```
POST /admin/prosessinstanser/hopp-over-steg/{uuid}
```

### Force-complete (ferdigstill) a Prosessinstans
```
POST /admin/prosessinstanser/ferdigstill/{uuid}
```

### Health / Metrics
Actuator endpoints are under the management base path `/internal` (see application.yml,
exposure: health, loggers, prometheus):
```
GET /internal/health
GET /internal/prometheus
```
