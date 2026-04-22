# Fagsak Debugging Guide

## Common SQL Queries

### Find Fagsak by Saksnummer
```sql
SELECT * FROM fagsak WHERE saksnummer = 'MEL-12345';
```

### Find Fagsak by Archive ID (GSAK)
```sql
SELECT * FROM fagsak WHERE gsak_saksnummer = 123456;
```

### Find All Fagsaker for a Person
```sql
SELECT f.*, a.rolle
FROM fagsak f
JOIN aktoer a ON a.fagsak_saksnummer = f.saksnummer
WHERE a.aktoer_id = '2512489212185'
ORDER BY f.registrert_dato DESC;
```

### Find Fagsaker by Organization
```sql
SELECT f.*, a.rolle
FROM fagsak f
JOIN aktoer a ON a.fagsak_saksnummer = f.saksnummer
WHERE a.orgnr = '912345678'
AND a.rolle IN ('VIRKSOMHET', 'ARBEIDSGIVER')
ORDER BY f.registrert_dato DESC;
```

### Find Active Cases (with active behandling)
```sql
SELECT f.saksnummer, f.type, f.tema, f.status, b.status as behandling_status
FROM fagsak f
JOIN behandling b ON b.fagsak_saksnummer = f.saksnummer
WHERE f.status = 'OPPRETTET'
AND b.status NOT IN ('AVSLUTTET', 'LUKKET')
ORDER BY f.registrert_dato DESC;
```

## Common Issues

### Issue: Duplicate Actors
**Symptom**: `TekniskException: "Det finnes mer enn en aktør med rollen X for sak Y"`

**Investigation**:
```sql
SELECT fagsak_saksnummer, rolle, COUNT(*) as count
FROM aktoer
WHERE rolle NOT IN ('TRYGDEMYNDIGHET', 'FULLMEKTIG', 'REPRESENTANT')
GROUP BY fagsak_saksnummer, rolle
HAVING COUNT(*) > 1;
```

**Resolution**: Remove duplicate actor, keeping the correct one.

### Issue: Multiple Active Behandlinger
**Symptom**: `TekniskException` when calling `hentAktivBehandling()`

**Investigation**:
```sql
SELECT f.saksnummer, COUNT(*) as active_count
FROM fagsak f
JOIN behandling b ON b.fagsak_saksnummer = f.saksnummer
WHERE b.status NOT IN ('AVSLUTTET', 'LUKKET')
AND b.type != 'AARSAVREGNING'
GROUP BY f.saksnummer
HAVING COUNT(*) > 1;
```

**Resolution**: Close extra behandlinger or merge them.

### Issue: Missing GSAK Reference
**Symptom**: Cannot archive documents, fagsak not linked to Joark

**Investigation**:
```sql
SELECT saksnummer, gsak_saksnummer
FROM fagsak
WHERE gsak_saksnummer IS NULL
AND status = 'OPPRETTET';
```

**Resolution**: Create arkivsak via OpprettArkivsak saga step.

### Issue: Case Type/Theme Mismatch
**Symptom**: `FunksjonellException` when changing type or theme

**Investigation**:
```kotlin
// Check if change is allowed
fagsak.kanEndreTypeOgTema()

// Validate legal combination
lovligeKombinasjonerService.validerKombinasjon(
    sakstype, sakstema, behandlingstype, behandlingstema
)
```

```sql
-- Check current state
SELECT f.saksnummer, f.type, f.tema,
       b.type as beh_type, b.tema as beh_tema, b.status
FROM fagsak f
JOIN behandling b ON b.fagsak_saksnummer = f.saksnummer
WHERE f.saksnummer = 'MEL-12345';
```

## Logs to Check

### FagsakService Operations
```
grep "FagsakService" application.log | grep "MEL-12345"
```

### Actor Changes
```
grep "Aktoer" application.log | grep "MEL-12345"
```

### Case Creation
```
grep "nyFagsakOgBehandling" application.log
```

## Key Code Locations

| Operation | Location |
|-----------|----------|
| Case creation | `FagsakService.nyFagsakOgBehandling()` |
| Case closure | `FagsakService.avsluttFagsakOgBehandling()` |
| Actor management | `FagsakService.oppdaterMyndigheter*()` |
| Type/theme change | `FagsakService.oppdaterFagsakOgBehandling()` |
| Repository queries | `FagsakRepository` |

## Metrics

```kotlin
// Counter for created cases
SAKER_OPPRETTET.increment()
```

