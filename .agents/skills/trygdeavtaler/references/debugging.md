# Trygdeavtaler Debugging

## Common Issues

### 1. Unsupported Country

**Symptom**: `FunksjonellException: Støtter ikke mapping til lovvalgsbestemmelse for land X`

**Cause**: Country code not in LovvalgsbestemmelseMapperAvtaleland

**Solution**: Check if the country has a bilateral agreement with Norway. Only these countries are supported: AU, BA, CA, CA_QC, CH, CL, FR, GB, GR, HR, IL, IN, IT, ME, PT, RS, SI, TR, US

### 2. Wrong Sakstype Selected

**Symptom**: EU/EEA country treated as trygdeavtale or vice versa

**Debug**:
```sql
SELECT f.id, f.gsak_saksnr, f.sakstype,
       mo.mottatte_opplysninger_data
FROM fagsak f
JOIN behandling b ON b.fagsak_id = f.id
JOIN mottatte_opplysninger mo ON mo.behandling_id = b.id
WHERE f.id = :fagsakId;
-- Check søknadsland in mottatte_opplysninger_data
```

**Validation**: JournalfoeringValidering checks:
```kotlin
if (erAvtaleland && !erEuEllerEøsLand && sakstype != Sakstyper.TRYGDEAVTALE) {
    throw FunksjonellException("Sak for trygdemyndighet fra $land skal være av type TRYGDEAVTALE")
}
```

### 3. Missing Lovvalgsbestemmelse

**Symptom**: Empty set returned from mapToLovvalgsbestemmelse

**Cause**: Some countries only support certain mapping types

**Debug**:
```sql
-- Check behandlingstema
SELECT b.id, b.tema as behandlingstema, f.sakstema, f.sakstype
FROM behandling b
JOIN fagsak f ON b.fagsak_id = f.id
WHERE b.id = :behandlingId;
```

**Countries with limited support**:

| Country | YRKESAKTIV | IKKE_YRKESAKTIV | UNNTAK |
|---------|------------|-----------------|--------|
| FR | No | No | Yes only |
| IT | No | No | Yes only |
| BA, HR, ME, RS, SI | No | Yes | Yes |
| CL, GR, PT, TR | No | Yes | Yes |

### 4. Document Generation Failed

**Symptom**: `TekniskException: Søknadsland er ikke implementert som produsertbart dokument`

**Cause**: Only AU, CA, GB, US have dedicated letter templates

**Debug**:
```kotlin
// TrygdeavtaleVedtakService only supports:
when (soeknadsland) {
    GB -> TRYGDEAVTALE_GB
    US -> TRYGDEAVTALE_US
    CA -> TRYGDEAVTALE_CAN
    AU -> TRYGDEAVTALE_AU
    else -> throw TekniskException(...)
}
```

## SQL Queries

### Find All Trygdeavtale Cases

```sql
SELECT f.id, f.gsak_saksnr, f.sakstype, f.sakstema,
       b.id as behandling_id, b.status, b.tema
FROM fagsak f
JOIN behandling b ON b.fagsak_id = f.id
WHERE f.sakstype = 'TRYGDEAVTALE'
ORDER BY b.opprettet_tid DESC
FETCH FIRST 50 ROWS ONLY;
```

### Find Cases by Agreement Country

```sql
SELECT f.id, f.gsak_saksnr, b.id as behandling_id,
       JSON_VALUE(mo.mottatte_opplysninger_data, '$.soeknadsland.landkoder[0]') as land
FROM fagsak f
JOIN behandling b ON b.fagsak_id = f.id
JOIN mottatte_opplysninger mo ON mo.behandling_id = b.id
WHERE f.sakstype = 'TRYGDEAVTALE'
AND JSON_VALUE(mo.mottatte_opplysninger_data, '$.soeknadsland.landkoder[0]') = :landkode;
```

### Check Lovvalgsperiode with Agreement Article

```sql
SELECT lp.id, lp.bestemmelse, lp.fom, lp.tom,
       lp.fastsatt_av_land, br.type as resultattype
FROM lovvalgsperiode lp
JOIN behandlingsresultat br ON lp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE f.sakstype = 'TRYGDEAVTALE'
AND b.id = :behandlingId;
```

### Find Vedtak for Trygdeavtale

```sql
SELECT b.id, b.status, br.type as resultattype,
       vm.vedtakstype, vm.vedtaksdato
FROM behandling b
JOIN behandlingsresultat br ON br.behandling_id = b.id
JOIN vedtak_metadata vm ON vm.behandlingsresultat_id = br.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE f.sakstype = 'TRYGDEAVTALE'
ORDER BY vm.vedtaksdato DESC;
```

### Check Prosessinstans for Trygdeavtale Vedtak

```sql
SELECT pi.id, pi.prosesstype, pi.status, ps.steg, ps.status as steg_status
FROM prosessinstans pi
JOIN prosess_steg ps ON ps.prosessinstans_id = pi.id
WHERE pi.behandling_id = :behandlingId
AND pi.prosesstype = 'IVERKSETT_VEDTAK_TRYGDEAVTALE'
ORDER BY ps.opprettet_tid;
```

## Agreement Article Reference

### Great Britain (GB) - Post-Brexit Agreement

| Article | Type | Description |
|---------|------|-------------|
| UK_ART5_4 | IKKE_YRKESAKTIV | Residence-based coverage |
| UK_ART6_1 | YRKESAKTIV | Posted workers |
| UK_ART6_2 | IKKE_YRKESAKTIV | Self-employed |
| UK_ART6_5 | YRKESAKTIV | Maritime workers |
| UK_ART7_1 | YRKESAKTIV | Civil servants |
| UK_ART8_2 | YRKESAKTIV | Multiple employment |
| UK_ART8_5 | IKKE_YRKESAKTIV | Multiple self-employment |
| UK_ART9 | Both | General exceptions |

### USA

| Article | Type | Description |
|---------|------|-------------|
| USA_ART5_2 | Both | General rule |
| USA_ART5_3 | UNNTAK | Exception |
| USA_ART5_4 | YRKESAKTIV | Posted workers |
| USA_ART5_5 | YRKESAKTIV | Self-employed |
| USA_ART5_6 | UNNTAK | Government employees |
| USA_ART5_9 | Both | Continued coverage |

### Canada (CA)

| Article | Type | Description |
|---------|------|-------------|
| CAN_ART6_2 | YRKESAKTIV | Posted workers |
| CAN_ART7 | YRKESAKTIV | Self-employed |
| CAN_ART7_4 | IKKE_YRKESAKTIV | Non-employed |
| CAN_ART9 | UNNTAK | Exceptions |
| CAN_ART10 | YRKESAKTIV | Government |
| CAN_ART10_4 | IKKE_YRKESAKTIV | Government non-employed |
| CAN_ART11 | Both | Voluntary |

### Australia (AU)

| Article | Type | Description |
|---------|------|-------------|
| AUS_ART9_2 | YRKESAKTIV | Posted workers |
| AUS_ART9_3 | YRKESAKTIV | Self-employed |
| AUS_ART11 | Both | Continued coverage |
| AUS_ART14_1 | Both | Voluntary |

## Feature Toggles

| Toggle | Purpose |
|--------|---------|
| `STANDARDVEDLEGG_EGET_VEDLEGG_AVTALELAND` | Use standard attachments for AU, CA, US, GB letters |

## Related Skills

- **lovvalg**: General lovvalg determination
- **vedtak**: Vedtak processing patterns
- **brev**: Letter generation for trygdeavtale documents
