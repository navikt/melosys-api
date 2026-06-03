---
name: medlemskap
description: |
  Expert knowledge of membership (medlemskap) domain in melosys-api.
  Use when: (1) Understanding pliktig vs frivillig membership determination,
  (2) Debugging medlemskapsperiode creation and updates,
  (3) Understanding trygdedekning combinations and rules,
  (4) Investigating MEDL synchronization issues,
  (5) Understanding membership status transitions and innvilgelsesresultat,
  (6) Working with UtledMedlemskapsperioder for proposal generation,
  (7) Understanding the relationship between bestemmelse, medlemskapstype, and trygdedekning.
  Triggers: medlemskapsperiode, pliktig, frivillig, trygdedekning, MEDL, innvilgelsesresultat, medlPeriodeID, bestemmelse.
---

# Medlemskap Domain

Medlemskap (membership) determines whether a person is covered by the Norwegian National Insurance
Scheme (folketrygden). The domain handles membership period creation, type determination (pliktig/frivillig),
coverage levels (trygdedekning), and synchronization with the central MEDL registry.

## Quick Reference

### Domain Model Hierarchy

```
Behandlingsresultat
└── medlemskapsperioder: Set<Medlemskapsperiode>

    Medlemskapsperiode
    ├── id: Long
    ├── fom: LocalDate               # Period start date
    ├── tom: LocalDate?              # Period end date (nullable for open-ended)
    ├── innvilgelsesresultat: InnvilgelsesResultat  # INNVILGET, DELVIS_INNVILGET, AVSLAATT, OPPHØRT
    ├── medlemskapstype: Medlemskapstyper           # PLIKTIG, FRIVILLIG, UNNTATT, ...
    ├── trygdedekning: Trygdedekninger              # FULL_DEKNING_FTRL, FTRL_2_9_*, etc.
    ├── bestemmelse: Bestemmelse                    # FTRL paragraph
    ├── medlPeriodeID: Long?         # Reference to MEDL registry
    └── trygdeavgiftsperioder: Set<Trygdeavgiftsperiode>
```

### Key Enums

| Enum | Values | Description |
|------|--------|-------------|
| **InnvilgelsesResultat** | INNVILGET, DELVIS_INNVILGET, AVSLAATT, OPPHØRT | Period outcome |
| **Medlemskapstyper** | PLIKTIG, FRIVILLIG, UNNTATT, DELVIS_UNNTATT, IKKE_MEDLEM | Membership type (mandatory/voluntary/exempt) |
| **Trygdedekninger** | FULL_DEKNING, FULL_DEKNING_FTRL, FULL_DEKNING_EOSFO, UTEN_DEKNING, the FTRL_2_9_* family, FTRL_2_7_*/FTRL_2_7A_*, treaty values (TILLEGGSAVTALE_NATO_HELSEDEL, UNNTATT_USA_5_2_G, UNNTATT_CAN_7_5_B) | Coverage type (stored in column `trygde_dekning`). See `kodeverk` enum `Trygdedekninger` for the full set. |

> **Trygdedekninger vs Avgiftsdekning** — the `HELSEDEL_MED_SYKEPENGER` /
> `HELSEDEL_UTEN_SYKEPENGER` / `PENSJONSDEL_MED_YRKESSKADETRYGD` /
> `PENSJONSDEL_UTEN_YRKESSKADETRYGD` constants are NOT `Trygdedekninger`; they are the
> `Avgiftsdekning` enum, derived from a `Trygdedekninger` value via
> `AvgiftsdekningerFraTrygdedekning` for trygdeavgift. Don't confuse the two.

### Medlemskapstype Determination

Determined by bestemmelse via `UtledMedlemskapstype`:

**PLIKTIG (Mandatory)** - Based on FTRL §2-1, §2-2, §2-5:
```kotlin
// From PliktigeMedlemskapsbestemmelser
- FTRL_KAP2_2_1        // Bosatt i Norge
- FTRL_KAP2_2_2        // Arbeidstaker i Norge
- FTRL_KAP2_2_3_ANDRE_LEDD
- FTRL_KAP2_2_5_FØRSTE_LEDD_A through H  // Various §2-5 provisions
- FTRL_KAP2_2_5_ANDRE_LEDD
- Vertslandsavtale_bestemmelser (NATO, etc.)
```

**FRIVILLIG (Voluntary)** - FTRL §2-8 and special cases:
```kotlin
- FTRL_KAP2_2_15_ANDRE_LEDD  // Special rule: always FRIVILLIG
- All other bestemmelser not in PLIKTIG list
```

### Trygdedekning Options

Valid trygdedekninger per behandlingstema are defined in `GyldigeTrygdedekningerService`
(then intersected with the bestemmelse via `LovligeKombinasjonerTrygdedekningBestemmelse`):

| Behandlingstema | Available Trygdedekninger |
|-----------------|---------------------------|
| YRKESAKTIV | FULL_DEKNING_FTRL, the full FTRL_2_9_* family, FTRL_2_7_TREDJE_LEDD_B_*, FTRL_2_7A_ANDRE_LEDD_B_*, TILLEGGSAVTALE_NATO_HELSEDEL |
| IKKE_YRKESAKTIV | FULL_DEKNING_FTRL, the FTRL_2_9_* family, FTRL_2_7_TREDJE_LEDD_B_*, TILLEGGSAVTALE_NATO_HELSEDEL |
| PENSJONIST | FULL_DEKNING_FTRL, FTRL_2_9_* (helse/pensjon variants), FTRL_2_7_TREDJE_LEDD_B_* |

## Service Layer

### MedlemskapsperiodeService
Location: `service/src/main/kotlin/.../ftrl/medlemskapsperiode/MedlemskapsperiodeService.kt`

Key operations:
- `hentMedlemskapsperioder(behandlingsresultatID)` - Get all periods for treatment
- `opprettMedlemskapsperiode(...)` - Create new period with validation
- `oppdaterMedlemskapsperiode(...)` - Update existing period
- `slettMedlemskapsperiode(behandlingsresultatID, periodeID)` - Delete period
- `erstattMedlemskapsperioder(...)` - Replace periods on re-assessment
- `opprettEllerOppdaterMedlPeriode(...)` - Sync to MEDL registry

### OpprettForslagMedlemskapsperiodeService
Location: `service/src/main/kotlin/.../ftrl/medlemskapsperiode/OpprettForslagMedlemskapsperiodeService.kt`

Key operations:
- `opprettForslagPåMedlemskapsperioder(behandlingID, bestemmelse)` - Generate period proposals

Validation checks:
1. Sakstype must be FTRL (`validerSakstype`)
2. Bestemmelse-trygdedekning combination must be valid (`validerBestemmelse`)
3. Required vilkår must be fulfilled (`validerVilkår`)

### UtledMedlemskapsperioder
Location: `service/src/main/kotlin/.../ftrl/medlemskapsperiode/UtledMedlemskapsperioder.kt`

Key operations:
- `lagMedlemskapsperioder(grunnlag)` - Create periods for first-time treatment
- `lagMedlemskapsperioderForAndregangsbehandling(...)` - Create periods for re-assessment

Logic overview:
- For YRKESAKTIV/IKKE_YRKESAKTIV: Creates periods based on søknadsperiode and mottaksdato
- For PENSJONIST: Creates helsedel periods only
- Handles avslag (rejection) periods before mottaksdato

## Period Lifecycle

### Creation Flow

```
Søknad received → Journalføring → Behandling created
                                        │
                                        ▼
                              OpprettForslagMedlemskapsperiodeService
                              (generates period proposals)
                                        │
                                        ▼
                              Saksbehandler review/adjust
                                        │
                                        ▼
                              fattVedtak() → IVERKSETT_VEDTAK_FTRL
                                        │
                                        ▼
                              LAGRE_MEDLEMSKAPSPERIODE_MEDL (saksflyt steg)
                              (writes to MEDL registry)
```

### InnvilgelsesResultat Transitions

| From | To | Trigger |
|------|-----|---------|
| (new) | INNVILGET | Positive vedtak |
| (new) | AVSLAATT | Rejection vedtak |
| INNVILGET | OPPHØRT | Termination (opphør) |

### Re-assessment (Ny vurdering)

When creating new behandling for existing fagsak:
1. Original periods are copied/referenced via `opprinneligBehandling`
2. New periods can modify or extend original periods
3. `erstattMedlemskapsperioder()` handles:
   - Opphør (terminate) original INNVILGET periods not carried forward
   - Feilregistrer (void) original OPPHØRT periods not carried forward
   - Create/update new periods

## Validation Rules

### Period Field Validation

```kotlin
// Required fields (always)
- fom: LocalDate         // Must be set
- innvilgelsesResultat   // Must be set
- bestemmelse            // Must be set
- trygdedekning          // Must be set

// tom: LocalDate - Required EXCEPT when:
- Land is only Norway (NOR) AND
- Bestemmelse is FTRL_KAP2_2_1 (bosatt i Norge)

// Period logic
- tom cannot be before fom (PeriodeRegler.feilIPeriode)
```

### Bestemmelse-Trygdedekning Validation

```kotlin
// Via LovligeKombinasjonerTrygdedekningBestemmelse
erBestemmelseGyldigForTrygdedekning(bestemmelse, trygdedekning)
```

### Vilkår Validation

Before creating period proposals, required vilkår must be fulfilled:
```kotlin
// Check via Behandlingsresultat
behandlingsresultat.oppfyllerVilkår(vilkårForBestemmelse)
```

## MEDL Integration

### Synchronization Points

| Saga Step | When |
|-----------|------|
| `LAGRE_MEDLEMSKAPSPERIODE_MEDL` | After vedtak, writes INNVILGET periods to MEDL |
| Re-assessment | Updates/terminates existing MEDL periods |
| Opphør | Changes period status to AVST (terminated) |

### medlPeriodeID

- Created when period is first synced to MEDL
- Used for subsequent updates
- NULL means period not yet in MEDL (or MEDL write failed)

### See Also
Refer to `medl` skill for detailed MEDL integration patterns.

## Debugging Queries

### Find Medlemskapsperioder for Behandling
```sql
SELECT mp.*, br.behandling_id
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.id
WHERE br.behandling_id = :behandlingId
ORDER BY mp.fom_dato;
```

### Find Periods by Person
```sql
SELECT mp.*, b.id as behandling_id, f.saksnummer
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE f.bruker_aktor_id = :aktorId
ORDER BY mp.fom_dato DESC;
```

### Check MEDL Sync Status
```sql
SELECT mp.id, mp.fom_dato, mp.tom_dato, mp.innvilgelse_resultat,
       mp.medlemskapstype, mp.medlperiode_id
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.id
WHERE br.behandling_id = :behandlingId
AND mp.medlperiode_id IS NULL;  -- Not synced to MEDL
```

### Find Periods with Trygdeavgift
```sql
SELECT mp.*, tp.id as avgift_id, tp.trygdeavgift_beloep_mnd_verdi, tp.trygdeavgift_beloep_mnd_valuta
FROM medlemskapsperiode mp
JOIN trygdeavgiftsperiode tp ON tp.medlemskapsperiode_id = mp.id
WHERE mp.behandlingsresultat_id = :behandlingsresultatId;
```

### Check Period Validation Issues
```sql
SELECT mp.*, b.tema, f.type as sakstype
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE mp.tom_dato IS NULL
AND mp.bestemmelse != 'FTRL_KAP2_2_1';  -- Open-ended but not bosatt
```

## Common Issues

| Issue | Symptoms | Investigation |
|-------|----------|---------------|
| Wrong medlemskapstype | PLIKTIG shown as FRIVILLIG | Check bestemmelse vs PliktigeMedlemskapsbestemmelser |
| Missing period proposals | No periods generated | Check vilkår fulfillment, bestemmelse-dekning combo |
| Invalid trygdedekning | FunksjonellException | Check LovligeKombinasjonerTrygdedekningBestemmelse |
| tom-dato required error | Can't save open-ended period | Only allowed for FTRL_KAP2_2_1 with land=NO |
| MEDL sync failed | medlPeriodeID is NULL | Check prosessinstans status, MEDL API logs |
| Period overlap | Kontroll warning | Check existing periods in MEDL via MedlemskapDokument |

## Detailed Documentation

- **[Types](references/types.md)**: Pliktig/frivillig/trygdedekning details
- **[Periods](references/periods.md)**: Period management and validation
- **[MEDL Sync](references/medl-sync.md)**: MEDL synchronization
- **[Debugging Guide](references/debugging.md)**: SQL queries and troubleshooting
