# Vilkår Debugging Guide

## SQL Queries

### Vilkår Analysis

```sql
-- All vilkårsresultater for a behandling
SELECT
    vr.id,
    vr.vilkaar,
    vr.oppfylt,
    vr.begrunnelse_fritekst,
    b.id as behandling_id,
    b.status as behandling_status,
    br.bestemmelse
FROM vilkaarsresultat vr
JOIN behandlingsresultat br ON vr.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
WHERE b.id = :behandlingId;

-- Vilkår with their begrunnelser
SELECT
    vr.id,
    vr.vilkaar,
    vr.oppfylt,
    vrb.begrunnelse
FROM vilkaarsresultat vr
LEFT JOIN vilkaarsresultat_begrunnelser vrb ON vrb.vilkaarsresultat_id = vr.id
JOIN behandlingsresultat br ON vr.behandlingsresultat_id = br.id
WHERE br.behandling_id = :behandlingId;
```

### Missing Vilkår Detection

```sql
-- Behandlinger with bestemmelse but no vilkår evaluated
SELECT
    b.id as behandling_id,
    f.saksnummer,
    br.bestemmelse,
    b.status
FROM behandling b
JOIN fagsak f ON b.fagsak_id = f.id
JOIN behandlingsresultat br ON br.behandling_id = b.id
WHERE br.bestemmelse IS NOT NULL
AND b.status != 'AVSLUTTET'
AND NOT EXISTS (
    SELECT 1 FROM vilkaarsresultat vr
    WHERE vr.behandlingsresultat_id = br.id
);

-- Avslåtte vilkår without begrunnelse
SELECT
    vr.id,
    vr.vilkaar,
    b.id as behandling_id,
    f.saksnummer
FROM vilkaarsresultat vr
JOIN behandlingsresultat br ON vr.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE vr.oppfylt = 0
AND vr.begrunnelse_fritekst IS NULL
AND NOT EXISTS (
    SELECT 1 FROM vilkaarsresultat_begrunnelser vrb
    WHERE vrb.vilkaarsresultat_id = vr.id
);
```

### Historical Analysis

```sql
-- Compare vilkår between original and re-assessment
WITH original AS (
    SELECT vr.vilkaar, vr.oppfylt, 'ORIGINAL' as source
    FROM vilkaarsresultat vr
    JOIN behandlingsresultat br ON vr.behandlingsresultat_id = br.id
    WHERE br.behandling_id = :opprinneligBehandlingId
),
ny AS (
    SELECT vr.vilkaar, vr.oppfylt, 'NY' as source
    FROM vilkaarsresultat vr
    JOIN behandlingsresultat br ON vr.behandlingsresultat_id = br.id
    WHERE br.behandling_id = :nyBehandlingId
)
SELECT * FROM original
UNION ALL
SELECT * FROM ny
ORDER BY vilkaar, source;
```

### Validation Queries

```sql
-- Find conflicting citizenship vilkår (both NORSK and ANNEN oppfylt)
SELECT
    b.id as behandling_id,
    f.saksnummer,
    MAX(CASE WHEN vr.vilkaar = 'NORSK_STATSBORGER' THEN vr.oppfylt END) as norsk,
    MAX(CASE WHEN vr.vilkaar = 'ANNEN_STATSBORGER' THEN vr.oppfylt END) as annen
FROM vilkaarsresultat vr
JOIN behandlingsresultat br ON vr.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE vr.vilkaar IN ('NORSK_STATSBORGER', 'ANNEN_STATSBORGER')
GROUP BY b.id, f.saksnummer
HAVING MAX(CASE WHEN vr.vilkaar = 'NORSK_STATSBORGER' THEN vr.oppfylt END) = 1
   AND MAX(CASE WHEN vr.vilkaar = 'ANNEN_STATSBORGER' THEN vr.oppfylt END) = 1;
```

## Common Error Messages

| Message | Cause | Solution |
|---------|-------|----------|
| "Vilkår X er påkrevd for bestemmelse Y" | Vilkår not evaluated | Complete vilkår evaluation |
| "Begrunnelse mangler for avslått vilkår" | oppfylt=false without reason | Add begrunnelse |
| "Kan ikke fatte vedtak før alle vilkår er vurdert" | Incomplete evaluation | Complete all vilkår |
| "Ugyldig kombinasjon av vilkår" | Conflicting selections | Review vilkår logic |
| "Bestemmelse må være satt før vilkårsvurdering" | No bestemmelse selected | Select bestemmelse first |

## Log Patterns

```bash
# VilkaarsvurderingService operations
grep "VilkaarsvurderingService" application.log | grep -E "lagre|hent|valider"

# Vilkår validation failures
grep "FunksjonellException.*ilkår" application.log

# Vilkår fetching
grep "VilkårForBestemmelse" application.log

# AvklarteFakta operations
grep "AvklartefaktaService" application.log
```

## Troubleshooting Flowchart

```
Vilkår not appearing in UI?
├── Is bestemmelse selected?
│   └── No → Select bestemmelse first
├── Is behandlingstema correct?
│   └── Check behandling.behandlingstema matches expected
├── Are conditions for vilkår met?
│   └── Some vilkår depend on land/statsborgerskap
└── Is VilkårForBestemmelse* returning the vilkår?
    └── Debug in VilkårForBestemmelseYrkesaktiv etc.

Cannot save vilkår?
├── Is behandling in editable state?
│   └── Status must not be AVSLUTTET
├── Are required fields filled?
│   └── Check begrunnelse for avslag
└── Is oppfylt set?
    └── Must be true or false, not null

Vedtak blocked by vilkår?
├── Are all required vilkår evaluated?
│   └── Check SQL for missing vilkår
├── Are avslåtte vilkår begrunnet?
│   └── Check SQL for missing begrunnelser
└── Are there conflicting vilkår?
    └── Check citizenship mutual exclusion
```

## Code Entry Points

| Scenario | Entry Point |
|----------|-------------|
| Get vilkår for bestemmelse | `VilkårForBestemmelse.hentVilkår()` |
| Save vilkår evaluation | `VilkaarsvurderingService.lagreVilkaarsvurdering()` |
| Validate before vedtak | `VilkaarsvurderingService.validerVilkårFørVedtak()` |
| Get avklartefakta | `AvklarteFaktaForBestemmelse.hentAvklarteFakta()` |
| Check all oppfylt | `VilkaarsvurderingService.alleVilkårOppfylt()` |

## Testing Vilkår Changes

When adding/modifying vilkår:

1. Unit test the `VilkårForBestemmelse*` class
2. Verify frontend displays the vilkår correctly
3. Test saving with different combinations
4. Test vedtak validation catches incomplete vilkår
5. Test re-assessment carries over vilkår correctly
