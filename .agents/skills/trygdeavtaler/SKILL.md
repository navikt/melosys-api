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
| `TrygdeavtaleVedtakService` | `service/src/main/java/no/nav/melosys/service/vedtak/` (Java) | Handles vedtak for trygdeavtale cases |
| `LovvalgsbestemmelseMapperAvtaleland` | `service/src/main/kotlin/.../lovvalgsbestemmelse/` | Maps countries to their agreement articles |
| `LovvalgsbestemmelseService` | `service/src/main/kotlin/.../lovvalgsbestemmelse/` | Routes to correct mapper based on sakstype |
| `GyldigeKombinasjoner` | `service/src/main/kotlin/.../lovligekombinasjoner/` | Defines valid combinations for TRYGDEAVTALE |

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

> The "Agreement Type" and "Key Articles" columns are editorial summaries for quick orientation, not exact reproductions of the code. The authoritative mapping of country to lovvalgsbestemmelser lives in `LovvalgsbestemmelseMapperAvtaleland.mapToLovvalgsbestemmelse(...)`, which cites the Confluence kodeverk page (`confluence.adeo.no/display/TEESSI/Spesifikke+kodeverk+Trygdeavtaler`). Note some "Full agreement" countries collapse YRKESAKTIV and IKKE_YRKESAKTIV into one branch (e.g. India). Verify exact articles in the mapper before relying on them.

### Mapping Types

`LovvalgsbestemmelseMappingType.utledType(sakstema, behandlingstema)` derives the mapping type that selects the article set:

| Mapping Type | Description | Derived from (sakstema + behandlingstema) |
|--------------|-------------|-------------------------------------------|
| `YRKESAKTIV` | Employed persons | MEDLEMSKAP_LOVVALG + YRKESAKTIV |
| `IKKE_YRKESAKTIV` | Non-employed | MEDLEMSKAP_LOVVALG + IKKE_YRKESAKTIV |
| `UNNTAK` | Exception requests | UNNTAK + (ANMODNING_OM_UNNTAK_HOVEDREGEL or REGISTRERING_UNNTAK) |

Any other combination (including PENSJONIST) throws `FunksjonellException` from `utledType` — there is no lovvalgsbestemmelse mapping for it.

### Sakstype TRYGDEAVTALE

Cases with `Sakstyper.TRYGDEAVTALE` differ from EU_EOS:

```kotlin
// Routing in FattVedtakVelger.getFattVedtakService(behandling)
// ÅRSAVREGNING short-circuits before the sakstype switch
if (behandling.type == Behandlingstyper.ÅRSAVREGNING) {
    return årsavregningVedtakService
}

when (behandling.fagsak.type) {
    Sakstyper.EU_EOS -> eosVedtakService
    Sakstyper.FTRL -> ftrlVedtakService
    Sakstyper.TRYGDEAVTALE -> trygdeavtaleVedtakService
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

Per `GyldigeKombinasjoner` / `LovligeBehandlingsKombinasjoner`:

### TRYGDEAVTALE + MEDLEMSKAP_LOVVALG

| Behandlingstema | Behandlingstyper |
|-----------------|------------------|
| YRKESAKTIV | FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE |
| IKKE_YRKESAKTIV | FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE |
| PENSJONIST | FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE |
| FORESPØRSEL_TRYGDEMYNDIGHET | HENVENDELSE |

### TRYGDEAVTALE + UNNTAK

| Behandlingstema | Behandlingstyper |
|-----------------|------------------|
| ANMODNING_OM_UNNTAK_HOVEDREGEL | FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE |
| REGISTRERING_UNNTAK | FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE |
| FORESPØRSEL_TRYGDEMYNDIGHET | HENVENDELSE |

### TRYGDEAVTALE + TRYGDEAVGIFT

| Behandlingstema | Behandlingstyper |
|-----------------|------------------|
| YRKESAKTIV | FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE |
| PENSJONIST | FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE |

Note: ÅRSAVREGNING is **not** a valid behandlingstype for TRYGDEAVTALE. In the matrix, ÅRSAVREGNING only attaches to EU_EOS+TRYGDEAVGIFT+PENSJONIST and FTRL+TRYGDEAVGIFT+YRKESAKTIV.

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
SELECT f.saksnummer, f.gsak_saksnummer, f.fagsak_type, b.id as behandling_id
FROM fagsak f
JOIN behandling b ON b.saksnummer = f.saksnummer
WHERE f.fagsak_type = 'TRYGDEAVTALE'
ORDER BY b.registrert_dato DESC;
```

### Check Lovvalgsperiode for Agreement

```sql
SELECT lp.id, lp.lovvalg_bestemmelse, lp.fom_dato, lp.tom_dato, f.gsak_saksnummer
FROM lovvalg_periode lp
JOIN behandlingsresultat br ON lp.beh_resultat_id = br.behandling_id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE f.fagsak_type = 'TRYGDEAVTALE'
AND lp.lovvalg_bestemmelse LIKE '%_ART%';
```

## References

For symptom-based troubleshooting, the per-mapping-type support matrix (which countries support YRKESAKTIV / IKKE_YRKESAKTIV / UNNTAK), full per-country article tables (GB, US, CA, AU), SQL debug queries, and the `STANDARDVEDLEGG_EGET_VEDLEGG_AVTALELAND` feature toggle, see [references/debugging.md](references/debugging.md).

## Related Skills

- **lovvalg**: General law determination logic
- **vedtak**: Vedtak processing
- **kodeverk**: Lovvalgsbestemmelser enums per country
- **behandling**: Case handling for TRYGDEAVTALE sakstype
