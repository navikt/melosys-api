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
SELECT f.saksnummer, f.gsak_saksnummer, f.fagsak_type,
       mo.data
FROM fagsak f
JOIN behandling b ON b.saksnummer = f.saksnummer
JOIN mottatteopplysninger mo ON mo.behandling_id = b.id
WHERE f.saksnummer = :saksnummer;
-- Check søknadsland in mottatteopplysninger.data
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
SELECT b.id, b.beh_tema as behandlingstema, f.tema as sakstema, f.fagsak_type
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
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
SELECT f.saksnummer, f.gsak_saksnummer, f.fagsak_type, f.tema,
       b.id as behandling_id, b.status, b.beh_tema
FROM fagsak f
JOIN behandling b ON b.saksnummer = f.saksnummer
WHERE f.fagsak_type = 'TRYGDEAVTALE'
ORDER BY b.registrert_dato DESC
FETCH FIRST 50 ROWS ONLY;
```

### Find Cases by Agreement Country

```sql
SELECT f.saksnummer, f.gsak_saksnummer, b.id as behandling_id,
       JSON_VALUE(mo.data, '$.soeknadsland.landkoder[0]') as land
FROM fagsak f
JOIN behandling b ON b.saksnummer = f.saksnummer
JOIN mottatteopplysninger mo ON mo.behandling_id = b.id
WHERE f.fagsak_type = 'TRYGDEAVTALE'
AND JSON_VALUE(mo.data, '$.soeknadsland.landkoder[0]') = :landkode;
```

### Check Lovvalgsperiode with Agreement Article

```sql
SELECT lp.id, lp.lovvalg_bestemmelse, lp.fom_dato, lp.tom_dato,
       br.fastsatt_av_land, br.resultat_type as resultattype
FROM lovvalg_periode lp
JOIN behandlingsresultat br ON lp.beh_resultat_id = br.behandling_id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE f.fagsak_type = 'TRYGDEAVTALE'
AND b.id = :behandlingId;
```

### Find Vedtak for Trygdeavtale

```sql
SELECT b.id, b.status, br.resultat_type as resultattype,
       vm.vedtak_type, vm.vedtak_dato
FROM behandling b
JOIN behandlingsresultat br ON br.behandling_id = b.id
JOIN vedtak_metadata vm ON vm.behandlingsresultat_id = br.behandling_id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE f.fagsak_type = 'TRYGDEAVTALE'
ORDER BY vm.vedtak_dato DESC;
```

### Check Prosessinstans for Trygdeavtale Vedtak

```sql
SELECT pi.uuid, pi.prosess_type, pi.status, pi.sist_fullfort_steg,
       ph.steg, ph.type as hendelse_type, ph.registrert_dato
FROM prosessinstans pi
LEFT JOIN prosessinstans_hendelser ph ON ph.prosessinstans_id = pi.uuid
WHERE pi.behandling_id = :behandlingId
AND pi.prosess_type = 'IVERKSETT_VEDTAK_TRYGDEAVTALE'
ORDER BY ph.registrert_dato;
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
