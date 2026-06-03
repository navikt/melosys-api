# Behandling Debugging Guide

## Table of Contents
1. [Quick Diagnostics](#quick-diagnostics)
2. [SQL Investigation Queries](#sql-investigation-queries)
3. [Common Issues](#common-issues)
4. [Log Patterns](#log-patterns)
5. [Recovery Procedures](#recovery-procedures)

## Quick Diagnostics

### Checklist for Behandling Issues

1. [ ] What is the behandling status?
2. [ ] What is the fagsak status?
3. [ ] Is there an active prosessinstans for this behandling?
4. [ ] What is the behandlingsresultat state?
5. [ ] Are there any related oppgaver in Gosys?
6. [ ] Check audit trail for recent changes

### Quick Status Overview

```sql
SELECT
    b.id as behandling_id,
    b.status as behandling_status,
    b.beh_type as behandlingstype,
    b.beh_tema as behandlingstema,
    b.registrert_dato,
    b.endret_dato,
    f.saksnummer,
    f.status as fagsak_status,
    f.fagsak_type as sakstype
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE b.id = :behandlingId;
```

## SQL Investigation Queries

### Find Behandling by Saksnummer

```sql
SELECT b.*, f.saksnummer
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE f.saksnummer = :saksnummer
ORDER BY b.registrert_dato DESC;
```

### Find All Behandlinger for Person

```sql
-- The actor (aktoer_id) is stored in the aktoer table, joined on saksnummer.
SELECT b.id, b.status, b.beh_type, b.beh_tema, b.registrert_dato, f.saksnummer
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
JOIN aktoer a ON a.saksnummer = f.saksnummer AND a.rolle = 'BRUKER'
WHERE a.aktoer_id = :aktorId
ORDER BY b.registrert_dato DESC;
```

### Find Active Behandlinger

```sql
SELECT b.id, b.status, b.beh_type, f.saksnummer, b.endret_dato
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE b.status NOT IN ('AVSLUTTET')
ORDER BY b.endret_dato DESC
FETCH FIRST 50 ROWS ONLY;
```

### Find Behandlinger Stuck in IVERKSETTER_VEDTAK

```sql
SELECT b.id, f.saksnummer, b.endret_dato,
       ROUND((SYSDATE - b.endret_dato) * 24, 2) as hours_stuck
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE b.status = 'IVERKSETTER_VEDTAK'
AND b.endret_dato < SYSDATE - INTERVAL '1' HOUR
ORDER BY b.endret_dato ASC;
```

### Check Behandling with Related Data

```sql
-- behandlingsresultat PK is behandling_id (no separate id).
-- Period tables reference it via beh_resultat_id.
SELECT
    b.id, b.status, b.beh_type, b.beh_tema,
    br.behandling_id as resultat_behandling_id, br.resultat_type,
    (SELECT COUNT(*) FROM medlemskapsperiode mp WHERE mp.beh_resultat_id = br.behandling_id) as medlemskapsperioder,
    (SELECT COUNT(*) FROM lovvalg_periode lp WHERE lp.beh_resultat_id = br.behandling_id) as lovvalgsperioder
FROM behandling b
LEFT JOIN behandlingsresultat br ON br.behandling_id = b.id
WHERE b.id = :behandlingId;
```

### Check Prosessinstanser for Behandling

```sql
-- prosessinstans has no status column; progress = steg + antall_retry/sover_til. PK is uuid.
SELECT p.uuid, p.prosess_type, p.steg, p.antall_retry, p.sist_forsoekt, p.sover_til,
       p.registrert_dato, p.endret_dato
FROM prosessinstans p
WHERE p.behandling_id = :behandlingId
ORDER BY p.registrert_dato DESC;
```

### Find Oppgave for Behandling

```sql
-- There is no oppgave table; the Gosys oppgave reference is the oppgave_id column on behandling.
SELECT b.id, b.oppgave_id
FROM behandling b
WHERE b.id = :behandlingId;
```

### Check Status History (behandling_historikk)

```sql
-- behandling_historikk is the status-change audit trail (there is no Envers behandling_aud table).
SELECT id, behandling_id, dato, status, ident, kommentar
FROM behandling_historikk
WHERE behandling_id = :behandlingId
ORDER BY dato DESC
FETCH FIRST 10 ROWS ONLY;
```

### Find Behandlinger Created Today

```sql
SELECT b.id, b.status, b.beh_type, b.beh_tema, f.saksnummer, b.registrert_dato
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE TRUNC(b.registrert_dato) = TRUNC(SYSDATE)
ORDER BY b.registrert_dato DESC;
```

## Common Issues

### Issue 1: Behandling Stuck in IVERKSETTER_VEDTAK

**Symptoms**: User cannot edit behandling, UI shows "Iverksetter vedtak"

**Investigation**:
```sql
-- Check prosessinstans progress (steg + retry/sover_til; no status column)
SELECT p.uuid, p.prosess_type, p.steg, p.antall_retry, p.sist_forsoekt, p.sover_til
FROM prosessinstans p
WHERE p.behandling_id = :behandlingId
AND p.prosess_type LIKE 'IVERKSETT_VEDTAK%';
```

**Possible Causes**:
1. Saga failed at a step
2. External service timeout (MEDL, Joark, RINA)
3. Race condition created duplicate saga

**Resolution**:
- If saga FEILET: Restart saga via admin endpoint
- If saga FERDIG but behandling not updated: Manually update status
- If duplicate sagas: Cancel one, verify data

### Issue 2: Cannot Create Ny Vurdering

**Symptoms**: "Det finnes allerede en aktiv behandling" or similar error

**Investigation**:
```sql
-- Check for active behandling on the same fagsak (joined via saksnummer)
SELECT b.id, b.status, b.beh_type
FROM behandling b
WHERE b.saksnummer = (SELECT saksnummer FROM behandling WHERE id = :behandlingId)
AND b.status NOT IN ('AVSLUTTET');
```

**Possible Causes**:
1. Existing active behandling on same fagsak
2. Fagsak status doesn't allow new behandling
3. Previous behandling in IVERKSETTER_VEDTAK

**Resolution**:
- Close or wait for existing active behandling
- Check fagsak.status allows ny_vurdering

### Issue 3: Missing Behandlingsresultat

**Symptoms**: NullPointerException when accessing behandlingsresultat

**Investigation**:
```sql
SELECT b.id, br.behandling_id as resultat_behandling_id, br.resultat_type
FROM behandling b
LEFT JOIN behandlingsresultat br ON br.behandling_id = b.id
WHERE b.id = :behandlingId;
```

**Possible Causes**:
1. Journalføring saga didn't complete
2. SED mottak saga failed
3. Manual creation without result

**Resolution**:
- Check prosessinstans for creation saga
- Create behandlingsresultat if missing

### Issue 4: Wrong Status After Manual Action

**Symptoms**: Status reverted or unexpected after user action

**Investigation**:
```sql
-- Check the status-change history
SELECT id, behandling_id, dato, status, ident, kommentar
FROM behandling_historikk
WHERE behandling_id = :behandlingId
ORDER BY dato DESC;
```

**Possible Causes**:
1. Concurrent modification (saga and manual)
2. OptimisticLockException swallowed
3. UI showing stale data

**Resolution**:
- Check for overlapping prosessinstanser
- Verify transaction completed successfully

### Issue 5: Behandling Cannot Be Edited

**Symptoms**: Fields disabled, "behandling er ikke redigerbar"

**Investigation**:
```kotlin
// Check these conditions
behandling.erRedigerbar()
// false when erInaktiv() (status AVSLUTTET or MIDLERTIDIG_LOVVALGSBESLUTNING),
// or status == IVERKSETTER_VEDTAK,
// or (status == ANMODNING_UNNTAK_SENDT && tema != IKKE_YRKESAKTIV)
```

**Possible Causes**:
1. Status is IVERKSETTER_VEDTAK (saga running)
2. Status is AVSLUTTET (already closed)
3. Status is MIDLERTIDIG_LOVVALGSBESLUTNING (Art. 13 provisional)

**Resolution**:
- Wait for saga to complete
- Create ny_vurdering if behandling is closed

## Log Patterns

### Status Changes
```
# BehandlingService.endreStatus
INFO  BehandlingService - Oppdaterer status for behandling {} fra {} til {}
# BehandlingService.endreType / endreTema
INFO  BehandlingService - Endrer behandlingstypen for behandling {} fra {} til {}
INFO  BehandlingService - Endrer behandlingstema for behandling {} fra {} til {}
```
Status changes also publish a `BehandlingEndretStatusEvent`, handled by `BehandlingEventListener`.

### Errors
```
# Thrown as FunksjonellException from BehandlingService
"Behandlingen må være aktiv for å kunne endres. Status var: {}"
"Behandling {} er allerede avsluttet!"
```

### Search Patterns
```
# Find behandling logs
behandling_id: {id}

# Find status change events
message: *EndretStatusEvent* AND behandling_id: {id}

# Find errors
level: ERROR AND behandling_id: {id}
```

## Recovery Procedures

### Unstick Behandling from IVERKSETTER_VEDTAK

1. Find the prosessinstans:
```sql
SELECT * FROM prosessinstans
WHERE behandling_id = :behandlingId
AND prosess_type LIKE 'IVERKSETT_VEDTAK%';
```

2. If the saga has stalled (high antall_retry / past sover_til): Restart via admin
```
POST /admin/prosessinstanser/restart        (body: {"uuids": ["<uuid>"]})
POST /admin/prosessinstanser/feilede/restart (restart all failed)
```

3. If the saga has not advanced its steg for >2h: Check for external service issues, restart

4. If no saga but status is IVERKSETTER_VEDTAK:
```sql
-- Manual fix (use with caution!)
UPDATE behandling SET status = 'UNDER_BEHANDLING' WHERE id = :id;
```

### Create Missing Behandlingsresultat

```kotlin
// Via service (preferred) - creates a fresh IKKE_FASTSATT result for the behandling
behandlingsresultatService.lagreNyttBehandlingsresultat(behandling)
```

### Reset Behandling to Active

Only if saga completed but behandling status wrong:
```sql
-- Verify saga is FERDIG first!
UPDATE behandling
SET status = 'AVSLUTTET', endret_dato = SYSDATE
WHERE id = :behandlingId
AND status = 'IVERKSETTER_VEDTAK';
```

### Force Close Behandling

When behandling is orphaned and needs cleanup:
```sql
-- Document why this is needed!
UPDATE behandling
SET status = 'AVSLUTTET',
    endret_dato = SYSDATE
WHERE id = :behandlingId;

UPDATE fagsak
SET status = 'AVSLUTTET'
WHERE saksnummer = (SELECT saksnummer FROM behandling WHERE id = :behandlingId)
AND NOT EXISTS (
    SELECT 1 FROM behandling
    WHERE saksnummer = fagsak.saksnummer
    AND status NOT IN ('AVSLUTTET')
    AND id != :behandlingId
);
```
