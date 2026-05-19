# Vedtak Race Conditions

## Table of Contents
1. [Overview](#overview)
2. [Duplicate Vedtak Creation](#duplicate-vedtak-creation)
3. [Behandling Status Race](#behandling-status-race)
4. [MEDL Update Conflicts](#medl-update-conflicts)
5. [Invoice Series Duplicates](#invoice-series-duplicates)
6. [Concurrent Ny Vurdering](#concurrent-ny-vurdering)
7. [Detection and Mitigation](#detection-and-mitigation)

## Overview

Vedtak processing is susceptible to race conditions because:
1. User can click "Fatt vedtak" multiple times rapidly
2. The check for existing vedtak (`harVedtakInstans`) runs before transaction commit
3. Multiple processes can modify the same behandling concurrently
4. External system calls (MEDL, Joark, RINA) are async and can fail mid-saga

## Duplicate Vedtak Creation

### Scenario
User double-clicks "Fatt vedtak" button:

```
Time    Request 1                   Request 2
────────────────────────────────────────────────────
T1      harVedtakInstans() = false
T2                                  harVedtakInstans() = false
T3      Create prosessinstans
T4      Begin LAGRE_MEDL...
T5                                  Create prosessinstans  ← DUPLICATE!
T6      Commit TX
T7                                  Commit TX
T8      Both saga instances now running
```

### Symptoms
- Two IVERKSETT_VEDTAK_* prosessinstanser for same behandling_id
- Duplicate MEDL period registrations
- Duplicate invoice series created
- Behandling may have inconsistent state

### Detection Query
```sql
-- Find duplicate vedtak processes
SELECT behandling_id, COUNT(*) as count,
       LISTAGG(id, ', ') WITHIN GROUP (ORDER BY registrert_dato) as prosess_ids
FROM prosessinstans
WHERE type LIKE 'IVERKSETT_VEDTAK%'
AND status NOT IN ('FEILET')
GROUP BY behandling_id
HAVING COUNT(*) > 1;
```

### Root Cause
`harVedtakInstans()` checks prosessinstans table, but concurrent transactions
can both pass the check before either commits.

### Mitigation
1. **Frontend debounce**: Disable button after first click
2. **Database constraint**: Unique constraint on (behandling_id, type prefix)
3. **Pessimistic locking**: SELECT FOR UPDATE on behandling before vedtak

---

## Behandling Status Race

### Scenario
Vedtak saga and saksbehandler action overlap:

```
Time    Vedtak Saga                 Saksbehandler Action
────────────────────────────────────────────────────
T1      Start AVSLUTT_SAK_OG_BEH
T2                                  Click "Legg tilbake"
T3      Read: status=IVERKSETTER
T4                                  Set: status=UNDER_BEHANDLING
T5      Set: status=AVSLUTTET       Overwrite or OptimisticLock!
```

### Symptoms
- `OptimisticLockException` in logs
- Behandling stuck in IVERKSETTER_VEDTAK
- Saga shows FERDIG but behandling not AVSLUTTET
- Or behandling AVSLUTTET without completed vedtak steps

### Detection Query
```sql
-- Find behandlinger stuck in IVERKSETTER_VEDTAK
SELECT b.id, b.status, b.endret_dato,
       p.id as prosess_id, p.status as prosess_status, p.sist_utforte_steg
FROM behandling b
LEFT JOIN prosessinstans p ON p.behandling_id = b.id
    AND p.type LIKE 'IVERKSETT_VEDTAK%'
WHERE b.status = 'IVERKSETTER_VEDTAK'
AND b.endret_dato < SYSDATE - INTERVAL '1' HOUR;
```

### Root Cause
No locking between vedtak saga and manual behandling operations.

### Mitigation
- Block manual operations when behandling.status = IVERKSETTER_VEDTAK
- Use version field for optimistic locking with retry

---

## MEDL Update Conflicts

### Scenario
Vedtak saga and another process (annullering, ny vurdering) both update MEDL:

```
Time    Vedtak (LAGRE_MEDL)         Annullering Saga
────────────────────────────────────────────────────
T1      Read MEDL periods
T2                                  Read MEDL periods
T3      Calculate new period
T4                                  Delete/modify period
T5      Write period to MEDL        Write changes to MEDL
T6      Success                     Success - but overwrote vedtak period!
```

### Symptoms
- MEDL period missing or incorrect after vedtak
- Discrepancy between behandlingsresultat and MEDL
- User reports wrong periods in MEDL

### Detection
Compare behandlingsresultat periods with MEDL:
```sql
-- Check for period mismatches
SELECT br.id, mp.fom, mp.tom, mp.status as melosys_status
FROM behandlingsresultat br
JOIN medlemskapsperiode mp ON mp.behandlingsresultat_id = br.id
WHERE br.behandling_id = :behandlingId;
-- Then compare with MEDL query via REST/SQL
```

### Root Cause
No coordination between sagas updating same MEDL data.

### Mitigation
- Use låsReferanse with behandling_id for vedtak sagas
- Ensure annullering waits for vedtak to complete

---

## Invoice Series Duplicates

### Scenario
OPPRETT_FAKTURASERIE step runs twice due to retry or duplicate vedtak:

```
Time    Vedtak Saga
────────────────────────────────────
T1      OPPRETT_FAKTURASERIE start
T2      Call faktureringskomponenten
T3      Timeout (no response)
T4      Step marked FEILET
T5      Retry: OPPRETT_FAKTURASERIE
T6      Call faktureringskomponenten
T7      Success - but first call also succeeded!
```

### Symptoms
- Two invoice series for same behandling
- Duplicate invoices sent to user
- faktureringskomponenten shows duplicate entries

### Detection Query
```sql
-- Check for duplicate fakturaserier
SELECT behandlingsresultat_id, COUNT(*)
FROM fakturaserie
GROUP BY behandlingsresultat_id
HAVING COUNT(*) > 1;
```

### Root Cause
- Network timeout interpreted as failure
- No idempotency key for invoice creation
- Retry creates duplicate

### Mitigation
- Use idempotency key (behandling_id + attempt) in faktureringskomponenten call
- Check for existing fakturaserie before creating new
- OpprettFakturaserie step should be idempotent

---

## Concurrent Ny Vurdering

### Scenario
Ny vurdering behandling created while vedtak saga is running:

```
Time    Vedtak Saga                 New Behandling Created
────────────────────────────────────────────────────
T1      Step 3: OPPRETT_FAKTURASERIE
T2                                  opprettNyVurdering() called
T3      Step 4: SEND_VEDTAKSBREV
T4                                  New behandling copies behandlingsresultat
T5      Step 5: AVSLUTT_SAK_OG_BEH
T6                                  New behandling has stale data!
T7      Set sak.status = AVKLART
```

### Symptoms
- New behandling has old/stale behandlingsresultat data
- Invoice or periods from vedtak not visible in new behandling
- Inconsistent state between old and new behandling

### Detection
```sql
-- Find ny vurdering created during vedtak
SELECT b1.id as vedtak_behandling, b2.id as ny_vurdering,
       p.registrert_dato as vedtak_start, p.endret_dato as vedtak_end,
       b2.registrert_dato as ny_vurdering_created
FROM behandling b1
JOIN prosessinstans p ON p.behandling_id = b1.id
    AND p.type LIKE 'IVERKSETT_VEDTAK%'
JOIN behandling b2 ON b2.fagsak_id = b1.fagsak_id AND b2.id > b1.id
WHERE b2.registrert_dato BETWEEN p.registrert_dato AND p.endret_dato;
```

### Root Cause
No blocking mechanism to prevent ny vurdering during active vedtak.

### Mitigation
- Check for active vedtak prosessinstans before creating ny vurdering
- Use fagsak-level lock during vedtak

---

## Detection and Mitigation

### General Detection Approach

1. **Monitor for duplicate prosessinstanser**:
```sql
SELECT behandling_id, type, COUNT(*)
FROM prosessinstans
GROUP BY behandling_id, type
HAVING COUNT(*) > 1;
```

2. **Check for stuck IVERKSETTER_VEDTAK**:
```sql
SELECT * FROM behandling
WHERE status = 'IVERKSETTER_VEDTAK'
AND endret_dato < SYSDATE - INTERVAL '2' HOUR;
```

3. **Monitor concurrent modifications**:
```sql
SELECT b.id, b.versjon,
       LAG(b.versjon) OVER (PARTITION BY b.id ORDER BY audit.tidspunkt) as prev_version
FROM behandling_audit audit
JOIN behandling b ON b.id = audit.behandling_id
WHERE audit.tidspunkt > SYSDATE - INTERVAL '1' HOUR;
```

### Prevention Strategies

| Strategy | Implementation | Effectiveness |
|----------|----------------|---------------|
| Frontend debounce | Disable button after click | Prevents most double-clicks |
| Database constraint | UNIQUE(behandling_id, type_prefix) | Hard guarantee |
| Pessimistic locking | SELECT FOR UPDATE | Serializes access |
| Idempotency keys | Include in external calls | Prevents duplicate side-effects |
| Status checks | Block operations when IVERKSETTER | Prevents manual interference |

### Recovery Procedures

1. **Duplicate vedtak**: Cancel one saga, verify data consistency
2. **Stuck behandling**: Check saga status, manually complete or reset
3. **MEDL mismatch**: Sync MEDL with behandlingsresultat
4. **Duplicate invoice**: Cancel in faktureringskomponenten

### Logging Patterns to Watch

```
# Duplicate detection
"Det finnes allerede en vedtak-prosess for behandling"

# Status conflicts
"OptimisticLockException"
"Behandling har uventet status"

# MEDL issues
"Kunne ikke lagre periode til MEDL"
"MEDL periode finnes allerede"

# Invoice issues
"Fakturaserie finnes allerede"
"Duplisert faktura oppdaget"
```
