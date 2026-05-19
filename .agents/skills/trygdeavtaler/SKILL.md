---
name: trygdeavtaler
description: |
  Expert knowledge of bilateral social security agreements (trygdeavtaler) in melosys-api.
  Use when: (1) Understanding which countries have agreements with Norway,
  (2) Debugging Sakstyper.TRYGDEAVTALE processing,
  (3) Understanding lovvalgsbestemmelser for specific agreement countries,
  (4) Investigating TrygdeavtaleVedtakService and attestation generation,
  (5) Understanding differences between EU/EEA and bilateral agreement handling.
---

# Trygdeavtaler Skill

Expert knowledge of bilateral social security agreements between Norway and non-EU/EEA countries.

## Quick Reference

### What are Trygdeavtaler?

Trygdeavtaler are bilateral social security agreements that Norway has with countries outside the EU/EEA. These agreements coordinate social security rights and obligations, similar to EU Regulation 883/2004 but with country-specific rules.

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `TrygdeavtaleVedtakService` | `service/.../vedtak/` | Handles vedtak for trygdeavtale cases |
| `LovvalgsbestemmelseMapperAvtaleland` | `service/.../lovvalgsbestemmelse/` | Maps countries to their agreement articles |
| `LovvalgsbestemmelseService` | `service/.../lovvalgsbestemmelse/` | Routes to correct mapper based on sakstype |
| `GyldigeKombinasjoner` | `service/.../lovligekombinasjoner/` | Defines valid combinations for TRYGDEAVTALE |

### Agreement Countries

Norway has bilateral agreements with these countries:

| Country | Code | Agreement Type | Key Articles |
|---------|------|----------------|--------------|
| Australia | AU | Full agreement | ART9, ART11, ART14 |
| Bosnia-Herzegovina | BA | Limited | ART3-7 |
| Canada | CA | Full agreement | ART6, ART7, ART10, ART11 |
| Canada (Quebec) | CA_QC | Separate agreement | ART6-10 |
| Chile | CL | Limited | ART5, ART6, ART8 |
| Croatia | HR | Limited | ART3-7 |
| France | FR | Exception only | (exception agreement) |
| Great Britain | GB | Full agreement (post-Brexit) | ART5-9 |
| Greece | GR | Limited | ART4, ART7 |
| India | IN | Full agreement | ART8, ART9, ART10 |
| Israel | IL | Limited | ART6-9 |
| Italy | IT | Exception only | (exception agreement) |
| Montenegro | ME | Limited | ART3-7 |
| Portugal | PT | Limited | ART8-11 |
| Serbia | RS | Limited | ART3-7 |
| Slovenia | SI | Limited | ART3-7 |
| Switzerland | CH | Full agreement | ART8-11 |
| Turkey | TR | Limited | ART3-6 |
| USA | US | Full agreement | ART5 variants |

### Mapping Types

Each agreement supports different behandlingstema scenarios:

| Mapping Type | Description | Behandlingstema |
|--------------|-------------|-----------------|
| `YRKESAKTIV` | Employed persons | YRKESAKTIV |
| `IKKE_YRKESAKTIV` | Non-employed | IKKE_YRKESAKTIV, PENSJONIST |
| `UNNTAK` | Exception requests | REGISTRERING_UNNTAK, ANMODNING_OM_UNNTAK_* |

### Sakstype TRYGDEAVTALE

Cases with `Sakstyper.TRYGDEAVTALE` differ from EU_EOS:

```kotlin
// Routing in FattVedtakVelger
when (sakstype) {
    Sakstyper.EU_EOS -> eosVedtakService
    Sakstyper.TRYGDEAVTALE -> trygdeavtaleVedtakService
    Sakstyper.FTRL -> ftrlVedtakService
}
```

### Document Types

Agreement-specific letter templates:

| Country | Produserbaredokumenter |
|---------|------------------------|
| Australia | `TRYGDEAVTALE_AU` |
| Canada | `TRYGDEAVTALE_CAN` |
| Great Britain | `TRYGDEAVTALE_GB` |
| USA | `TRYGDEAVTALE_US` |

## Processing Flow

### Vedtak Flow for Trygdeavtale

```
1. FattVedtakVelger routes to TrygdeavtaleVedtakService
   ↓
2. Validate behandlingsresultat and run kontroll
   ↓
3. Check if ikke-yrkesaktiv flow (special handling)
   ↓
4. Set fastsattAvLand = NO (Norway always sets law)
   ↓
5. Generate agreement-specific letter (TRYGDEAVTALE_GB, etc.)
   ↓
6. Start IVERKSETT_VEDTAK_TRYGDEAVTALE prosessinstans
```

### Lovvalgsbestemmelse Selection

```kotlin
// In LovvalgsbestemmelseService
when (sakstype) {
    Sakstyper.TRYGDEAVTALE ->
        LovvalgsbestemmelseMapperAvtaleland.mapToLovvalgsbestemmelse(
            land,
            mappingType  // YRKESAKTIV, IKKE_YRKESAKTIV, or UNNTAK
        )
}
```

## Valid Combinations

### TRYGDEAVTALE + MEDLEMSKAP_LOVVALG

| Behandlingstema | Behandlingstyper |
|-----------------|------------------|
| YRKESAKTIV | FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE |
| IKKE_YRKESAKTIV | FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE |

### TRYGDEAVTALE + UNNTAK

| Behandlingstema | Behandlingstyper |
|-----------------|------------------|
| REGISTRERING_UNNTAK | FØRSTEGANG, NY_VURDERING |
| ANMODNING_OM_UNNTAK_HOVEDREGEL | FØRSTEGANG, NY_VURDERING |

### TRYGDEAVTALE + TRYGDEAVGIFT

| Behandlingstema | Behandlingstyper |
|-----------------|------------------|
| AVGIFT | FØRSTEGANG, NY_VURDERING, ÅRSAVREGNING |

## Key Differences from EU/EEA

| Aspect | EU/EEA | Trygdeavtale |
|--------|--------|--------------|
| Regulation | 883/2004, 987/2009 | Bilateral agreements |
| EESSI/SED | Yes (EUX integration) | No |
| A1 Certificate | Standard EU A1 | Country-specific attestations |
| FastsattAvLand | Can be other country | Always Norway |
| Lovvalgsbestemmelser | 883/2004 articles | Agreement-specific articles |

## Debugging

### Find Trygdeavtale Cases

```sql
SELECT f.id, f.gsak_saksnr, f.sakstype, b.id as behandling_id
FROM fagsak f
JOIN behandling b ON b.fagsak_id = f.id
WHERE f.sakstype = 'TRYGDEAVTALE'
ORDER BY b.opprettet_tid DESC;
```

### Check Lovvalgsperiode for Agreement

```sql
SELECT lp.id, lp.bestemmelse, lp.fom, lp.tom, f.gsak_saksnr
FROM lovvalgsperiode lp
JOIN behandlingsresultat br ON lp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE f.sakstype = 'TRYGDEAVTALE'
AND lp.bestemmelse LIKE '%_ART%';
```

## Related Skills

- **lovvalg**: General law determination logic
- **vedtak**: Vedtak processing
- **kodeverk**: Lovvalgsbestemmelser enums per country
- **behandling**: Case handling for TRYGDEAVTALE sakstype
