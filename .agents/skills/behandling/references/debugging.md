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
    b.type as behandlingstype,
    b.tema as behandlingstema,
    b.registrert_dato,
    b.endret_dato,
    f.saksnummer,
    f.status as fagsak_status,
    f.type as sakstype
FROM behandling b
JOIN fagsak f ON b.fagsak_id = f.id
WHERE b.id = :behandlingId;
```

## SQL Investigation Queries

### Find Behandling by Saksnummer

```sql
SELECT b.*, f.saksnummer
FROM behandling b
JOIN fagsak f ON b.fagsak_id = f.id
WHERE f.saksnummer = :saksnummer
ORDER BY b.registrert_dato DESC;
```

### Find All Behandlinger for Person

```sql
SELECT b.id, b.status, b.type, b.tema, b.registrert_dato, f.saksnummer
FROM behandling b
JOIN fagsak f ON b.fagsak_id = f.id
WHERE f.bruker_aktor_id = :aktorId
ORDER BY b.registrert_dato DESC;
```

### Find Active Behandlinger

```sql
SELECT b.id, b.status, b.type, f.saksnummer, b.endret_dato
FROM behandling b
JOIN fagsak f ON b.fagsak_id = f.id
WHERE b.status NOT IN ('AVSLUTTET')
ORDER BY b.endret_dato DESC
FETCH FIRST 50 ROWS ONLY;
```

### Find Behandlinger Stuck in IVERKSETTER_VEDTAK

```sql
SELECT b.id, f.saksnummer, b.endret_dato,
       ROUND((SYSDATE - b.endret_dato) * 24, 2) as hours_stuck
FROM behandling b
JOIN fagsak f ON b.fagsak_id = f.id
WHERE b.status = 'IVERKSETTER_VEDTAK'
AND b.endret_dato < SYSDATE - INTERVAL '1' HOUR
ORDER BY b.endret_dato ASC;
```

### Check Behandling with Related Data

```sql
SELECT
    b.id, b.status, b.type, b.tema,
    br.id as resultat_id, br.type as resultat_type,
    (SELECT COUNT(*) FROM medlemskapsperiode mp WHERE mp.behandlingsresultat_id = br.id) as medlemskapsperioder,
    (SELECT COUNT(*) FROM lovvalgsperiode lp WHERE lp.behandlingsresultat_id = br.id) as lovvalgsperioder
FROM behandling b
LEFT JOIN behandlingsresultat br ON br.behandling_id = b.id
WHERE b.id = :behandlingId;
```

### Check Prosessinstanser for Behandling

```sql
SELECT p.id, p.type, p.status, p.sist_utforte_steg,
       p.registrert_dato, p.endret_dato
FROM prosessinstans p
WHERE p.behandling_id = :behandlingId
ORDER BY p.registrert_dato DESC;
```

### Find Oppgave for Behandling

```sql
SELECT o.id, o.oppgave_id, o.type, o.status
FROM oppgave o
WHERE o.behandling_id = :behandlingId;
```

### Check Version/Audit Trail

```sql
SELECT * FROM behandling_aud
WHERE id = :behandlingId
ORDER BY rev DESC
FETCH FIRST 10 ROWS ONLY;
```

### Find Behandlinger Created Today

```sql
SELECT b.id, b.status, b.type, b.tema, f.saksnummer, b.registrert_dato
FROM behandling b
JOIN fagsak f ON b.fagsak_id = f.id
WHERE TRUNC(b.registrert_dato) = TRUNC(SYSDATE)
ORDER BY b.registrert_dato DESC;
```

## Common Issues

### Issue 1: Behandling Stuck in IVERKSETTER_VEDTAK

**Symptoms**: User cannot edit behandling, UI shows "Iverksetter vedtak"

**Investigation**:
```sql
-- Check prosessinstans status
SELECT p.id, p.type, p.status, p.sist_utforte_steg
FROM prosessinstans p
WHERE p.behandling_id = :behandlingId
AND p.type LIKE 'IVERKSETT_VEDTAK%';
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
-- Check for active behandling
SELECT b.id, b.status, b.type
FROM behandling b
WHERE b.fagsak_id = (SELECT fagsak_id FROM behandling WHERE id = :behandlingId)
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
SELECT b.id, br.id as resultat_id
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
-- Check audit trail
SELECT * FROM behandling_aud
WHERE id = :behandlingId
ORDER BY rev DESC;
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
// Returns false if status in [AVSLUTTET, IVERKSETTER_VEDTAK, MIDLERTIDIG_LOVVALGSBESLUTNING]
```

**Possible Causes**:
1. Status is IVERKSETTER_VEDTAK (saga running)
2. Status is AVSLUTTET (already closed)
3. Status is MIDLERTIDIG_LOVVALGSBESLUTNING (Art. 13 provisional)

**Resolution**:
- Wait for saga to complete
- Create ny_vurdering if behandling is closed

## Log Patterns

### Behandling Creation
```
INFO  JournalføringService - Oppretter ny sak og behandling for journalpost {}
INFO  BehandlingService - Opprettet behandling {} for fagsak {}
```

### Status Changes
```
INFO  BehandlingService - Endrer status på behandling {} fra {} til {}
INFO  BehandlingEventListener - BehandlingEndretStatusEvent: {} -> {}
```

### Errors
```
ERROR BehandlingService - Kunne ikke endre status: {}
ERROR FunksjonellException - Det finnes allerede en aktiv behandling
WARN  BehandlingService - Behandling {} er ikke redigerbar
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
AND type LIKE 'IVERKSETT_VEDTAK%';
```

2. If saga FEILET: Restart via admin
```
POST /api/admin/prosessinstanser/{id}/restart
```

3. If saga stuck UNDER_BEHANDLING for >2h: Check for external service issues, restart

4. If no saga but status is IVERKSETTER_VEDTAK:
```sql
-- Manual fix (use with caution!)
UPDATE behandling SET status = 'UNDER_BEHANDLING' WHERE id = :id;
```

### Create Missing Behandlingsresultat

```kotlin
// Via service (preferred)
behandlingsresultatService.opprettForBehandling(behandlingId)
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
WHERE id = (SELECT fagsak_id FROM behandling WHERE id = :behandlingId)
AND NOT EXISTS (
    SELECT 1 FROM behandling
    WHERE fagsak_id = fagsak.id
    AND status NOT IN ('AVSLUTTET')
    AND id != :behandlingId
);
```
