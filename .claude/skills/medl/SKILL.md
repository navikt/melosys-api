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
│   ├── MedlemskapRestConsumer.kt   # REST client with @Retryable
│   └── dto/                        # Request/response DTOs

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

**Lovvalgsbestemmelse → GrunnlagMedl**:
```kotlin
// EU Regulation 883/2004
ART_11_3_A → FO_11_3_A
ART_12_1 → FO_12_1
ART_13_1 → FO_13_1
ART_16_1 → FO_16_1

// Bilateral treaties
TRYGDEAVTALE_AUSTRALIA → KONV_AUSTRALIA_*
TRYGDEAVTALE_USA → KONV_USA_*
```

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

**For POST (Create)**:
```kotlin
MedlemskapsunntakForPost(
    ident = fnr,
    fraOgMed = periode.fom,
    tilOgMed = periode.tom,
    status = PeriodestatusMedl.GYLD.name,
    dekning = DekningMedl.FULL.name,
    lovvalgsland = "SWE",  // ISO3
    lovvalg = LovvalgMedl.ENDL.name,
    grunnlag = GrunnlagMedl.FO_12_1.name,
    sporingsinformasjon = SporingsinformasjonForPost(
        kildedokument = KildedokumenttypeMedl.SED.name
    )
)
```

**For PUT (Update)**:
```kotlin
MedlemskapsunntakForPut(
    unntakId = medlPeriodeID,  // Required!
    // ... same fields as POST
    sporingsinformasjon = SporingsinformasjonForPut(
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
-- Check if period was actually created
SELECT * FROM lovvalgsperiode WHERE behandlingsresultat_id = :id;
-- Check medl_periode_id column
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
// Via MedlService
val perioder = medlService.hentPerioder(fnr, fom, tom)
val periode = medlService.hentEksisterendePeriode(medlPeriodeID)
```

### Check Domain Entity
```sql
-- Lovvalgsperiode with MEDL ID
SELECT lp.id, lp.medl_periode_id, lp.fom, lp.tom, lp.lovvalgsland
FROM lovvalgsperiode lp
WHERE lp.behandlingsresultat_id = :behandlingId;

-- Medlemskapsperiode with MEDL ID
SELECT mp.id, mp.medl_periode_id, mp.fom, mp.tom
FROM medlemskapsperiode mp
WHERE mp.behandlingsresultat_id = :behandlingId;
```

### Check Saga Step Execution
```sql
SELECT pi.id, pi.type, pi.status, pi.sist_utforte_steg
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.type LIKE 'IVERKSETT_VEDTAK%';
```

### Logs
```bash
# MEDL REST calls
grep "MedlemskapRestConsumer" application.log

# Saga step execution
grep "LagreMedlemsperiodeMedl\|LagreLovvalgsperiodeMedl" application.log
```

## Detailed Documentation

- **[Enums](references/enums.md)**: Complete MEDL enum reference
- **[Mapping](references/mapping.md)**: Domain to MEDL data mapping
- **[Debugging](references/debugging.md)**: SQL queries and investigation steps
