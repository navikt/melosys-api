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
├── lovvalgsperioder: List<Lovvalgsperiode>
│   ├── fom/tom: LocalDate
│   ├── lovvalgsland: String (country code)
│   ├── bestemmelse: LovvalgsBestemmelser
│   ├── tilleggsbestemmelse: TilleggsBestemmelser?
│   ├── innvilgelsesresultat: Innvilgelsesresultater
│   ├── medlemskapstype: Medlemskapstyper
│   ├── dekning: Trygdedekning
│   └── trygdeavgiftsperioder: Set<Trygdeavgiftsperiode>
└── vedtakMetadata: VedtakMetadata

Unntaksperiode
├── id: Long
├── fom/tom: LocalDate
├── unntakFraBestemmelse: UnntaksBestemmelser
├── unntakFraLovvalgsland: String
└── innvilgelsesresultat: Innvilgelsesresultater
```

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

### LovvalgsBestemmelser (883/2004)

```
FO_883_2004_ART11_3_A   - Employed in one country
FO_883_2004_ART11_3_B   - Civil servant
FO_883_2004_ART11_3_C   - Unemployed
FO_883_2004_ART11_3_D   - Military service
FO_883_2004_ART11_3_E   - Other non-employed
FO_883_2004_ART11_4     - Maritime worker
FO_883_2004_ART11_5     - Air crew
FO_883_2004_ART12_1     - Posted worker
FO_883_2004_ART12_2     - Posted self-employed
FO_883_2004_ART13_1     - Multi-state employee
FO_883_2004_ART13_2     - Multi-state self-employed
FO_883_2004_ART13_3     - Multi-state both
FO_883_2004_ART13_4     - Multi-state civil servant
FO_883_2004_ART14_11    - Business outside EEA (987/2009)
FO_883_2004_ART15       - EU contract staff
FO_883_2004_ART16_1     - Exception agreement
FO_883_2004_ART16_2     - Pensioner exception
```

### TilleggsBestemmelser

```
FO_883_2004_ART11_2     - Receiving cash benefits
FO_883_2004_ART11_4_1   - Maritime - flag state
FO_883_2004_ART11_5     - Air crew
```

## A1 Attestation

### Generation Flow

```
Vedtaksfatting
    │
    ▼
┌─────────────────┐
│ Generer A1      │
│ (A1Generator)   │
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
| A002 | Response to A001 | Agreement/rejection of exception |
| A004 | Response to A003 | Confirmation/objection to decision |
| A007 | Objection | Dispute of legislation decision |

## Debugging Queries

### Find Lovvalgsperiode by Behandling
```sql
SELECT lp.*, br.id as br_id, b.status
FROM lovvalgsperiode lp
JOIN behandlingsresultat br ON lp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
WHERE b.id = :behandlingId;
```

### Check A1 Generation Status
```sql
SELECT dp.*, b.id as behandling_id, f.saksnummer
FROM dokumentproduksjon dp
JOIN behandling b ON dp.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE dp.type = 'A1'
AND f.saksnummer = :saksnummer
ORDER BY dp.registrert_dato DESC;
```

### Find LA_BUC for Fagsak
```sql
SELECT bc.*, f.saksnummer
FROM buc_case bc
JOIN fagsak f ON bc.fagsak_id = f.id
WHERE f.saksnummer = :saksnummer
AND bc.type LIKE 'LA_BUC%';
```

### Check SED Status
```sql
SELECT sed.*, bc.type as buc_type, f.saksnummer
FROM sed sed
JOIN buc_case bc ON sed.buc_case_id = bc.id
JOIN fagsak f ON bc.fagsak_id = f.id
WHERE f.saksnummer = :saksnummer
ORDER BY sed.registrert_dato DESC;
```

## Common Issues

| Issue | Symptoms | Investigation |
|-------|----------|---------------|
| Wrong article selected | Incorrect lovvalgsbestemmelse | Check vilkårsvurdering logic |
| A1 not generated | No dokumentproduksjon | Check vedtak saga completed |
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
6. 2-month objection period
7. Result: One country's legislation applies

### Article 16 (Exception)
1. Parties request exception from art. 11-15
2. Both countries must agree
3. Must be in worker's interest
4. Send A001 to other country
5. Wait for A002 response
6. Result: Agreed exception period

## Detailed Documentation

- **[Articles](references/articles.md)**: Detailed article explanations
- **[LA BUC](references/la-buc.md)**: BUC types and SED flows
- **[A1 Attest](references/a1-attest.md)**: A1 generation details
- **[Debugging Guide](references/debugging.md)**: SQL queries and troubleshooting
