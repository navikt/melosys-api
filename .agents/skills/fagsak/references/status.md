# Fagsak Status Transitions

## Saksstatuser Enum

| Status | Description | Terminal |
|--------|-------------|----------|
| `OPPRETTET` | Case created, active | No |
| `ANNULLERT` | Case annulled/cancelled | Yes |
| `OPPHØRT` | Membership/law choice terminated | Yes |
| `HENLAGT` | Case dismissed/dropped | Yes |
| `VIDERESENDT` | Forwarded to another authority | Yes |

## Status Transitions

```
                    ┌─────────────┐
                    │  OPPRETTET  │
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │ ANNULLERT│    │  OPPHØRT │    │ HENLAGT  │
    └──────────┘    └──────────┘    └──────────┘
                           │
                           ▼
                    ┌──────────────┐
                    │ VIDERESENDT  │
                    └──────────────┘
```

## Business Rules

### OPPRETTET → ANNULLERT
- Case was created in error
- No vedtak (decision) was made
- Typically used when duplicate case detected

### OPPRETTET → OPPHØRT
- Membership or law choice period ended
- Used after successful vedtak when case is completed
- Most common terminal status for completed cases

### OPPRETTET → HENLAGT
- Case dropped without decision
- Person no longer relevant (e.g., left Norway, deceased)
- Missing information that cannot be obtained

### OPPHØRT → VIDERESENDT
- Responsibility transferred to another country
- Used in EU/EEA coordination scenarios

## Invalid Statuses for Trygdeavgift

Certain statuses prevent trygdeavgift (social insurance charge) processing:

```java
// From FagsakService (a List, not a Set)
public static final List<Saksstatuser> UGYLDIGE_SAKSSTATUSER_FOR_TRYGDEAVGIFT =
    Arrays.asList(
        Saksstatuser.ANNULLERT,
        Saksstatuser.OPPHØRT,
        Saksstatuser.HENLAGT,
        Saksstatuser.HENLAGT_BORTFALT,
        Saksstatuser.VIDERESENDT
    );
```

## Changing Status

### Via FagsakService
```kotlin
fagsakService.oppdaterStatus(fagsak, Saksstatuser.OPPHØRT)
```

### With Case Closure
```kotlin
// Closes both fagsak and active behandling
fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.OPPHØRT)
```

## Type/Theme Change Constraints

Status affects whether type/theme can be changed:

```kotlin
// Returns false if case cannot be modified
fagsak.kanEndreTypeOgTema()
```

The implementation checks exactly two conditions (both must hold):
1. There is an active non-årsavregning behandling (`harAktivBehandlingIkkeÅrsavregning()`)
2. The fagsak has exactly one behandling in total (`behandlinger.size == 1`)

## Debugging

### Find cases by status
```sql
SELECT saksnummer, fagsak_type, tema, status, registrert_dato
FROM fagsak
WHERE status = 'OPPRETTET'
ORDER BY registrert_dato DESC;
```

### Find cases stuck in OPPRETTET
```sql
SELECT f.saksnummer, f.registrert_dato, b.status as behandling_status
FROM fagsak f
JOIN behandling b ON b.saksnummer = f.saksnummer
WHERE f.status = 'OPPRETTET'
AND f.registrert_dato < SYSDATE - INTERVAL '30' DAY
AND b.status != 'AVSLUTTET';
```

### Status change audit
```sql
-- NOTE: fagsak is NOT Envers-audited - there is no fagsak_aud table.
-- The only audit (*_AUD) tables are aktoer_aud and fullmakt_aud.
-- Fagsak status history is not tracked in a dedicated audit table;
-- inspect endret_dato/endret_av on the fagsak row instead.
SELECT saksnummer, status, endret_dato, endret_av
FROM fagsak
WHERE saksnummer = 'MEL-12345';
