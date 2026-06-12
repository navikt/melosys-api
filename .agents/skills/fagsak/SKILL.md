---
name: fagsak
description: |
  Expert knowledge of Fagsak (case) domain in melosys-api.
  Use when: (1) Understanding the fagsak lifecycle and status transitions,
  (2) Working with actors (aktører) - citizens, employers, authorities,
  (3) Debugging case creation, closure, or type/theme changes,
  (4) Understanding relationship between fagsak and behandling,
  (5) Working with EU/EEA authorities or bilateral treaty authorities.
  Triggers: "fagsak", "saksnummer", "MEL-", "aktør", "sakstype", "sakstema", "avslutt sak".
---

# Fagsak Domain

Fagsak (case/dossier) is the top-level entity representing a social security case in melosys-api.
Each fagsak contains one or more behandlinger (treatments) and multiple aktører (actors).

## Quick Reference

### Module Structure
```
domain/
├── Fagsak.kt              # Main entity (saksnummer, type, tema, status) - Kotlin
├── Aktoer.java            # Actor entity (citizen, employer, authority) - Java
└── Behandling.kt          # Treatment entity (child of Fagsak) - Kotlin

repository/
└── FagsakRepository.java  # CRUD + custom queries

service/sak/
└── FagsakService.java     # Business operations
```

The kodeverk enums (`Sakstyper`, `Sakstemaer`, `Saksstatuser`, `Aktoersroller`) are
**not local source files** - they ship from the external `melosys-internt-kodeverk`
dependency in package `no.nav.melosys.domain.kodeverk`:
- `Sakstyper`     - EU_EOS, TRYGDEAVTALE, FTRL, etc.
- `Sakstemaer`    - MEDLEMSKAP_LOVVALG, TRYGDEAVGIFT, etc.
- `Saksstatuser`  - OPPRETTET, ANNULLERT, OPPHØRT, HENLAGT, VIDERESENDT, etc.
- `Aktoersroller` - BRUKER, VIRKSOMHET, ARBEIDSGIVER, TRYGDEMYNDIGHET, FULLMEKTIG, etc.

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
// Citizen (bruker) - Fagsak.kt returns Kotlin nullable types, not Java Optional
val bruker: Aktoer? = fagsak.hentBruker()
val aktørId: String = fagsak.hentBrukersAktørID()  // throws if not found
val aktørIdEllerNull: String? = fagsak.finnBrukersAktørID()

// Company/Employer
val virksomhet: Aktoer = fagsak.hentVirksomhet()  // throws if not found
val orgnr: String? = fagsak.finnVirksomhetsOrgnr()
val arbeidsgiver: Aktoer? = fagsak.hentUnikArbeidsgiver()

// Authorities (EU/EEA)
val myndigheter: List<Aktoer> = fagsak.hentMyndigheter()
```

### Accessing Treatments
```kotlin
// Active treatment (non-årsavregning)
val behandling: Behandling? = fagsak.finnAktivBehandlingIkkeÅrsavregning()
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
// Both methods take saksnummer (String), not a Fagsak object - they look up the fagsak internally.

// EU/EEA: use institusjonID (format: "landkode:institusjonskode")
fagsakService.oppdaterMyndigheterForEuEos(saksnummer, listOf("SE:1234", "DK:5678"))

// Treaty: use land code
fagsakService.oppdaterMyndighetForTrygdeavtale(saksnummer, Land_iso2.US)
```

### 5. Type/Theme Change Constraints
```kotlin
// Returns false if multiple treatments or no active non-annual treatment
if (!fagsak.kanEndreTypeOgTema()) {
    throw FunksjonellException("Kan ikke endre type/tema")
}
```

## Debugging

> NOTE: both `aktoer` and `behandling` reference `fagsak` via a column named
> **`saksnummer`** (the FK is named `saksnummer`, not `fagsak_saksnummer`).
> The behandling type column is **`beh_type`** and `ÅRSAVREGNING` is stored verbatim
> (with `Å`). There is no `LUKKET` behandlingsstatus - the closed status is `AVSLUTTET`.

### Find Case by Saksnummer
```sql
SELECT * FROM fagsak WHERE saksnummer = 'MEL-12345';
```

### Find Cases for a Person
```sql
SELECT f.* FROM fagsak f
JOIN aktoer a ON a.saksnummer = f.saksnummer
WHERE a.rolle = 'BRUKER' AND a.aktoer_id = '2512489212185';
```

### Find Cases with Multiple Active Treatments (problematic)
```sql
SELECT f.saksnummer, COUNT(*) as count
FROM fagsak f
JOIN behandling b ON b.saksnummer = f.saksnummer
WHERE b.status != 'AVSLUTTET'
AND b.beh_type != 'ÅRSAVREGNING'
GROUP BY f.saksnummer
HAVING COUNT(*) > 1;
```

See **[Debugging Guide](references/debugging.md)** for more SQL queries, common issues,
log greps, and key code locations.

## Detailed Documentation

- **[Actors](references/actors.md)**: Actor types, roles, and management
- **[Status Transitions](references/status.md)**: Valid status changes and constraints
- **[Debugging Guide](references/debugging.md)**: SQL queries, common issues, and code locations
