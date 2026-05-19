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
| **EU-UK TCA** | Trade and Cooperation | Post-Brexit social security protocol |

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
| **Third-Country Nationals** | When legally resident and moving within EU |

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
| **Switzerland** | EU-Switzerland Agreement | Full |
| **UK** | EU-UK Trade and Cooperation Agreement | Protocol-based |

## Sakstype Handling

### EU_EOS vs Other Types

| Sakstype | Regulation | Service | Key Difference |
|----------|------------|---------|----------------|
| `EU_EOS` | 883/2004 | EosVedtakService | EESSI integration, SEDs |
| `TRYGDEAVTALE` | Bilateral agreements | TrygdeavtaleVedtakService | Country-specific articles |
| `FTRL` | Folketrygdloven | FtrlVedtakService | National law, no EESSI |

### EU_EOS Processing

```kotlin
// In FattVedtakVelger
when (sakstype) {
    Sakstyper.EU_EOS -> eosVedtakService
    Sakstypers.TRYGDEAVTALE -> trygdeavtaleVedtakService
    Sakstyper.FTRL -> ftrlVedtakService
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
| Art. 14 | Posted workers details | Criteria for art. 12 posting |
| Art. 15 | Temporary posting | Specific posting rules |
| Art. 16 | Multi-state procedure | Art. 13 determination process |
| Art. 18 | Exception procedure | Art. 16 application process |

### Art. 14.11 - Business Outside EEA

When employer has no registered office in EEA:
```
Kodeverk: FO_987_2009_ART14_11
Result: Employee's residence country determines legislation
```

## EESSI Integration

### Administrative Commission Decisions

| Decision | Topic | Relevance |
|----------|-------|-----------|
| **A1** | Portable Document A1 | Standard attestation format |
| **A2** | Periods aggregation | Counting periods from other countries |
| **H5** | Posted workers | Implementation of Art. 12 |

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
| `EosVedtakService` | service/.../vedtak/ | Vedtak for EU_EOS cases |
| `Lovvalgbestemmelser_883_2004` | domain/.../kodeverk/ | 883/2004 article enum |
| `LovvalgsbestemmelseMapperEuEos` | service/.../lovvalgsbestemmelse/ | Maps to bestemmelser |
| `GyldigeKombinasjoner` | service/.../lovligekombinasjoner/ | Valid EU_EOS combinations |

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

### Find EU_EOS Cases

```sql
SELECT f.id, f.gsak_saksnr, b.id as behandling_id, b.status
FROM fagsak f
JOIN behandling b ON b.fagsak_id = f.id
WHERE f.sakstype = 'EU_EOS'
ORDER BY b.opprettet_tid DESC
FETCH FIRST 50 ROWS ONLY;
```

### Check Lovvalgsbestemmelse Used

```sql
SELECT lp.id, lp.bestemmelse, lp.fom, lp.tom, f.gsak_saksnr
FROM lovvalgsperiode lp
JOIN behandlingsresultat br ON lp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE f.sakstype = 'EU_EOS'
AND lp.bestemmelse LIKE 'FO_883%'
ORDER BY b.opprettet_tid DESC;
```

### Find BUCs for EU_EOS Case

```sql
SELECT bc.id, bc.type, bc.status, f.gsak_saksnr
FROM buc_case bc
JOIN fagsak f ON bc.fagsak_id = f.id
WHERE f.sakstype = 'EU_EOS'
AND f.saksnummer = :saksnummer;
```

## UK Post-Brexit

After Brexit, UK cases use:

| Period | Regulation | Kodeverk |
|--------|------------|----------|
| Until 31.12.2020 | EU 883/2004 (withdrawal) | FO_883_2004_* |
| From 01.01.2021 | EU-UK TCA Protocol | Lovvalgbestemmelser_konv_efta_storbritannia |

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
