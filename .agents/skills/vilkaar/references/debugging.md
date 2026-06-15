# Vilkår Debugging Guide

## Schema notes (read first)

- `vilkaarsresultat.beh_resultat_id` is the FK; it references `behandlingsresultat.behandling_id` (behandlingsresultat has a 1:1 PK = `behandling_id`).
- Begrunnelse-koder live in `vilkaar_begrunnelse` (`vilkaar_resultat_id` FK, `kode` value) — there is no `vilkaarsresultat_begrunnelser` table.
- `behandling` links to `fagsak` via `saksnummer` (fagsak PK is `saksnummer`, not a numeric `id`); behandling has no `fagsak_id`.
- There is no `bestemmelse` column on `behandlingsresultat`; bestemmelse lives on `medlemskapsperiode`.

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
    b.status as behandling_status
FROM vilkaarsresultat vr
JOIN behandlingsresultat br ON vr.beh_resultat_id = br.behandling_id
JOIN behandling b ON br.behandling_id = b.id
WHERE b.id = :behandlingId;

-- Vilkår with their begrunnelse-koder
SELECT
    vr.id,
    vr.vilkaar,
    vr.oppfylt,
    vb.kode
FROM vilkaarsresultat vr
LEFT JOIN vilkaar_begrunnelse vb ON vb.vilkaar_resultat_id = vr.id
WHERE vr.beh_resultat_id = :behandlingId;
```

### Missing Vilkår Detection

```sql
-- Behandlinger (med behandlingsresultat) som mangler vilkårsresultater
SELECT
    b.id as behandling_id,
    b.saksnummer,
    b.status
FROM behandling b
JOIN behandlingsresultat br ON br.behandling_id = b.id
WHERE b.status != 'AVSLUTTET'
AND NOT EXISTS (
    SELECT 1 FROM vilkaarsresultat vr
    WHERE vr.beh_resultat_id = br.behandling_id
);

-- Avslåtte vilkår uten begrunnelse (verken kode eller fritekst)
SELECT
    vr.id,
    vr.vilkaar,
    b.id as behandling_id,
    b.saksnummer
FROM vilkaarsresultat vr
JOIN behandlingsresultat br ON vr.beh_resultat_id = br.behandling_id
JOIN behandling b ON br.behandling_id = b.id
WHERE vr.oppfylt = 0
AND vr.begrunnelse_fritekst IS NULL
AND NOT EXISTS (
    SELECT 1 FROM vilkaar_begrunnelse vb
    WHERE vb.vilkaar_resultat_id = vr.id
);
```

### Historical Analysis

```sql
-- Compare vilkår between original and re-assessment
WITH original AS (
    SELECT vr.vilkaar, vr.oppfylt, 'ORIGINAL' as source
    FROM vilkaarsresultat vr
    WHERE vr.beh_resultat_id = :opprinneligBehandlingId
),
ny AS (
    SELECT vr.vilkaar, vr.oppfylt, 'NY' as source
    FROM vilkaarsresultat vr
    WHERE vr.beh_resultat_id = :nyBehandlingId
)
SELECT * FROM original
UNION ALL
SELECT * FROM ny
ORDER BY vilkaar, source;
```

## Common Error Messages

| Message | Cause | Solution |
|---------|-------|----------|
| `Kan ikke endre vilkår <X>` | Attempt to register an `IMMUTABLE_VILKAAR` (`FO_883_2004_INNGANGSVILKAAR`) | Don't send immutable vilkår in the POST body |
| `Kan ikke finne behandlingsresultat for behandling: <id>` | No behandlingsresultat exists yet | Ensure behandlingsresultat is created first |
| `Arbeidssituasjon <X> er ugyldig` | Missing/invalid `ARBEIDSSITUASJON` avklart faktum for 2-1/2-2 | Send a valid `Arbeidssituasjontype` |
| `FamilieRelasjon <X> er ugyldig` | Missing/invalid `IKKE_YRKESAKTIV_RELASJON` for 2-5/2-8 | Send a valid `Ikkeyrkesaktivrelasjontype` |
| `BehandlingID trengs for å avgjøre land for ftrlKap2_1` | `behandlingID` null for FTRL_KAP2_2_1 routing | Pass behandlingID into `hentVilkår` |

## Log Patterns

```bash
# Vilkår save/read service
grep "VilkaarsresultatService" application.log

# Vilkår routing
grep "VilkårForBestemmelse" application.log

# Avklarte fakta
grep "AvklarteFaktaForBestemmelse" application.log

# Funksjonelle feil knyttet til vilkår
grep "FunksjonellException" application.log | grep -i "vilkår\|arbeidssituasjon\|familierelasjon"
```

## Troubleshooting Flowchart

```
Vilkår not appearing in UI?
├── Is bestemmelse selected?
│   └── No → Select bestemmelse first
├── Is behandlingstema correct?
│   └── Check behandling.beh_tema matches expected (YRKESAKTIV/IKKE_YRKESAKTIV/PENSJONIST)
├── Does the bestemmelse need avklarte fakta first?
│   └── Some (2-1, 2-2, 2-5 andre ledd, 2-8 fjerde ledd) branch on
│       arbeidssituasjon / relasjon / søknadsland before returning vilkår
└── Is VilkårForBestemmelse* returning the vilkår?
    └── Debug in VilkårForBestemmelseYrkesaktiv / IkkeYrkesaktiv / Pensjonist

Cannot save vilkår?
├── Is an IMMUTABLE_VILKAAR being changed?
│   └── FO_883_2004_INNGANGSVILKAAR cannot be registered via this path
├── Does behandlingsresultat exist?
│   └── registrerVilkår throws IkkeFunnetException otherwise
└── Is the Vilkaar.kode valid?
    └── VilkaarDto.vilkaar must map via Vilkaar.valueOf(...)
```

## Code Entry Points

| Scenario | Entry Point |
|----------|-------------|
| Get vilkår for bestemmelse | `VilkårForBestemmelse.hentVilkår()` |
| Get avklarte fakta | `AvklarteFaktaForBestemmelse.hentAvklarteFakta()` |
| Read stored vilkårsresultater | `VilkaarsresultatService.hentVilkaar()` / `VilkaarController` GET |
| Save vilkår evaluation | `VilkaarsresultatService.registrerVilkår()` / `VilkaarController` POST |
| Overstyr inngangsvilkår | `InngangsvilkaarService.overstyrInngangsvilkårTilOppfylt()` |
| Check a vilkår is oppfylt | `VilkaarsresultatService.oppfyllerVilkaar()` |

## Testing Vilkår Changes

When adding/modifying vilkår:

1. Unit test the relevant `VilkårForBestemmelse*` class (see `VilkårForBestemmelse{Yrkesaktiv,IkkeYrkesaktiv,Pensjonist}Test.kt`)
2. Verify frontend displays the vilkår correctly
3. Test saving with different begrunnelse-kode combinations
4. Test that re-assessment (EØS vs non-EØS) carries over / resets vilkår as expected
