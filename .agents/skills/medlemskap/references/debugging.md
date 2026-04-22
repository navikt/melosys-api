# Medlemskap Debugging Guide

## SQL Queries

### Period Analysis

```sql
-- All medlemskapsperioder for a behandling with full context
SELECT
    mp.id,
    mp.fom_dato,
    mp.tom_dato,
    mp.innvilgelse_resultat,
    mp.medlemskapstype,
    mp.trygde_dekning,
    mp.bestemmelse,
    mp.medlperiode_id,
    b.id as behandling_id,
    b.status as behandling_status,
    f.saksnummer,
    f.type as sakstype
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE b.id = :behandlingId;

-- Find all periods for a person across all saker
SELECT
    f.saksnummer,
    b.id as behandling_id,
    b.status,
    mp.fom_dato,
    mp.tom_dato,
    mp.innvilgelse_resultat,
    mp.medlemskapstype,
    mp.medlperiode_id
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE f.bruker_aktor_id = :aktorId
ORDER BY mp.fom_dato DESC;

-- Periods missing MEDL sync
SELECT
    f.saksnummer,
    b.id as behandling_id,
    b.status,
    mp.id,
    mp.fom_dato,
    mp.innvilgelse_resultat
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE mp.innvilgelse_resultat = 'INNVILGET'
AND mp.medlperiode_id IS NULL
AND b.status = 'AVSLUTTET';
```

### Trygdeavgift Linkage

```sql
-- Medlemskapsperioder with their avgiftsperioder
SELECT
    mp.id as medlemskap_id,
    mp.fom_dato,
    mp.tom_dato,
    mp.medlemskapstype,
    tp.id as avgift_id,
    tp.periode_fra,
    tp.periode_til,
    tp.trygdeavgiftsbeloep_md
FROM medlemskapsperiode mp
LEFT JOIN trygdeavgiftsperiode tp ON tp.grunnlag_medlemskapsperiode_id = mp.id
WHERE mp.behandlingsresultat_id = :behandlingsresultatId
ORDER BY mp.fom_dato, tp.periode_fra;

-- Periods missing avgift calculation
SELECT mp.*
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
WHERE mp.innvilgelse_resultat = 'INNVILGET'
AND mp.medlemskapstype = 'FRIVILLIG'  -- Frivillig requires avgift
AND NOT EXISTS (
    SELECT 1 FROM trygdeavgiftsperiode tp
    WHERE tp.grunnlag_medlemskapsperiode_id = mp.id
)
AND b.status = 'AVSLUTTET';
```

### Validation Queries

```sql
-- Find open-ended periods with wrong bestemmelse
SELECT mp.*, b.id as behandling_id, f.saksnummer
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE mp.tom_dato IS NULL
AND mp.bestemmelse != 'FTRL_KAP2_2_1';

-- Periods with tom < fom (data corruption)
SELECT mp.*, f.saksnummer
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE mp.tom_dato < mp.fom_dato;

-- PLIKTIG type with FRIVILLIG bestemmelse (mismatch)
SELECT mp.*, f.saksnummer
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE mp.medlemskapstype = 'PLIKTIG'
AND mp.bestemmelse LIKE '%2_8%';  -- §2-8 is frivillig
```

### Re-assessment Analysis

```sql
-- Compare original and new behandling periods
WITH original AS (
    SELECT mp.*, 'ORIGINAL' as source
    FROM medlemskapsperiode mp
    JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.id
    WHERE br.behandling_id = :opprinneligBehandlingId
),
new AS (
    SELECT mp.*, 'NEW' as source
    FROM medlemskapsperiode mp
    JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.id
    WHERE br.behandling_id = :nyBehandlingId
)
SELECT * FROM original
UNION ALL
SELECT * FROM new
ORDER BY fom_dato, source;
```

## Common Error Messages

### FunksjonellException Messages

| Message | Cause | Solution |
|---------|-------|----------|
| "Tom-dato er påkrevd" | tom is NULL but required | Only FTRL_KAP2_2_1 with land=NO allows null tom |
| "Fom-dato, innvilgelsesresultat, bestemmelse og trygdedekning er påkrevd" | Missing required fields | Ensure all fields populated |
| "Trygedekning X støttes ikke for behandlingstema Y og bestemmelse Z" | Invalid combination | Check LovligeKombinasjonerTrygdedekningBestemmelse |
| "Tom-dato kan ikke være før fom-dato" | Invalid period range | Fix dates |
| "Kan ikke opprette medlemskapsperioder for sakstype X" | Wrong sakstype | Only FTRL sakstype supports medlemskapsperioder |
| "Bestemmelse er ikke satt. Krever bestemmelse ved opprettelse av forslag" | Missing bestemmelse | Set bestemmelse before generating proposals |
| "Ulovlig kombinasjon av bestemmelse X og trygdedekning Y" | Invalid combo | Check allowed combinations |
| "Vilkår X er påkrevd for bestemmelse Y" | Missing vilkår | Fulfill required vilkår first |

### IkkeFunnetException Messages

| Message | Cause | Solution |
|---------|-------|----------|
| "Behandling X har ingen medlemskapsperiode med id Y" | Period not found | Check behandlingsresultat_id matches |
| "Finner ingen medlemskapsperiode med id X for behandling Y" | Period deleted or wrong ID | Verify period exists |

## Log Patterns

```bash
# MedlemskapsperiodeService operations
grep "MedlemskapsperiodeService" application.log | grep -E "opprett|oppdater|slett"

# Period proposal generation
grep "OpprettForslagMedlemskapsperiodeService" application.log

# MEDL sync issues
grep "LagreMedlemsperiodeMedl" application.log | grep -E "ERROR|WARN"

# Validation failures
grep "FunksjonellException.*medlemskap" application.log
```

## Troubleshooting Flowchart

```
Period creation failing?
├── Is sakstype FTRL?
│   └── No → medlemskapsperioder only for FTRL
├── Is bestemmelse set?
│   └── No → Set bestemmelse first
├── Is trygdedekning valid for bestemmelse?
│   └── No → Check LovligeKombinasjonerTrygdedekningBestemmelse
├── Are required vilkår fulfilled?
│   └── No → Complete vilkårsvurdering
├── Is tom null but bestemmelse != FTRL_KAP2_2_1?
│   └── Yes → tom required for this bestemmelse
└── Is tom < fom?
    └── Yes → Fix date range

MEDL sync failing?
├── Check prosessinstans for LAGRE_MEDLEMSKAPSPERIODE_MEDL
├── Check medlPeriodeID on period
├── Check MEDL API availability
└── Check MedlemskapRestConsumer logs
```

## Code Entry Points

| Scenario | Entry Point |
|----------|-------------|
| Create period | `MedlemskapsperiodeService.opprettMedlemskapsperiode()` |
| Update period | `MedlemskapsperiodeService.oppdaterMedlemskapsperiode()` |
| Generate proposals | `OpprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder()` |
| Delete period | `MedlemskapsperiodeService.slettMedlemskapsperiode()` |
| Re-assessment replace | `MedlemskapsperiodeService.erstattMedlemskapsperioder()` |
| MEDL sync | `LagreMedlemsperiodeMedl.utfør()` (saksflyt steg) |
