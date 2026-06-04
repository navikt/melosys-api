---
name: lovvalg
description: |
  Expert knowledge of lovvalg (law determination/applicable legislation) domain in melosys-api.
  Use when: (1) Understanding which EU/EEA article applies (11, 12, 13, 16),
  (2) Debugging LA_BUC processing and SED sending/receiving,
  (3) Understanding A1 attestation generation,
  (4) Investigating lovvalgsperiode and unntaksperiode management,
  (5) Understanding automatic vs manual lovvalg determination,
  (6) Debugging lovvalgsbeslutning status transitions,
  (7) Understanding the mapping between regulations and kodeverk.
---

# Lovvalg Domain

Lovvalg (law determination/applicable legislation) handles determining which country's social security
legislation applies to a person working across EU/EEA borders. This is governed by EU regulations
883/2004 and 987/2009, and involves BUC (Business Use Case) processes with other EU/EEA institutions.

## Quick Reference

### Domain Model Hierarchy

```
Behandlingsresultat
├── lovvalgsperioder: List<Lovvalgsperiode>   (@Table "lovvalg_periode")
│   ├── fom/tom: LocalDate
│   ├── lovvalgsland: Land_iso2
│   ├── bestemmelse: LovvalgBestemmelse
│   ├── tilleggsbestemmelse: LovvalgBestemmelse?
│   ├── innvilgelsesresultat: InnvilgelsesResultat
│   ├── medlemskapstype: Medlemskapstyper
│   ├── dekning: Trygdedekninger
│   ├── kilde: PeriodeKilde?
│   └── trygdeavgiftsperioder: Set<Trygdeavgiftsperiode>
└── vedtakMetadata: VedtakMetadata
```

Unntak (registered exception) is **not** a separate entity. It is stored as the
`unntak_fra_lovvalgsland` and `unntak_fra_bestemmelse` columns on the
`lovvalg_periode` table (mapped on the `Lovvalgsperiode` entity). The only
`Unntaksperiode` type in the codebase is a trivial service record
`record Unntaksperiode(LocalDate fom, LocalDate tom)`.

### Key Articles (EU Regulation 883/2004)

| Article | Scope | Description |
|---------|-------|-------------|
| **Art. 11** | General rule | Work in one country - subject to that country's legislation |
| **Art. 11.3.a** | Employed | Working in one country as employee |
| **Art. 11.3.b** | Civil servant | Tjenestemann in public administration |
| **Art. 11.4** | Maritime | Work on ship - flag state or employer state |
| **Art. 12.1** | Posted worker | Employee posted to another country (max 24 months) |
| **Art. 12.2** | Self-employed | Self-employed working temporarily in another country |
| **Art. 13** | Multi-state | Working in two or more countries |
| **Art. 13.1** | Employee multi | Employee working in multiple countries |
| **Art. 13.2** | Self-employed multi | Self-employed in multiple countries |
| **Art. 13.3** | Both | Employee and self-employed in different countries |
| **Art. 16.1** | Exception | Agreed exception from art. 11-15 |
| **Art. 16.2** | Pensioner exception | Exception for pensioners |

### LA_BUC Types

| BUC | Purpose | Key SEDs |
|-----|---------|----------|
| **LA_BUC_01** | Exception request (Art. 16) | A001, A002, A005, A006, A008, A011 |
| **LA_BUC_02** | Multi-state decision (Art. 13) | A003, A004, A005, A006, A007, A008, A012 |
| **LA_BUC_03** | Relevant information | A008 |
| **LA_BUC_04** | Posted worker notification (Art. 12) | A009 |
| **LA_BUC_05** | Legislation notification (Art. 11) | A010 |
| **LA_BUC_06** | Information request | A005, A006 |

### Lovvalgsbeslutning Flow

```
┌─────────────────┐
│ Søknad mottas   │
│ (søknad-altinn) │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Journalføring   │
│ → Behandling    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Vurdere vilkår  │
│ (regelmodul)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐     ┌─────────────────┐
│ Fatt vedtak     │────►│ Send SED        │
│ (fattVedtak)    │     │ (via EUX)       │
└────────┬────────┘     └────────┬────────┘
         │                       │
         ▼                       ▼
┌─────────────────┐     ┌─────────────────┐
│ Lagre MEDL      │     │ Generer A1      │
│ lovvalgsperiode │     │ attest          │
└─────────────────┘     └─────────────────┘
```

## Service Layer

### LovvalgsperiodeService
Location: `service/src/main/kotlin/.../LovvalgsperiodeService.kt`

Key operations:
- `hentLovvalgsperioder()` - Get all periods for behandling
- `lagreLovvalgsperioder()` - Save periods
- `oppdaterLovvalgsperiode()` - Update period
- `hentTidligereLovvalgsperioder()` - Get previous periods from fagsak

### LovvalgsbestemmelseService
Location: `service/src/main/kotlin/.../lovvalgsbestemmelse/LovvalgsbestemmelseService.kt`

Key operations:
- `hentLovvalgsbestemmelser()` - Get valid bestemmelser for context
- `mapLovvalgsbestemmelse()` - Map between regulation types

### OpprettLovvalgsperiodeService
Location: `service/src/main/kotlin/.../lovvalgsperiode/OpprettLovvalgsperiodeService.kt`

Key operations:
- `opprettLovvalgsperiode()` - Create period from request
- Maps OpprettLovvalgsperiodeRequest to Lovvalgsperiode

## Kodeverk

Bestemmelse values implement the `LovvalgBestemmelse` interface
(`domain.kodeverk`). The main concrete enums are `Lovvalgbestemmelser_883_2004`
and `Tilleggsbestemmelser_883_2004`, plus `Lovvalgbestemmelser_987_2009`,
`Overgangsregelbestemmelser` (1408/71) and the EFTA/Storbritannia konvensjon
enums. Note the constant spelling has **no underscore before the article
letter/sub-number**: it is `FO_883_2004_ART11_3A`, not `FO_883_2004_ART11_3_A`.

### Lovvalgbestemmelser_883_2004

```
FO_883_2004_ART11_1     - General rule
FO_883_2004_ART11_3A    - Employed in one country
FO_883_2004_ART11_3B    - Civil servant
FO_883_2004_ART11_3C    - Unemployed
FO_883_2004_ART11_3D    - Military service
FO_883_2004_ART11_3E    - Other non-employed
FO_883_2004_ART11_4     - Maritime worker
FO_883_2004_ART12_1     - Posted worker
FO_883_2004_ART12_2     - Posted self-employed
FO_883_2004_ART13_1A    - Multi-state employee
FO_883_2004_ART13_1B1   - Multi-state employee (sub-case i)
FO_883_2004_ART13_1B2   - Multi-state employee (sub-case ii)
FO_883_2004_ART13_1B3   - Multi-state employee (sub-case iii)
FO_883_2004_ART13_1B4   - Multi-state employee (sub-case iv)
FO_883_2004_ART13_2A    - Multi-state self-employed
FO_883_2004_ART13_2B    - Multi-state self-employed
FO_883_2004_ART13_3     - Multi-state both employed and self-employed
FO_883_2004_ART13_4     - Multi-state civil servant
FO_883_2004_ART15       - EU contract staff
FO_883_2004_ART16_1     - Exception agreement
FO_883_2004_ART16_2     - Pensioner exception
FO_883_2004_ANNET       - Other
```

`Art 14 nr. 11` (business/employer outside EU/EEA, multi-state work) belongs to
the implementing regulation 987/2009, so the constant lives in
`Lovvalgbestemmelser_987_2009` as `FO_987_2009_ART14_11` (not 883/2004).

### Tilleggsbestemmelser_883_2004

```
FO_883_2004_ART11_2     - Receiving cash benefits
FO_883_2004_ART11_4_1   - Maritime - flag state
FO_883_2004_ART11_5     - Air crew
FO_883_2004_ART87A      - Transitional
FO_883_2004_ART87_8     - Transitional
```

## A1 Attestation

### Generation Flow

```
Vedtaksfatting
    │
    ▼
┌─────────────────┐
│ Generer A1      │
│ (A1Mapper /     │
│  BrevDataByggerA1)│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Send til EUX    │
│ (via SED-steg)  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Journalfør      │
│ (Joark)         │
└─────────────────┘
```

### A1 Content

The A1 certificate confirms:
- Person identity
- Applicable legislation (which country)
- Period of validity
- Employer information
- Article/legal basis

## SED Processing

### Outgoing SEDs

| SED | Trigger | Content |
|-----|---------|---------|
| A001 | Exception request | Request for art. 16 exception |
| A003 | Art. 13 decision | Multi-state legislation decision |
| A009 | Art. 12 notification | Posted worker notification |
| A010 | Art. 11 notification | General legislation notification |

### Incoming SEDs

| SED | Source | Action |
|-----|--------|--------|
| A011 | Response to A001 | Approval (full agreement) of the art. 16 exception |
| A002 | Response to A001 | Full or partial rejection of the exception request |
| A004 | Response to A003 | Objection to the art. 13 decision |
| A007 | Objection | Dispute of legislation decision |

## Debugging Queries

Key schema facts (verified against Flyway migrations):
- The table is `lovvalg_periode`; its FK to behandlingsresultat is `beh_resultat_id`.
- `behandlingsresultat` has **no `id` column** — its PK is `behandling_id`
  (shared with `behandling.id`), so `beh_resultat_id` effectively equals `behandling.id`.
- `behandling` references `fagsak` via the **`saksnummer`** column (no `fagsak_id`);
  `fagsak`'s PK is `saksnummer`, not `id`.
- There are no `buc_case`, `sed` or `dokumentproduksjon` tables. BUC/SED state
  lives in RINA (queried via EUX, not the DB). The saksrelasjon linking a
  behandling/arkivsak to a RINA case is owned by melosys-eessi and reached
  through `EessiClient` (see `EessiService.finnSakForRinasaksnummer` /
  `lagreSaksrelasjon`), not stored in the melosys-api Oracle DB.

### Find Lovvalgsperiode by Behandling
```sql
SELECT lp.*, b.status
FROM lovvalg_periode lp
JOIN behandling b ON lp.beh_resultat_id = b.id
WHERE b.id = :behandlingId;
```

### Find Lovvalgsperioder for Fagsak
```sql
SELECT lp.id, lp.fom_dato, lp.tom_dato, lp.lovvalgsland,
       lp.lovvalg_bestemmelse, lp.innvilgelse_resultat, lp.medlemskapstype,
       lp.trygde_dekning, lp.medlperiode_id, b.status
FROM lovvalg_periode lp
JOIN behandling b ON lp.beh_resultat_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE f.saksnummer = :saksnummer
ORDER BY lp.fom_dato DESC;
```

### Inspect a registered unntak (exception)
```sql
-- Unntak is stored as columns on lovvalg_periode, not a separate table
SELECT lp.id, lp.fom_dato, lp.tom_dato,
       lp.unntak_fra_lovvalgsland, lp.unntak_fra_bestemmelse,
       lp.lovvalg_bestemmelse, lp.innvilgelse_resultat
FROM lovvalg_periode lp
WHERE lp.beh_resultat_id = :behandlingId
AND (lp.unntak_fra_bestemmelse IS NOT NULL OR lp.unntak_fra_lovvalgsland IS NOT NULL);
```

### Check the iverksett-vedtak prosessinstans (A1 / SED / MEDL steps)
```sql
-- Confirm the vedtak saga ran and which step it stopped on.
-- A1 sending and MEDL registration happen as saga steps, not as DB rows.
SELECT pi.uuid, pi.prosess_type, pi.sist_fullfort_steg, pi.status,
       pi.endret_dato
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.prosess_type LIKE 'IVERKSETT_VEDTAK%'
ORDER BY pi.endret_dato DESC;
```

## Common Issues

| Issue | Symptoms | Investigation |
|-------|----------|---------------|
| Wrong article selected | Incorrect lovvalgsbestemmelse | Check vilkårsvurdering logic |
| A1 not generated | A1 step not completed | Check vedtak saga (IVERKSETT_VEDTAK prosessinstans) completed |
| SED not sent | BUC not updated | Check EUX integration logs |
| Lovvalgsland wrong | Wrong country in period | Check søknad arbeidsland data |
| Period overlap | Validation error | Check existing lovvalgsperioder |
| Missing MEDL registration | Not in register | Check LAGRE_LOVVALGSPERIODE_MEDL step |

## Article Decision Flow

### Article 12 (Posted Worker)
1. Employee temporarily sent abroad
2. Employer must have substantial activity in Norway
3. Maximum 24 months
4. Employee previously subject to Norwegian legislation
5. Result: Norway remains applicable legislation

### Article 13 (Multi-State)
1. Work in two or more countries
2. Determine substantial part (≥25%) in residence country
3. If yes: residence country legislation
4. If no: employer establishment country
5. Notify other countries via A003
6. Receiving country may object with A004; otherwise the BUC auto-closes
   passively after ~8 weeks and the decision stands
7. Result: One country's legislation applies

### Article 16 (Exception)
1. Parties request exception from art. 11-15
2. Both countries must agree
3. Must be in worker's interest
4. Send A001 to other country
5. Wait for response: A011 = approval, A002 = full/partial rejection
6. Result: Agreed exception period

## Detailed Documentation

- **[Articles](references/articles.md)**: Detailed article explanations
- **[Debugging Guide](references/debugging.md)**: SQL queries and troubleshooting
