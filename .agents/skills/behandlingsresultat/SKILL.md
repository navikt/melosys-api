---
name: behandlingsresultat
description: |
  Expert knowledge of Behandlingsresultat (treatment result) domain in melosys-api.
  Use when: (1) Understanding result types and their meaning (FASTSATT_LOVVALGSLAND, MEDLEM_I_FOLKETRYGDEN, etc.),
  (2) Working with periods - Lovvalgsperiode, Medlemskapsperiode, Anmodningsperiode, Utpekingsperiode,
  (3) Debugging vedtak/decision metadata, criteria evaluation, or social insurance charges,
  (4) Understanding EU/EEA coordination logic and Article 16 exception handling,
  (5) Working with trygdeavgift (social insurance charge) calculations.
---

# Behandlingsresultat Domain

Behandlingsresultat captures the outcome of a Behandling, including which law applies, membership status,
periods, decision metadata, and eligibility criteria results.

## Quick Reference

### Entity Structure
```
Behandling (1)
    ↓ OneToOne
Behandlingsresultat
    ├─ VedtakMetadata (decision date, appeal deadline)
    ├─ Lovvalgsperiode* (law choice periods)
    │   └─ Trygdeavgiftsperiode*
    ├─ Medlemskapsperiode* (membership periods)
    │   └─ Trygdeavgiftsperiode*
    ├─ Anmodningsperiode* (Article 16 request periods)
    │   └─ AnmodningsperiodeSvar
    ├─ Utpekingsperiode* (designation periods)
    ├─ Vilkaarsresultat* (eligibility criteria)
    ├─ Kontrollresultat* (verification results)
    ├─ Avklartefakta* (clarified facts)
    └─ HelseutgiftDekkesPeriode* (EEA pensioner health coverage)
```

### Key Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| `type` | Behandlingsresultattyper | IKKE_FASTSATT, FASTSATT_LOVVALGSLAND, MEDLEM_I_FOLKETRYGDEN, etc. |
| `behandlingsmåte` | Behandlingsmaate | AUTOMATISERT, MANUELT, DELVIS_AUTOMATISERT |
| `fastsattAvLand` | Land_iso2 | Country that determined the result |
| `vedtakMetadata` | VedtakMetadata | Decision date and appeal deadline |
| `begrunnelseFritekst` | String? | Free-text justification |

### Result Types (Behandlingsresultattyper)

| Type | Description | Use Case |
|------|-------------|----------|
| `IKKE_FASTSATT` | Not determined | Initial state |
| `FASTSATT_LOVVALGSLAND` | Law choice country set | EU/EEA lovvalg |
| `FORELOEPIG_FASTSATT_LOVVALGSLAND` | Provisional law choice set | Backs `erUtpeking()` and `erInnvilgelse()` (designation) |
| `MEDLEM_I_FOLKETRYGDEN` | Member of Norwegian NI | FTRL membership |
| `UNNTATT_MEDLEMSKAP` | Exempt from membership | Non-member |
| `AVSLAG_SØKNAD` | Application denied | Rejection |
| `OPPHØRT` | Membership terminated | Termination |
| `ANMODNING_OM_UNNTAK` | Article 16 exception request | Sent to foreign authority |
| `REGISTRERT_UNNTAK` | Registered exception | Exception approved |
| `HENLEGGELSE` | Case dismissed | Dropped case |
| `FERDIGBEHANDLET` | Completed | Generic completion |
| `MEDHOLD` | Appeal upheld | Klage result |
| `OMGJORT` | Decision reversed (fvl § 35) | NY_VURDERING result (no klage) |

The enum has more values than shown here, e.g. `FORELOEPIG_FASTSATT_LOVVALGSLAND`, `REGISTRERT_UNNTAK`,
`DELVIS_GODKJENT_UNNTAK`, `UTPEKING_NORGE_AVVIST`, `AVSLAG_MANGLENDE_OPPL`, `ANNULLERT`, `AVBRUTT`,
`DELVIS_OPPHØRT`, `FASTSATT_TRYGDEAVGIFT`, `HENLEGGELSE_BORTFALT`. See
[Result Types](references/result-types.md) for the validation rules.

## Period Types

### Lovvalgsperiode (Law Choice Period)
Determines which country's social security law applies.

```kotlin
lovvalgsperiode.lovvalgsland        // Land_iso2 - chosen country
lovvalgsperiode.bestemmelse         // LovvalgBestemmelse - regulation (Art. 11, 13, etc.)
lovvalgsperiode.innvilgelsesresultat // INNVILGET, AVSLAATT, OPPHØRT
lovvalgsperiode.medlemskapstype     // PLIKTIG, FRIVILLIG, UNNTATT
lovvalgsperiode.dekning             // Trygdedekninger
```

### Medlemskapsperiode (Membership Period)
Membership in Norwegian National Insurance (folketrygden).

```kotlin
medlemskapsperiode.innvilgelsesresultat  // INNVILGET, AVSLAATT, OPPHØRT
medlemskapsperiode.medlemskapstype       // PLIKTIG, FRIVILLIG, UNNTATT
medlemskapsperiode.trygdedekning         // Coverage type
medlemskapsperiode.bestemmelse           // Membership-specific provision
```

### Anmodningsperiode (Article 16 Request Period)
Request for exception under EU Regulation Article 16.

```kotlin
anmodningsperiode.sendtUtland           // Sent to foreign authority
anmodningsperiode.anmodetAv             // Requested by
anmodningsperiode.anmodningsperiodeSvar // Response from authority
```

### Utpekingsperiode (Designation Period)
Provisional designation of applicable law (EU/EFTA).

## Common Operations

### Fetching Behandlingsresultat
```kotlin
// Basic fetch
val resultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId)

// With specific relations loaded
val resultat = behandlingsresultatService.hentBehandlingsresultatMedAnmodningsperioder(behandlingId)
val resultat = behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(behandlingId)
```

### Status Checks
```kotlin
resultat.erInnvilgelse()           // Is approval (lovvalg)
resultat.erAvslag()                // Is denial
resultat.erOpphørt()               // Is terminated
resultat.erAnmodningOmUnntak()     // Is Article 16 request
resultat.erUtpeking()              // Is designation
resultat.erAutomatisert()          // Was auto-processed
resultat.erRegistrertUnntak()      // Is registered exception
```

### Period Access
```kotlin
// Single period (throws if multiple exist)
val lovvalg = resultat.hentLovvalgsperiode()
val anmodning = resultat.hentAnmodningsperiode()

// Optional access
val lovvalg: Optional<Lovvalgsperiode> = resultat.finnLovvalgsperiode()

// Validated period (any type)
val periode: PeriodeOmLovvalg? = resultat.finnValidertPeriodeOmLovvalg()

// Collections
val medlemsperioder = resultat.medlemskapsperioder  // Set<Medlemskapsperiode>
```

### Trygdeavgift (Social Insurance Charges)
```kotlin
// Get all charge-liable periods
val avgiftsPerioder = resultat.finnAvgiftspliktigPerioder()

// Check overlap with tax year
resultat.harInnvilgetAvgiftspliktigPeriodeSomOverlapperMedÅr(2024)

// Get all charge periods
val trygdeavgift = resultat.trygdeavgiftsperioder
```

### VedtakMetadata (Decision)
```kotlin
resultat.harVedtak()  // Has decision metadata

// Set decision (vedtaksdato is set internally to Instant.now())
resultat.settVedtakMetadata(klagefrist = LocalDate.now().plusWeeks(6))

// Or with an explicit Vedtakstyper
resultat.settVedtakMetadata(vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK, klagefrist = LocalDate.now().plusWeeks(6))
```

### Clearing for Reprocessing
```kotlin
// Clear all periods and clarifications (takes behandlingID: Long)
behandlingsresultatService.tømBehandlingsresultat(behandlingId)

// Clear only membership periods (takes behandlingID: Long)
behandlingsresultatService.tømMedlemskapsperioder(behandlingId)
```

## Important Interfaces

### PeriodeOmLovvalg
Implemented by: Lovvalgsperiode, Anmodningsperiode, Utpekingsperiode
```kotlin
interface PeriodeOmLovvalg {
    val lovvalgsland: Land_iso2
    val bestemmelse: LovvalgBestemmelse?
    // ...
}
```

### AvgiftspliktigPeriode
Implemented by: Lovvalgsperiode, Medlemskapsperiode
```kotlin
interface AvgiftspliktigPeriode {
    val trygdeavgiftsperioder: Set<Trygdeavgiftsperiode>
    // ...
}
```

## Key Services

### BehandlingsresultatService
- `hentBehandlingsresultat(behandlingId)` - Fetch by ID
- `lagreNyttBehandlingsresultat(behandling)` - Create (initial: IKKE_FASTSATT, MANUELT)
- `oppdaterBehandlingsresultattype(id, behandlingsresultattype)` - Update type
- `tømBehandlingsresultat(behandlingID)` - Clear for reprocessing

### AngiBehandlingsresultatService
- `oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(behandlingID, behandlingsresultattype)` - Set type with validation and close case
- Validates result type against case type, theme, and treatment type

### ReplikerBehandlingsresultatService
- `replikerBehandlingsresultat()` - Clone result for replicated treatment
- Deep clones all relationships, resets to IKKE_FASTSATT/MANUELT

## Important Gotchas

### 1. Single Period Assumption
Some methods assume only one period of a type exists:
```kotlin
// Throws if multiple lovvalgsperioder exist
resultat.hentLovvalgsperiode()

// Safe alternative
resultat.finnLovvalgsperiode()  // Returns Optional
```

### 2. Type-Driven Logic
Business logic depends heavily on `type` and `fagsak.erLovvalg()`:
```kotlin
// Different period collections based on case type
val perioder = if (fagsak.erLovvalg()) {
    resultat.lovvalgsperioder
} else {
    resultat.medlemskapsperioder
}
```

### 3. Cascade Deletion
All child entities use `CascadeType.ALL` with `orphanRemoval = true`.
Removing a period from the collection deletes it from the database.

### 4. Conditional Eager Loading
Use specific service methods to optimize loading:
```kotlin
// Only loads anmodningsperioder
hentBehandlingsresultatMedAnmodningsperioder()

// Loads both membership and lovvalg
hentResultatMedMedlemskapOgLovvalg()
```

## Debugging

### Find Behandlingsresultat
```sql
SELECT br.* FROM behandlingsresultat br
WHERE br.behandling_id = :behandlingId;
```

### Find All Periods for a Result
```sql
-- Lovvalgsperioder (table lovvalg_periode, FK beh_resultat_id)
SELECT * FROM lovvalg_periode WHERE beh_resultat_id = :id;

-- Medlemskapsperioder (FK behandlingsresultat_id)
SELECT * FROM medlemskapsperiode WHERE behandlingsresultat_id = :id;

-- Anmodningsperioder (FK beh_resultat_id)
SELECT * FROM anmodningsperiode WHERE beh_resultat_id = :id;
```

### Find Results Without Decision
```sql
SELECT br.behandling_id, b.status
FROM behandlingsresultat br
JOIN behandling b ON b.id = br.behandling_id
LEFT JOIN vedtak_metadata vm ON vm.behandlingsresultat_id = br.behandling_id
WHERE vm.behandlingsresultat_id IS NULL
AND br.resultat_type != 'IKKE_FASTSATT';
```

### Find Results with Multiple Periods (potential issue)
```sql
SELECT br.behandling_id, COUNT(*) as period_count
FROM behandlingsresultat br
JOIN lovvalg_periode lp ON lp.beh_resultat_id = br.behandling_id
GROUP BY br.behandling_id
HAVING COUNT(*) > 1;
```

## Detailed Documentation

- **[Period Types](references/periods.md)**: Deep dive into period types and their relationships
- **[Result Types](references/result-types.md)**: Complete result type reference
- **[Trygdeavgift](references/trygdeavgift.md)**: Trygdeavgiftsperiode entities attached to result periods
