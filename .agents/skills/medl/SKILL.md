---
name: medl
description: |
  Expert knowledge of MEDL (Medlemskapsregister) integration in melosys-api.
  Use when: (1) Debugging MEDL period creation/update failures,
  (2) Understanding data mapping between domain entities and MEDL API,
  (3) Working with saga steps that write to MEDL (LagreMedlemsperiodeMedl, LagreLovvalgsperiodeMedl),
  (4) Understanding MEDL enums (PeriodestatusMedl, LovvalgMedl, DekningMedl, GrunnlagMedl),
  (5) Troubleshooting Article 13 provisional vs final period logic.
---

# MEDL Integration

MEDL (Medlemskapsregister) is NAV's central registry for membership and law choice periods.
Melosys writes periods to MEDL when vedtak is made.

## Quick Reference

### Module Structure
```
integrasjon/
├── medl/
│   ├── MedlService.kt              # Core service orchestrating operations
│   ├── MedlemskapClient.kt         # REST client (open class, @Retryable)
│   ├── MedlPeriodeKonverter.kt     # Domain → MEDL enum mapping (object)
│   └── api/v1/                     # Request/response DTOs
│       ├── MedlemskapsunntakForPost.kt
│       ├── MedlemskapsunntakForPut.kt
│       ├── MedlemskapsunntakForGet.kt
│       ├── MedlemskapsunntakSoekRequest.kt
│       └── Sporingsinformasjon.kt

service/medl/
├── MedlPeriodeService.java         # Business logic for period operations
└── MedlAnmodningsperiodeService.java

saksflyt/steg/medl/
├── LagreMedlemsperiodeMedl.kt      # FTRL membership periods
├── LagreLovvalgsperiodeMedl.kt     # EU/EEA law choice periods
├── LagreAnmodningsperiodeIMedl.java # Article 16 requests
├── AvsluttTidligereMedlPeriode.java
└── AvsluttTidligereMedlAnmodningsperiode.java
```

### Key Operations

| Operation | Method | Status |
|-----------|--------|--------|
| Create final period | `opprettPeriodeEndelig()` | GYLD |
| Create provisional | `opprettPeriodeForeløpig()` | UAVK |
| Create under clarification | `opprettPeriodeUnderAvklaring()` | UAVK |
| Create terminated | `opprettOpphørtPeriode()` | AVST |
| Update period | `oppdaterPeriode*()` | Various |
| Reject/void period | `avvisPeriode()` | AVST |

### MEDL Enums

**PeriodestatusMedl** (Period Status):

| Code | Meaning |
|------|---------|
| `GYLD` | Valid/approved |
| `UAVK` | Under clarification/pending |
| `AVST` | Ceased/terminated |

**LovvalgMedl** (Law Choice):

| Code | Meaning |
|------|---------|
| `ENDL` | Final (endelig) |
| `FORL` | Provisional (foreløpig) |
| `UAVK` | Under clarification |

**StatusaarsakMedl** (Status Reason):

| Code | Meaning |
|------|---------|
| `AVVIST` | Rejected |
| `FEILREGISTRERT` | Misregistered |
| `OPPHORT` | Terminated |

## Saga Steps

### LagreMedlemsperiodeMedl
For FTRL membership periods (non-EU/EEA).

**Skips when**:
- `behandling.erEøsPensjonist()` is true
- `fagsak.erLovvalg()` is true

**Behavior**:
- First-time: Creates only approved (INNVILGET) periods
- Re-assessment: Replaces periods from original treatment

### LagreLovvalgsperiodeMedl
For EU/EEA law choice periods. Most complex logic.

**Skips when**:
- Registration exemption not approved
- Tourist ship exemption + first-time treatment
- EØS pensioner
- Non-EU/EEA case type

**Key decision**: Provisional vs Final
```kotlin
// Provisional (FORL) when:
// - Article 13 AND no approved exemption registration
val erForeløpig = erArt13 && !erGodkjentRegistreringUnntak

// Final (ENDL) otherwise
```

### LagreAnmodningsperiodeIMedl
For Article 16 exception request periods.

**Creates**: Period with status UAVK (under clarification)
**Inherits**: medlPeriodeID from original treatment for re-assessments

## Data Mapping

### Domain → MEDL

**Lovvalgsbestemmelse → GrunnlagMedl** (see `MedlPeriodeKonverter.kt` / `GrunnlagMedl.kt` for the authoritative table):
```kotlin
// EU Regulation 883/2004
FO_883_2004_ART11_3A → GrunnlagMedl.FO_11_3_A   // .kode = "FO_11_3_a"
FO_883_2004_ART12_1  → GrunnlagMedl.FO_12_1
FO_883_2004_ART13_1A → GrunnlagMedl.FO_13_1_A   // .kode = "FO_13_1_a"
FO_883_2004_ART16_1  → GrunnlagMedl.FO_16       // ART16_1 and ART16_2 both map to FO_16

// Bilateral treaties (constant names are the country name, no KONV_ prefix)
Lovvalgsbestemmelser_trygdeavtale_au.AUS → GrunnlagMedl.AUSTRALIA  // .kode = "Australia"
Lovvalgsbestemmelser_trygdeavtale_us.USA → GrunnlagMedl.USA        // .kode = "USA"
// (KONV_STORBRIT_NIRLAND_* values exist only for the EFTA/Storbritannia konvensjon)
```
Note: GrunnlagMedl constants carry a `.kode` string that often differs from the constant name
(e.g. `FO_11_3_A.kode == "FO_11_3_a"`). Production code transmits `.kode`, not `.name`.

**Trygdedekninger → DekningMedl**:
```kotlin
FULL_DEKNING → FULL
UTEN_DEKNING → UNNTATT
// FTRL-specific mappings for chapter 2 provisions
```

**Land → ISO3 Country Code**:
```kotlin
// Uses IsoLandkodeKonverterer
Land_iso2.SE → "SWE"
Land_iso2.DK → "DNK"
```

### DTOs

**For POST (Create)** — `MedlService.opprettPeriode` builds the request in two stages: the
constructor sets `fraOgMed`/`tilOgMed`/`dekning`/`lovvalgsland`/`grunnlag`, then `.apply{}`
sets `sporingsinformasjon`/`ident`/`lovvalg`/`status`/`statusaarsak`. All wire values use `.kode`:
```kotlin
MedlemskapsunntakForPost(
    fraOgMed = periode.fom,
    tilOgMed = periode.tom,
    dekning = DekningMedl.FULL.kode,
    lovvalgsland = "SWE",                 // ISO3
    grunnlag = GrunnlagMedl.FO_12_1.kode  // .kode == "FO_12_1"
).apply {
    ident = fnr
    lovvalg = LovvalgMedl.ENDL.kode
    status = PeriodestatusMedl.GYLD.kode
    sporingsinformasjon = MedlemskapsunntakForPost.SporingsinformasjonForPost(
        kildedokument = KildedokumenttypeMedl.SED.kode
    )
}
```

**For PUT (Update)**:
```kotlin
MedlemskapsunntakForPut(
    unntakId = medlPeriodeID,  // Required!
    // ... same fields as POST
    sporingsinformasjon = MedlemskapsunntakForPut.SporingsinformasjonForPut(
        kildedokument = "...",
        versjon = existingVersjon  // Required for optimistic locking!
    )
)
```

## Common Issues

### 1. Null medlPeriodeID
**Symptom**: `TekniskException: "Opprettelse av periode i MEDL feilet med retur av null medlPeriodeID"`

**Cause**: MEDL returned null ID after creation

**Investigation**:
```sql
-- Check if period was actually created (table is lovvalg_periode; MEDL id col is medlperiode_id)
SELECT * FROM lovvalg_periode WHERE beh_resultat_id = :id;
```

### 2. Version Mismatch
**Symptom**: 409 Conflict or optimistic locking error

**Cause**: PUT request has stale `versjon`

**Solution**: Always fetch current period before update to get latest version

### 3. Unsupported Dekning/Grunnlag
**Symptom**: `TekniskException: "Dekningstype støttes ikke"` or `"Lovvalgsbestemmelse støttes ikke i MEDL"`

**Cause**: Domain value has no mapping to MEDL enum

**Investigation**: Check `MedlPeriodeKonverter` for supported mappings

### 4. Article 13 Provisional Logic
**Symptom**: Period created as provisional when should be final (or vice versa)

**Key check**:
```kotlin
// Must be Article 13 AND no approved exemption
val erForeløpig = harBestemmelse(ART_13_*) && !erGodkjentRegistreringUnntak
```

### 5. Missing Overgangsregler
**Symptom**: `FunksjonellException: "Grunnlaget [code] og overgangsregler skal benyttes, men er tom"`

**Cause**: Art. 87.8 or 87a requires transition rules but none provided

## REST Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/rest/v1/periode/soek` | POST | Search periods |
| `/api/v1/medlemskapsunntak` | POST | Create period |
| `/api/v1/medlemskapsunntak/{id}` | GET | Fetch period |
| `/api/v1/medlemskapsunntak` | PUT | Update period |

## Debugging

### Find Period in MEDL
```kotlin
// Via MedlService (public entry point — returns a Saksopplysning)
val saksopplysning = medlService.hentPeriodeListe(fnr, fom, tom)
// Note: hentEksisterendePeriode(medlPeriodeID) is a PRIVATE helper in MedlService;
// to fetch a single period directly, call MedlemskapClient.hentPeriode(periodeId).
```

### Check Domain Entity
```sql
-- Lovvalgsperiode (table lovvalg_periode; FK beh_resultat_id, MEDL id medlperiode_id)
SELECT lp.id, lp.medlperiode_id, lp.fom_dato, lp.tom_dato, lp.lovvalgsland
FROM lovvalg_periode lp
WHERE lp.beh_resultat_id = :behandlingsresultatId;

-- Medlemskapsperiode (table medlemskapsperiode; FK behandlingsresultat_id, MEDL id medlperiode_id)
SELECT mp.id, mp.medlperiode_id, mp.fom_dato, mp.tom_dato
FROM medlemskapsperiode mp
WHERE mp.behandlingsresultat_id = :behandlingsresultatId;
```

### Check Saga Step Execution
```sql
-- Columns: prosess_type (entity field `type`), sist_fullfort_steg (field `sistFullførtSteg`)
SELECT pi.id, pi.prosess_type, pi.status, pi.sist_fullfort_steg
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.prosess_type LIKE 'IVERKSETT_VEDTAK%';
```

### Logs
```bash
# MEDL REST calls
grep "MedlemskapClient" application.log

# Saga step execution
grep "LagreMedlemsperiodeMedl\|LagreLovvalgsperiodeMedl" application.log
```

## Detailed Documentation

- **[Enums](references/enums.md)**: Complete MEDL enum reference
- **[Mapping](references/mapping.md)**: Domain to MEDL data mapping
- **[Debugging](references/debugging.md)**: SQL queries, log patterns, and failure-mode playbook
