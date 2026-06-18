---
name: ftrl
description: |
  Expert knowledge of Folketrygdloven (National Insurance Act) processing in melosys-api.
  Use when: (1) Understanding FTRL bestemmelser (§2-5 to §2-13),
  (2) Debugging yrkesaktiv/ikke-yrkesaktiv/pensjonist processing flows,
  (3) Understanding trygdedekning options and combinations,
  (4) Investigating vilkår for each bestemmelse,
  (5) Understanding medlemskapsperiode generation logic,
  (6) Debugging frivillig vs pliktig membership determination.
---

# Folketrygdloven (FTRL) Domain

Folketrygdloven (National Insurance Act) Chapter 2 governs membership in the Norwegian social
insurance scheme for persons outside treaty/EU contexts. This domain handles §2-1 through §2-13,
managing pliktig (mandatory) and frivillig (voluntary) membership applications.

## Quick Reference

### Domain Model Hierarchy

```
Behandlingsresultat
├── medlemskapsperioder: List<Medlemskapsperiode>
│   ├── fom/tom: LocalDate          (columns fom_dato/tom_dato)
│   ├── bestemmelse: Bestemmelse
│   ├── medlemskapstype: Medlemskapstyper (PLIKTIG/FRIVILLIG)
│   ├── trygdedekning: Trygdedekninger   (column trygde_dekning)
│   ├── innvilgelsesresultat: InnvilgelsesResultat   (column innvilgelse_resultat)
│   ├── medlPeriodeID: Long?        (column medlperiode_id)
│   └── trygdeavgiftsperioder: Set<Trygdeavgiftsperiode>
└── trygdeavgiftType: Trygdeavgift_typer?
```

Note: `Medlemskapsperiode` (`domain/.../Medlemskapsperiode.kt`) has no `arbeidsland` column
(dropped in V98), and no direct `skatteforholdTilNorge`/`inntektsperioder` collections; only
`trygdeavgiftsperioder` is mapped directly (via `grunnlagMedlemskapsperiode`).

### FTRL Bestemmelser (Paragraphs)

| Paragraph | Type | Description |
|-----------|------|-------------|
| **§2-1** | Pliktig | Bosatt i Norge - grunnregel |
| **§2-2** | Pliktig | Arbeidstaker i Norge |
| **§2-5.a-g** | Pliktig | Offentlig ansatte (statsforvaltning, forsvaret, etc.) |
| **§2-5.h** | Pliktig | Student med lån/stipend |
| **§2-6** | Pliktig | Sjømenn på norskregistrerte skip |
| **§2-7.1** | Frivillig | Opphold i Norge (ikke bosatt) |
| **§2-7.4** | Frivillig | Familiemedlemmer §2-7 |
| **§2-7a** | Frivillig | Sjømenn bosatt i Norge, utenlandsk skip |
| **§2-8.1.a** | Frivillig | Utsendt arbeidstaker |
| **§2-8.1.b** | Frivillig | Student utenlands |
| **§2-8.1.c** | Frivillig | Offentlig oppdrag/stipend |
| **§2-8.1.d** | Frivillig | Pensjonist/uføretrygdet |
| **§2-8.2** | Frivillig | Andre yrkesaktive (rimelighetsvurdering) |
| **§2-8.4** | Frivillig | Familiemedlemmer §2-8 |
| **§2-13** | Unntak | Unntak fra medlemskap |

### Behandlingstema

The valid bestemmelser per tema are defined in code as `YrkesaktivBestemmelser`,
`IkkeYrkesaktivBestemmelser` and `PensjonistBestemmelser`
(`service/.../ftrl/bestemmelse/`). The lists below mirror those code objects.

| Tema | Description | Bestemmelser (from code) |
|------|-------------|--------------|
| **YRKESAKTIV** | Working persons | §2-1, §2-2, §2-3 andre ledd, §2-5 første ledd a-g, §2-7 første ledd, §2-7a, §2-8.1.a, §2-8.1.b, §2-8 andre ledd, + vertslandsavtaler (Arktisk råds sekretariat art16, Det internasjonale Barentssekretariatet art14, Den nordatlantiske sjøpattedyrkommisjon art16, Tilleggsavtale NATO) |
| **IKKE_YRKESAKTIV** | Non-working persons | §2-1, §2-5 første ledd h, §2-5 andre ledd, §2-7 første ledd, §2-7 fjerde ledd, §2-8.1.b, §2-8.1.c, §2-8 andre ledd, §2-8 fjerde ledd |
| **PENSJONIST** | Pensioners/disabled | §2-1, §2-5 andre ledd, §2-7 første ledd, §2-7 fjerde ledd, §2-8.1.d, §2-8 andre ledd, §2-8 fjerde ledd |

### Trygdedekning (Coverage)

Coverage codes live in the `Trygdedekninger` enum (`domain/.../kodeverk/Trygdedekninger`).
For FTRL the relevant values are `FULL_DEKNING_FTRL` (pliktige + §2-7/§2-7a) and the
`FTRL_2_9_*` / `FTRL_2_7*` series (frivillig §2-8 / §2-7). There is no bare `FULL_DEKNING`,
`HELSEDEL` or `UTEN_DEKNING` value in this enum (the bare `HELSEDEL_*` codes belong to a
different enum, `Avgiftsdekning`).

| Code | Description | Use Case |
|------|-------------|----------|
| `FULL_DEKNING_FTRL` | Full coverage in folketrygden | Pliktige bestemmelser + §2-7/§2-7a |
| `FTRL_2_9_FØRSTE_LEDD_A_HELSE` | §2-9 first ledd a, health only | Frivillig §2-8 |
| `FTRL_2_9_FØRSTE_LEDD_B_PENSJON` | §2-9 first ledd b, pension only | Frivillig §2-8 |
| `FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON` | §2-9 first ledd c, health + pension | Frivillig §2-8 (e.g. pensjonist) |
| `FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER` | §2-7 third ledd b | Frivillig §2-7 |
| `TILLEGGSAVTALE_NATO_HELSEDEL` | NATO supplementary agreement, health | Vertslandsavtale NATO |

(The full `FTRL_2_9_*` matrix — including andre/tredje ledd variants — is enumerated in
`LovligeKombinasjonerTrygdedekningBestemmelse`.)

### Medlemskapstype

| Type | Description |
|------|-------------|
| `PLIKTIG` | Mandatory membership - automatic by law |
| `FRIVILLIG` | Voluntary membership - by application |

## Service Layer

### FtrlBestemmelser
Location: `service/src/main/kotlin/.../ftrl/bestemmelse/FtrlBestemmelser.kt`

`@Component` that resolves the valid bestemmelser for a given `Behandlingstema` (+ optional
`Trygdedekninger`), by intersecting the tema list (`YrkesaktivBestemmelser` /
`IkkeYrkesaktivBestemmelser` / `PensjonistBestemmelser`) with the legal
trygdedekning-combinations from `LovligeKombinasjonerTrygdedekningBestemmelse`.

### YrkesaktivBestemmelser / IkkeYrkesaktivBestemmelser / PensjonistBestemmelser
Location: `service/src/main/kotlin/.../ftrl/bestemmelse/`

Separate files defining valid bestemmelser for each behandlingstema.

### MedlemskapsperiodeService
Location: `service/src/main/kotlin/.../ftrl/medlemskapsperiode/MedlemskapsperiodeService.kt`

Key operations:
- `hentMedlemskapsperioder()` - Get periods for behandling
- `opprettMedlemskapsperiode()` - Create new period
- `oppdaterMedlemskapsperiode()` - Update existing period
- `erstattMedlemskapsperioder()` - Replace periods (ny vurdering)

### OpprettForslagMedlemskapsperiodeService
Location: `service/src/main/kotlin/.../ftrl/medlemskapsperiode/OpprettForslagMedlemskapsperiodeService.kt`

Creates suggested membership periods based on søknadsperiode and rules.

### UtledMedlemskapsperioder
Location: `service/src/main/kotlin/.../ftrl/medlemskapsperiode/UtledMedlemskapsperioder.kt`

Logic for deriving membership periods from input data.

### GyldigeTrygdedekningerService
Location: `service/src/main/kotlin/.../ftrl/GyldigeTrygdedekningerService.kt`

Returns valid trygdedekning options for given context.

### LovligeKombinasjonerTrygdedekningBestemmelse
Location: `service/src/main/kotlin/.../ftrl/bestemmelse/LovligeKombinasjonerTrygdedekningBestemmelse.kt`

Defines valid combinations of trygdedekning and bestemmelse.

## Vilkår System

### Vilkår Structure
Location: `service/src/main/kotlin/.../ftrl/bestemmelse/vilkaar/`

```
VilkårForBestemmelse (interface)
├── VilkårForBestemmelseYrkesaktiv
├── VilkårForBestemmelseIkkeYrkesaktiv
└── VilkårForBestemmelsePensjonist
```

Each bestemmelse has associated vilkår (conditions) that must be evaluated.

### AvklarteFaktaForBestemmelse
Location: `service/src/main/kotlin/.../ftrl/bestemmelse/avklartefakta/`

Defines which avklarte fakta are required for each bestemmelse.

## Processing Flows

### Yrkesaktiv Flow (§2-8)
```
┌─────────────────────┐
│ Søknad mottas       │
│ (behandlingstema:   │
│  YRKESAKTIV)        │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Velg bestemmelse    │
│ (§2-8.1.a, §2-8.2)  │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Avklar vilkår       │
│ (arbeidsgiver,      │
│  arbeidsland, etc.) │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Opprett medlemskaps-│
│ periode med avgift  │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Fatt vedtak         │
│ IVERKSETT_VEDTAK_   │
│ FTRL                │
└─────────────────────┘
```

### Pensjonist Flow (§2-8.1.d)
```
┌─────────────────────┐
│ Søknad mottas       │
│ (behandlingstema:   │
│  PENSJONIST)        │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Bestemmelse:        │
│ §2-8.1.d automatisk │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Trygdedekning:      │
│ FTRL_2_9_*_HELSE    │
│ (kun helsedel)      │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Betalingsvalg:      │
│ FAKTURA/KONTONUMMER │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Fatt vedtak         │
└─────────────────────┘
```

## Saksflyt Integration

### IVERKSETT_VEDTAK_FTRL
Step sequence (from `ProsessflytDefinisjon`):
```
LAGRE_PERSONOPPLYSNINGER →
LAGRE_MEDLEMSKAPSPERIODE_MEDL →
OPPRETT_FAKTURASERIE →
AVSLUTT_SAK_OG_BEHANDLING →
SEND_MELDING_OM_VEDTAK →
OPPRETTE_AARSAVREGNING_ENDRING →
RESET_ÅPNE_ÅRSAVREGNINGER
```

## Debugging Queries

> Schema note: `medlemskapsperiode.behandlingsresultat_id` is the FK to
> `behandlingsresultat`, whose PK is `behandling_id` (1:1 with `behandling`, no separate
> `id` column). So `mp.behandlingsresultat_id` equals the `behandling.id`. `behandling` links
> to `fagsak` via `saksnummer` (not `fagsak_id`); `fagsak`'s PK is `saksnummer` and its type
> column is `fagsak_type`. There is no `bruker_aktor_id` on `fagsak` — the bruker's aktørID
> lives in the `aktoer` table (column `aktoer_id`, `rolle = 'BRUKER'`, joined on `saksnummer`).

### Find Medlemskapsperiode by Behandling
```sql
SELECT mp.*, b.status
FROM medlemskapsperiode mp
JOIN behandlingsresultat br ON mp.behandlingsresultat_id = br.behandling_id
JOIN behandling b ON br.behandling_id = b.id
WHERE b.id = :behandlingId;
```

### Check Bestemmelse and Dekning
```sql
SELECT mp.id, mp.fom_dato, mp.tom_dato,
       mp.bestemmelse, mp.trygde_dekning,
       mp.medlemskapstype,
       mp.innvilgelse_resultat
FROM medlemskapsperiode mp
WHERE mp.behandlingsresultat_id = :behandlingId;
```

### Find FTRL Cases for Person
```sql
SELECT f.saksnummer, f.fagsak_type, b.id, b.status, f.tema
FROM fagsak f
JOIN aktoer a ON a.saksnummer = f.saksnummer AND a.rolle = 'BRUKER'
JOIN behandling b ON b.saksnummer = f.saksnummer
WHERE a.aktoer_id = :aktorId
AND f.fagsak_type = 'FTRL'
ORDER BY b.registrert_dato DESC;
```

### Check MEDL Registration
```sql
SELECT mp.medlperiode_id, mp.bestemmelse, mp.fom_dato, mp.tom_dato
FROM medlemskapsperiode mp
WHERE mp.behandlingsresultat_id = :behandlingId
AND mp.medlperiode_id IS NOT NULL;
```

## Common Issues

| Issue | Symptoms | Investigation |
|-------|----------|---------------|
| Wrong bestemmelse | Incorrect paragraph in vedtak | Check vilkårsvurdering flow |
| Invalid dekning | Validation error | Check LovligeKombinasjonerTrygdedekningBestemmelse |
| Period overlap | Cannot save medlemskapsperiode | Check existing periods for fagsak |
| Missing MEDL | medlperiode_id is NULL | Check LAGRE_MEDLEMSKAPSPERIODE_MEDL step |
| Avgift calculation fail | No trygdeavgiftsperiode | Verify skatteforhold and inntekt grunnlag |

## Bestemmelse Details

### §2-8 First Paragraph (Frivillig Yrkesaktiv)

**a) Utsendt arbeidstaker**
- Norwegian employer sends to work abroad
- Employer has substantial activity in Norway
- Previously subject to Norwegian legislation

**b) Student utenlands**
- Studying abroad with Norwegian loan/stipend
- Enrolled at recognized institution

**c) Offentlig oppdrag/stipend**
- Government assignment abroad
- Research stipend from Norwegian sources

**d) Pensjonist/uføretrygdet**
- Receiving pension from folketrygden
- Only helsedel coverage available

### §2-8 Second Paragraph
- Other workers where "særlige grunner" make it reasonable
- Discretionary evaluation (rimelighetsvurdering)

### §2-13 Unntak
- Exception from mandatory membership
- Used when person should not be member despite §2-1/§2-2

## Detailed Documentation

- **[Debugging Guide](references/debugging.md)**: SQL queries and troubleshooting
