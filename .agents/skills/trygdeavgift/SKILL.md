---
name: trygdeavgift
description: |
  Expert knowledge of trygdeavgift (social insurance charge) domain in melosys-api.
  Use when: (1) Understanding avgift calculation for different member types,
  (2) Debugging årsavregning (annual reconciliation) processing,
  (3) Understanding fakturering flow to OEBS/faktureringskomponenten,
  (4) Investigating avgiftssatser and calculation rules per year,
  (5) Understanding skatteforhold and inntektskilder,
  (6) Debugging trygdeavgiftsperiode generation and storage,
  (7) Understanding the 25% rule and minstebeløp calculations.
---

# Trygdeavgift Domain

Trygdeavgift (social insurance charge) is the domain handling calculation, collection, and reconciliation
of social security contributions for members of Norwegian folketrygden. This includes advance
invoicing (forskuddsfakturering), annual reconciliation (årsavregning), and integration with OEBS.

## Quick Reference

### Domain Model Hierarchy

```
Behandlingsresultat
├── trygdeavgiftType: Trygdeavgift_typer
├── trygdeavgiftFritekst: String?
├── medlemskapsperioder: MutableSet<Medlemskapsperiode>
│   └── trygdeavgiftsperioder: Set<Trygdeavgiftsperiode>
├── lovvalgsperioder: MutableSet<Lovvalgsperiode>
│   └── trygdeavgiftsperioder: Set<Trygdeavgiftsperiode>
├── helseutgiftDekkesPerioder: MutableList<HelseutgiftDekkesPeriode>
│   └── trygdeavgiftsperioder: Set<Trygdeavgiftsperiode>
├── årsavregning: Årsavregning?
└── fakturaserieReferanse: String?

Trygdeavgiftsperiode
├── id: Long
├── periodeFra: LocalDate
├── periodeTil: LocalDate?
├── trygdeavgiftsbeløpMd: Penger
├── trygdesats: BigDecimal
├── grunnlagMedlemskapsperiode: Medlemskapsperiode?
├── grunnlagLovvalgsPeriode: Lovvalgsperiode?
├── grunnlagHelseutgiftDekkesPeriode: HelseutgiftDekkesPeriode?
├── grunnlagInntekstperiode: Inntektsperiode
└── grunnlagSkatteforholdTilNorge: SkatteforholdTilNorge

Årsavregning
├── id: Long
├── aar: Int
├── behandlingsresultat: Behandlingsresultat?      # @MapsId — shares PK with Behandlingsresultat
├── tidligereBehandlingsresultat: Behandlingsresultat?   # object reference, not an Id
├── tidligereFakturertBeloep: BigDecimal?
├── beregnetAvgiftBelop: BigDecimal?
├── tilFaktureringBeloep: BigDecimal?
├── harInnbetaltTrygdeavgift: Boolean?
├── innbetaltTrygdeavgift: BigDecimal?
├── manueltAvgiftBeloep: BigDecimal?
├── endeligAvgiftValg: EndeligAvgiftValg?
└── harSkjoennsfastsattInntektsgrunnlag: Boolean
```

### Key Enums

| Enum | Values | Description |
|------|--------|-------------|
| **Trygdeavgift_typer** | FORELØPIG, ENDELIG | Whether the avgift is provisionally or finally set |
| **Avgiftsdekning** | HELSEDEL_UTEN_SYKEPENGER, HELSEDEL_MED_SYKEPENGER, PENSJONSDEL_UTEN_YRKESSKADETRYGD, PENSJONSDEL_MED_YRKESSKADETRYGD | Coverage affecting calculation |
| **Skatteplikttype** | SKATTEPLIKTIG, IKKE_SKATTEPLIKTIG | Whether the person is skattepliktig til Norge (drives NAV vs Skatteetaten collection) |
| **EndeligAvgiftValg** | OPPLYSNINGER_ENDRET, OPPLYSNINGER_ENDRET_MED_PERIODE_FRA_AVGIFTSSYSTEMET, OPPLYSNINGER_UENDRET, MANUELL_ENDELIG_AVGIFT | How the final avgift for an årsavregning is determined |
| **Betalingstype** | FAKTURA, TREKK | Payment method choice (stored on Fagsak.betalingsvalg) |

### Avgift Calculation Flow

```
┌─────────────────────┐
│ Vedtaksfatting      │
│ (fattVedtak)        │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ TrygdeavgiftsService│
│ beregnOgLagre()     │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐     ┌─────────────────────┐
│ melosys-trygdeavgift│────►│ Trygdeavgiftsperiode│
│ -beregning          │     │ created & stored    │
└─────────────────────┘     └─────────┬───────────┘
                                      │
                                      ▼
                            ┌─────────────────────┐
                            │ OPPRETT_FAKTURASERIE│
                            │ (saksflyt steg)     │
                            └─────────┬───────────┘
                                      │
                                      ▼
                            ┌─────────────────────┐
                            │ Fakturerings-       │
                            │ komponenten/OEBS    │
                            └─────────────────────┘
```

## Service Layer

### TrygdeavgiftsberegningService
Location: `service/src/main/kotlin/.../avgift/TrygdeavgiftsberegningService.kt`

Key operations:
- `beregnOgLagreTrygdeavgift()` - Calculate and persist avgift periods
- `lagNyeTrygdeavgiftsperioder()` - Create new avgift periods (private helper)
- `beregnTrygdeavgift()` - Call external beregning module
- `finnFakturamottakerNavn()` - Resolve the invoice recipient's name
- `sjekkTrygdeavgiftSkalBetalesTilNav()` - Determine if NAV collects avgift (private)

### TrygdeavgiftService
Location: `service/src/main/kotlin/.../avgift/TrygdeavgiftService.kt`

Key operations:
- `harFagsakBehandlingerMedTrygdeavgift()` - Check for avgift on case
- `harFakturerbarTrygdeavgift()` - Check if avgift should be invoiced
- `harTrygdeavgift()` - Check if behandling has avgift

### ÅrsavregningService
Location: `service/src/main/kotlin/.../avgift/aarsavregning/ÅrsavregningService.kt`

Key operations:
- `opprettÅrsavregning()` - Create annual reconciliation
- `hentTidligereTrygdeavgiftsgrunnlag()` - Get previous year's basis
- `hentNyttTrygdeavgiftsgrunnlag()` - Get new basis
- `hentGjeldendeBehandlingsresultaterForÅrsavregning()` - Get results for year

### TrygdeavgiftMottakerService
Location: `service/src/main/kotlin/.../avgift/TrygdeavgiftMottakerService.kt`

Key operations:
- `skalBetalesTilNav()` - Determine if avgift is collected by NAV
- `getTrygdeavgiftMottaker()` - Find invoice recipient (overloaded for behandlingID / trygdeavgiftsperioder / behandlingsresultat)

## Saksflyt Integration

### Fakturering Steps

| ProsessSteg | Description |
|-------------|-------------|
| `OPPRETT_FAKTURASERIE` | Create invoice series in faktureringskomponenten |
| `KANSELLER_FAKTURASERIE` | Cancel existing invoice series |
| `OPPDATER_FAKTURAMOTTAKER` | Update invoice recipient |
| `SEND_FAKTURA_AARSAVREGNING` | Send annual reconciliation invoice |
| `BEREGN_OG_SEND_FAKTURA` | Calculate and send invoice (satsendring) |

### Vedtak Flow with Avgift

```
IVERKSETT_VEDTAK_FTRL:
  LAGRE_MEDLEMSKAPSPERIODE_MEDL →
  OPPRETT_FAKTURASERIE →          # Creates invoice in OEBS
  AVSLUTT_SAK_OG_BEHANDLING →
  SEND_MELDING_OM_VEDTAK →
  RESET_ÅPNE_ÅRSAVREGNINGER

IVERKSETT_VEDTAK_AARSAVREGNING:
  SEND_FAKTURA_AARSAVREGNING →    # Annual reconciliation invoice
  VARSLE_PENSJONSOPPTJENING →
  AVSLUTT_SAK_OG_BEHANDLING →
  SEND_MELDING_OM_VEDTAK
```

## Integration with External Systems

### Faktureringskomponenten
Location: `integrasjon/src/main/kotlin/.../faktureringskomponenten/`

- REST API integration for invoice management
- Creates fakturaserie with periods and amounts
- Handles kansellering of existing series
- Returns fakturaserieReferanse stored on Behandlingsresultat

### OEBS
- Faktureringskomponenten handles integration with OEBS
- OEBS manages actual billing and payment collection
- Manglende innbetaling events sent via Kafka

### Skatteetaten (Sigrun)
- Annual income data retrieved for årsavregning
- Skattehendelser processed via Kafka consumer
- Used for 25% rule calculations

## Debugging Queries

### Find Trygdeavgiftsperioder for Behandling
```sql
SELECT t.*, mp.id as medlemskap_id, lp.id as lovvalg_id
FROM trygdeavgiftsperiode t
LEFT JOIN medlemskapsperiode mp ON t.medlemskapsperiode_id = mp.id
LEFT JOIN lovvalg_periode lp ON t.lovvalg_periode_id = lp.id
WHERE mp.behandlingsresultat_id = :behandlingsresultatId
   OR lp.beh_resultat_id = :behandlingsresultatId;
```

### Check Fakturaserie Status
```sql
SELECT br.behandling_id, br.fakturaserie_referanse, b.status, f.saksnummer
FROM behandlingsresultat br
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE br.fakturaserie_referanse IS NOT NULL
AND f.saksnummer = :saksnummer;
```

### Find Årsavregning for Year
```sql
SELECT aa.*, br.behandling_id as behandlingsresultat_id, b.id as behandling_id
FROM aarsavregning aa
JOIN behandlingsresultat br ON aa.behandlingsresultat_id = br.behandling_id
JOIN behandling b ON br.behandling_id = b.id
WHERE aa.aar = :year
AND b.saksnummer = :saksnummer;
```

### Check Manglende Innbetaling
```sql
SELECT pi.*, b.id as behandling_id, f.saksnummer
FROM prosessinstans pi
JOIN behandling b ON pi.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE pi.prosess_type = 'OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING'
ORDER BY pi.registrert_dato DESC;
```

## Common Issues

| Issue | Symptoms | Investigation |
|-------|----------|---------------|
| Avgift not calculated | Missing trygdeavgiftsperioder | Check TrygdeavgiftsberegningService logs |
| Wrong avgift amount | Unexpected invoice amount | Verify satser, inntekt, dekning |
| Fakturaserie not created | No fakturaserieReferanse | Check OPPRETT_FAKTURASERIE step |
| Årsavregning missing | No reconciliation for year | Check skattehendelser received |
| Double invoicing | Multiple fakturaseriereferanser | Check prosessinstans for duplicate vedtak |
| 25% rule not applied | Avgift too high | Cap is enforced in the external melosys-trygdeavgift-beregning service; verify request/response there |

## Calculation Rules

### Avgiftssatser (2024)
Rates are defined per year and depend on:
- Avgiftsdekning (helsedel/pensjonsdel, med/uten sykepenger)
- Member type (frivillig/pliktig, yrkesaktiv/pensjonist)
- Whether the person is skattepliktig til Norge (Skatteplikttype)

### 25% Rule (hjemmel: folketrygdloven § 23-3 fjerde ledd)
- Avgift cannot exceed 25% of the part of income that **exceeds the minstebeløp** (not 25% of total income)
- Applies to pliktig medlemskap, frivillig medlemskap and EØS-pensjonister (applied differently per group)
- The cap (`MAKS_PROSENTDEL_AV_INNTEKT = 0.25`) is enforced in the external `melosys-trygdeavgift-beregning` service, not in melosys-api. `TotalbeløpBeregner` in this repo only aggregates avgift and inntekt across periods for årsavregning.

### Minstebeløp (hjemmel: folketrygdloven § 23-3 fjerde ledd)
- Minimum amount threshold; the 25% cap applies to income above this amount
- Defined per year in the external beregning service

## Detailed Documentation

- **[Beregning](references/beregning.md)**: Calculation rules and formulas
- **[Fakturering](references/fakturering.md)**: OEBS integration and invoice flow
- **[Årsavregning](references/aarsavregning.md)**: Annual reconciliation process
- **[Debugging Guide](references/debugging.md)**: SQL queries and troubleshooting
