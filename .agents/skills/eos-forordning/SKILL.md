---
name: eos-forordning
description: |
  Expert knowledge of EU/EEA social security coordination regulations in melosys-api.
  Use when: (1) Understanding EU Regulation 883/2004 and 987/2009 framework,
  (2) Understanding personal and material scope of coordination,
  (3) Understanding coordination principles (single legislation, lex loci laboris),
  (4) Understanding territorial scope (EU, EEA, Switzerland, UK),
  (5) Understanding the relationship between Sakstyper.EU_EOS and regulations,
  (6) Understanding how EESSI integrates with regulation procedures.
---

# EOS-Forordning Skill

Expert knowledge of EU/EEA social security coordination regulations (EC) No 883/2004 and 987/2009.

## Quick Reference

### Regulatory Framework

| Regulation | Full Name | Purpose |
|------------|-----------|---------|
| **(EC) No 883/2004** | Basic Regulation | Coordination rules for social security |
| **(EC) No 987/2009** | Implementing Regulation | Procedures and implementation details |
| **EEA Agreement** | Annex VI | Extends EU regulations to EEA countries |
| **Separasjonsavtalen** | Withdrawal/Separation Agreement (art. 29/30) | Keeps 883/2004 & 987/2009 in force for pre-2021 NO-UK cross-border situations |
| **EØS/EFTA-UK-konvensjonen** | EEA EFTA Convention (kodeverk `KONV_EFTA_STORBRITANNIA_*`, art. 13-18) | Post-Brexit basis for new NO-UK cases (in force from 01.01.2024). Note: Norway is NOT party to the EU-UK TCA. |

### Core Principles

| Principle | Description | Implementation |
|-----------|-------------|----------------|
| **Single Legislation** | Person subject to ONE country's legislation only (Art. 11.1) | Lovvalgsperiode has single lovvalgsland |
| **Lex Loci Laboris** | Work location determines legislation (Art. 11.3.a) | Default for employed persons |
| **Equal Treatment** | No discrimination based on nationality (Art. 4) | Same rights as national citizens |
| **Aggregation** | Periods from all countries count (Art. 6) | For qualifying for benefits |
| **Export of Benefits** | Benefits paid regardless of residence (Art. 7) | No residence requirements |

### Personal Scope (Art. 2)

The regulations apply to:

| Category | Description |
|----------|-------------|
| **Nationals** | EU/EEA/Swiss nationals subject to member state legislation |
| **Stateless/Refugees** | Residing in EU/EEA, subject to member state legislation |
| **Family Members** | Covered for derived rights (healthcare, survivors) |
| **Third-Country Nationals** | At EU level (Reg. 1231/2010) when legally resident and moving within EU. NOT adopted by Norway — for Norwegian cases a plain TCN is generally outside the personkrets of 883/2004, and only covered via the Nordic Convention or the NL/LUX/AT bilateral agreements. |

### Material Scope (Art. 3)

Benefits covered:

| Branch | Norwegian Coverage |
|--------|-------------------|
| Sickness | Folketrygden + Helfo |
| Maternity/Paternity | Foreldrepenger |
| Old-age | Alderspensjon |
| Invalidity | Uføretrygd |
| Survivors | Etterlattepensjon |
| Accidents at Work | Yrkesskade |
| Unemployment | Dagpenger |
| Family | Barnetrygd, kontantstøtte |
| Pre-retirement | N/A in Norway |

### Territorial Scope

| Territory | Regulation Basis | A1 Validity |
|-----------|-----------------|-------------|
| **EU-27** | Direct application of 883/2004 | Full |
| **EEA (NO, IS, LI)** | EEA Agreement Annex VI | Full |
| **Switzerland** | EU-Switzerland Agreement (applies from 01.01.2016) | Full |
| **UK** | Separasjonsavtalen (pre-2021 cases) + EØS/EFTA-UK-konvensjonen (new cases) | Convention-based (NOT the EU-UK TCA — Norway is not a TCA party) |

## Sakstype Handling

### EU_EOS vs Other Types

| Sakstype | Regulation | Service | Key Difference |
|----------|------------|---------|----------------|
| `EU_EOS` | 883/2004 | EosVedtakService | EESSI integration, SEDs |
| `TRYGDEAVTALE` | Bilateral agreements | TrygdeavtaleVedtakService | Country-specific articles |
| `FTRL` | Folketrygdloven | FtrlVedtakService | National law, no EESSI |

### EU_EOS Processing

```kotlin
// In FattVedtakVelger.getFattVedtakService(behandling)
// ÅRSAVREGNING short-circuits before the sakstype branch
if (behandling.type == Behandlingstyper.ÅRSAVREGNING) {
    return årsavregningVedtakService
}

return when (behandling.fagsak.type) {
    Sakstyper.EU_EOS -> eosVedtakService
    Sakstyper.FTRL -> ftrlVedtakService
    Sakstyper.TRYGDEAVTALE -> trygdeavtaleVedtakService
}
```

EU_EOS cases:
- Use 883/2004 articles (FO_883_2004_ART*)
- Integrate with EESSI via EUX
- Generate EU A1 certificates
- Exchange SEDs with other institutions

## Title II - Applicable Legislation

The key articles for determining legislation:

| Article | Scenario | Result |
|---------|----------|--------|
| **Art. 11** | General rule - work in one country | Country of work |
| **Art. 12** | Posted workers | Sending country (max 24 months) |
| **Art. 13** | Multi-state work | Residence if substantial, else employer |
| **Art. 14** | Voluntary insurance | Optional additions |
| **Art. 15** | Contract agents | Choice of legislation |
| **Art. 16** | Exceptions | Agreed exception |

For detailed article descriptions, see the **lovvalg** skill.

## Implementing Regulation 987/2009

Key procedural articles:

| Article | Topic | Purpose |
|---------|-------|---------|
| Art. 14 | Supplementary provisions for art. 12 AND art. 13 | Clarifying criteria (e.g. the 25% substantial-activity test is art. 14(8)) |
| Art. 15 | Procedure for applying art. 12 | Posting procedure |
| Art. 16 | Multi-state procedure | Art. 13 determination process |
| Art. 18 | Exception procedure | Art. 16 application process |

### Art. 14.11 - Business Outside EEA

When employer has no registered office in EEA:
```
Kodeverk: FO_987_2009_ART14_11
Result: Employee's residence country determines legislation
```

## EESSI Integration

### Portable Documents and AC Decisions

Do not confuse the Portable Document with Administrative Commission (AC) Decisions:

| Item | Type | Relevance |
|------|------|-----------|
| **PD A1** | Portable Document | The attestation of applicable legislation issued for posted/multi-state workers (the result of LA_BUC processing) |
| **Decision A2** | AC Decision | Interpretation of the posting rules (art. 12) |
| **Decision A3** | AC Decision | Aggregation of uninterrupted posting periods |

The PD A1 is the document NAV produces; AC Decisions are interpretive guidance from the Administrative Commission, not documents exchanged per case.

### BUC Types by Article

| Article | BUC | Purpose |
|---------|-----|---------|
| Art. 11 | LA_BUC_05 | Legislation notification |
| Art. 12 | LA_BUC_04 | Posted worker notification |
| Art. 13 | LA_BUC_02 | Multi-state decision |
| Art. 16 | LA_BUC_01 | Exception request |

## Key Components in Melosys

| Component | Location | Purpose |
|-----------|----------|---------|
| `EosVedtakService` | service/src/main/java/no/nav/melosys/service/vedtak/ | Vedtak for EU_EOS cases |
| `FattVedtakVelger` | service/src/main/kotlin/no/nav/melosys/service/vedtak/ | Routes behandling to the right vedtak service by sakstype |
| `Lovvalgbestemmelser_883_2004` | external dependency (package `no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser`, not in-repo) | 883/2004 article enum |
| `LovvalgsbestemmelseKodeMapper` | service/src/main/kotlin/no/nav/melosys/service/brev/felles/ | Maps lovvalgsbestemmelse enums to brev/A1 codes |
| `GyldigeKombinasjoner` | service/src/main/kotlin/no/nav/melosys/service/lovligekombinasjoner/ | Valid EU_EOS combinations |

## Valid Combinations for EU_EOS

### MEDLEMSKAP_LOVVALG

| Behandlingstema | Behandlingstyper |
|-----------------|------------------|
| YRKESAKTIV | FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE |
| IKKE_YRKESAKTIV | FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE |
| PENSJONIST | FØRSTEGANG, NY_VURDERING, KLAGE |

### UNNTAK

| Behandlingstema | Behandlingstyper |
|-----------------|------------------|
| REGISTRERING_UNNTAK | FØRSTEGANG, NY_VURDERING |
| ANMODNING_OM_UNNTAK_HOVEDREGEL | FØRSTEGANG, NY_VURDERING |
| ANMODNING_OM_UNNTAK_NY_ARBEIDSGIVER | FØRSTEGANG, NY_VURDERING |
| ANMODNING_OM_UNNTAK_FORLENGELSE | FØRSTEGANG, NY_VURDERING |

## Debugging Queries

> Schema note: `fagsak` PK is `saksnummer` (string); the sakstype column is `fagsak_type`. `behandling` joins `fagsak` via the `saksnummer` FK column (there is no `fagsak_id`), and orders by `registrert_dato`. BUC/SED documents are NOT stored in the melosys-api database — they live in melosys-eessi.

### Find EU_EOS Cases

```sql
SELECT f.saksnummer, f.gsak_saksnummer, b.id as behandling_id, b.status
FROM fagsak f
JOIN behandling b ON b.saksnummer = f.saksnummer
WHERE f.fagsak_type = 'EU_EOS'
ORDER BY b.registrert_dato DESC
FETCH FIRST 50 ROWS ONLY;
```

### Check Lovvalgsbestemmelse Used

```sql
SELECT lp.id, lp.lovvalg_bestemmelse, lp.fom_dato, lp.tom_dato, f.gsak_saksnummer
FROM lovvalg_periode lp
JOIN behandlingsresultat br ON lp.beh_resultat_id = br.behandling_id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE f.fagsak_type = 'EU_EOS'
AND lp.lovvalg_bestemmelse LIKE 'FO_883%'
ORDER BY b.registrert_dato DESC;
```

## UK Post-Brexit

After Brexit, UK cases use:

| Period | Legal basis | Kodeverk |
|--------|-------------|----------|
| Pre-2021 cross-border situations | Separasjonsavtalen (art. 29/30) keeps 883/2004 & 987/2009 in force | FO_883_2004_* |
| New cases (EØS/EFTA-UK-konvensjonen, in force from 01.01.2024) | EEA EFTA Convention (art. 13-18) — NOT the EU-UK TCA, which Norway is not party to | Lovvalgbestemmelser_konv_efta_storbritannia |

```kotlin
// UK mapping in statistics
Lovvalgsbestemmelse.avKonvensjonEftaStorbritannia(lovvalgBestemmelse)
```

## Related Skills

- **lovvalg**: Detailed article descriptions and lovvalgsperiode management
- **eessi-eux**: EESSI integration and SED exchange
- **sed**: SED document types and BUC processes
- **trygdeavtaler**: Bilateral agreements (non-EU/EEA)
- **kodeverk**: LovvalgsBestemmelser enums

## Reference Documentation

- **[Debugging Guide](references/debugging.md)**: SQL queries and common issues
