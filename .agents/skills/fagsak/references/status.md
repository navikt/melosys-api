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

```kotlin
// From FagsakService
val UGYLDIGE_SAKSSTATUSER_FOR_TRYGDEAVGIFT = setOf(
    Saksstatuser.ANNULLERT,
    Saksstatuser.HENLAGT
)
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

Factors checked:
1. Must have exactly one active behandling (non-årsavregning)
2. Status must be OPPRETTET
3. No completed vedtak on active behandling

## Debugging

### Find cases by status
```sql
SELECT saksnummer, type, tema, status, registrert_dato
FROM fagsak
WHERE status = 'OPPRETTET'
ORDER BY registrert_dato DESC;
```

### Find cases stuck in OPPRETTET
```sql
SELECT f.saksnummer, f.registrert_dato, b.status as behandling_status
FROM fagsak f
JOIN behandling b ON b.fagsak_saksnummer = f.saksnummer
WHERE f.status = 'OPPRETTET'
AND f.registrert_dato < SYSDATE - INTERVAL '30' DAY
AND b.status NOT IN ('AVSLUTTET', 'LUKKET');
```

### Status change audit
```sql
-- If using Hibernate Envers auditing
SELECT * FROM fagsak_aud
WHERE saksnummer = 'MEL-12345'
ORDER BY rev DESC;
