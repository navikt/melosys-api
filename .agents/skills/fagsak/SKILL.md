---
name: fagsak
description: |
  Expert knowledge of Fagsak (case) domain in melosys-api.
  Use when: (1) Understanding the fagsak lifecycle and status transitions,
  (2) Working with actors (aktører) - citizens, employers, authorities,
  (3) Debugging case creation, closure, or type/theme changes,
  (4) Understanding relationship between fagsak and behandling,
  (5) Working with EU/EEA authorities or bilateral treaty authorities.
---

# Fagsak Domain

Fagsak (case/dossier) is the top-level entity representing a social security case in melosys-api.
Each fagsak contains one or more behandlinger (treatments) and multiple aktører (actors).

## Quick Reference

### Module Structure
```
domain/
├── Fagsak.kt              # Main entity (saksnummer, type, tema, status)
├── Aktoer.java            # Actor entity (citizen, employer, authority)
├── Behandling.kt          # Treatment entity (child of Fagsak)
└── kodeverk/
    ├── Sakstyper.java     # EU_EOS, TRYGDEAVTALE, FTRL, etc.
    ├── Sakstemaer.java    # MEDLEMSKAP_LOVVALG, TRYGDEAVGIFT, etc.
    ├── Saksstatuser.java  # OPPRETTET, ANNULLERT, OPPHØRT, HENLAGT, etc.
    └── Aktoersroller.java # BRUKER, VIRKSOMHET, ARBEIDSGIVER, etc.

repository/
└── FagsakRepository.java  # CRUD + custom queries

service/sak/
└── FagsakService.java     # Business operations
```

### Entity Relationships
```
Fagsak (1)
├── (1:*) Aktoer
│   ├── BRUKER (citizen) - identified by aktørId
│   ├── VIRKSOMHET (company) - identified by orgnr
│   ├── ARBEIDSGIVER (employer) - identified by orgnr
│   ├── TRYGDEMYNDIGHET (foreign authority) - institusjonID or land
│   └── FULLMEKTIG (representative)
└── (1:*) Behandling
    ├── Saksopplysninger
    ├── Behandlingsnotater
    └── MottatteOpplysninger
```

### Key Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| `saksnummer` | String | Primary key, format "MEL-{sequence}" |
| `gsakSaksnummer` | Long? | Archive case ID (Joark/GSAK) |
| `type` | Sakstyper | EU_EOS, TRYGDEAVTALE, FTRL, etc. |
| `tema` | Sakstemaer | MEDLEMSKAP_LOVVALG, TRYGDEAVGIFT, etc. |
| `status` | Saksstatuser | OPPRETTET, ANNULLERT, OPPHØRT, HENLAGT, etc. |
| `betalingsvalg` | Betalingstype? | Payment choice |

## Common Operations

### Creating a Case
```kotlin
val request = OpprettSakRequest.Builder()
    .medAktørID("2512489212185")
    .medSakstype(Sakstyper.EU_EOS)
    .medSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG)
    .medBehandlingstype(Behandlingstyper.FØRSTEGANG)
    .medBehandlingstema(Behandlingstema.MEDLEMSKAP)
    .medMottaksdato(LocalDate.now())
    .build()

val fagsak = fagsakService.nyFagsakOgBehandling(request)
```

### Finding a Case
```kotlin
// By saksnummer
val fagsak = fagsakService.hentFagsak("MEL-12345")

// By GSAK/archive ID
val fagsak = fagsakService.hentFagsakFraArkivsakID(123456L)

// By actor
val fagsaker = fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, "2512489212185")
val fagsaker = fagsakService.hentFagsakerMedOrgnr(Aktoersroller.ARBEIDSGIVER, "912345678")
```

### Accessing Actors
```kotlin
// Citizen (bruker)
val bruker: Optional<Aktoer> = fagsak.hentBruker()
val aktørId: String = fagsak.hentBrukersAktørID()  // throws if not found

// Company/Employer
val virksomhet = fagsak.hentVirksomhet()  // throws if not found
val orgnr: Optional<String> = fagsak.finnVirksomhetsOrgnr()
val arbeidsgiver: Optional<Aktoer> = fagsak.hentUnikArbeidsgiver()

// Authorities (EU/EEA)
val myndigheter: List<Aktoer> = fagsak.hentMyndigheter()
```

### Accessing Treatments
```kotlin
// Active treatment (non-årsavregning)
val behandling: Optional<Behandling> = fagsak.finnAktivBehandlingIkkeÅrsavregning()
val behandling: Behandling = fagsak.hentAktivBehandling()  // throws if not found

// Annual reconciliations
val årsavregninger: List<Behandling> = fagsak.hentAktiveÅrsavregninger()

// Closed treatments
val inactive: List<Behandling> = fagsak.hentInaktiveBehandlinger()

// Sorted
val newest: List<Behandling> = fagsak.hentBehandlingerSortertSynkendePåRegistrertDato()
```

### Type Checking
```kotlin
fagsak.erLovvalg()              // Is lovvalg case
fagsak.erSakstypeEøs()          // Is EU_EOS
fagsak.erSakstypeTrygdeavtale() // Is TRYGDEAVTALE
fagsak.erSakstypeFtrl()         // Is FTRL
fagsak.erSakstemaTrygdeavgift() // Is TRYGDEAVGIFT theme
```

### Closing a Case
```kotlin
fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.OPPHØRT)
```

## Important Gotchas

### 1. Single Active Treatment Assumption
Current implementation assumes max 1 active treatment per case (excluding årsavregning).
```kotlin
// Throws TekniskException if multiple active treatments found
val behandling = fagsak.hentAktivBehandling()
```
TODO: MELOSYS-6520 - refactor for multi-treatment support.

### 2. Actor Role Uniqueness
Only one actor per role type allowed (except TRYGDEMYNDIGHET which can be multiple).
```kotlin
// Throws: "Det finnes mer enn en aktør med rollen X for sak Y"
```

### 3. Eager Loading
Both `aktører` and `behandlinger` use `FetchType.EAGER` - all loaded with case.

### 4. EU/EEA vs Treaty Authorities
```kotlin
// EU/EEA: use institusjonID (format: "landkode:institusjonskode")
fagsakService.oppdaterMyndigheterForEuEos(fagsak, listOf("SE:1234", "DK:5678"))

// Treaty: use land code
fagsakService.oppdaterMyndighetForTrygdeavtale(fagsak, Land_iso2.US)
```

### 5. Type/Theme Change Constraints
```kotlin
// Returns false if multiple treatments or no active non-annual treatment
if (!fagsak.kanEndreTypeOgTema()) {
    throw FunksjonellException("Kan ikke endre type/tema")
}
```

## Debugging

### Find Case by Saksnummer
```sql
SELECT * FROM fagsak WHERE saksnummer = 'MEL-12345';
```

### Find All Actors for a Case
```sql
SELECT a.* FROM aktoer a
WHERE a.fagsak_saksnummer = 'MEL-12345';
```

### Find Cases for a Person
```sql
SELECT f.* FROM fagsak f
JOIN aktoer a ON a.fagsak_saksnummer = f.saksnummer
WHERE a.rolle = 'BRUKER' AND a.aktoer_id = '2512489212185';
```

### Find Cases with Multiple Active Treatments (problematic)
```sql
SELECT f.saksnummer, COUNT(*) as count
FROM fagsak f
JOIN behandling b ON b.fagsak_saksnummer = f.saksnummer
WHERE b.status NOT IN ('AVSLUTTET', 'LUKKET')
AND b.type != 'AARSAVREGNING'
GROUP BY f.saksnummer
HAVING COUNT(*) > 1;
```

## Detailed Documentation

- **[Actors](references/actors.md)**: Actor types, roles, and management
- **[Status Transitions](references/status.md)**: Valid status changes and constraints
