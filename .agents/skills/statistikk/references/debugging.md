# Statistikk Debugging

## Common Issues

### 1. A1 Message Not Published

**Symptom**: VedtakMetadata saved but no Kafka message published

**Possible Causes**:
- `a1Produseres()` returns false for behandlingsresultat
- No lovvalgsperiode found
- Event listener failed silently

**Debug Steps**:
```sql
-- Check if A1 should be produced
SELECT b.id, br.behandling_id as br_id,
       vm.vedtak_dato, vm.vedtak_type,
       (SELECT COUNT(*) FROM lovvalg_periode lp
        WHERE lp.beh_resultat_id = br.behandling_id) as lovvalg_periode_count
FROM behandling b
JOIN behandlingsresultat br ON br.behandling_id = b.id
LEFT JOIN vedtak_metadata vm ON vm.behandlingsresultat_id = br.behandling_id
WHERE b.id = :behandlingId;

-- Check lovvalg_periode details
SELECT lp.id, lp.fom_dato, lp.tom_dato, lp.lovvalg_bestemmelse,
       lp.beh_resultat_id
FROM lovvalg_periode lp
JOIN behandlingsresultat br ON lp.beh_resultat_id = br.behandling_id
WHERE br.behandling_id = :behandlingId;
```

### 2. Invalid Artikkel Mapping

**Symptom**: `UnsupportedOperationException: Lovvalgsbestemmelse X støttes ikke`

**Cause**: Lovvalgbestemmelse not in LOVVALGSBESTEMMELSE_MAP

**Debug**:
```sql
-- Check which bestemmelse is used
SELECT lp.lovvalg_bestemmelse, b.id as behandling_id
FROM lovvalg_periode lp
JOIN behandlingsresultat br ON lp.beh_resultat_id = br.behandling_id
JOIN behandling b ON br.behandling_id = b.id
WHERE b.id = :behandlingId;
```

**Supported Bestemmelser**:
- FO_883_2004_ART11_3A → 11.3a
- FO_883_2004_ART11_3B → 11.3b
- FO_883_2004_ART11_4_2 → 11.4
- FO_883_2004_ART12_1 → 12.1
- FO_883_2004_ART12_2 → 12.2
- FO_883_2004_ART13_* → 13.1-13.4
- FO_883_2004_ART16_* → 16

### 3. Multiple Recipients Error

**Symptom**: `FunksjonellException: Finner flere enn én mottaker av A1`

**Cause**: More than one land in trygdemyndighetsland for Article 12

**Debug**:
```sql
-- Check trygdemyndighetsland (stored on aktoer.trygdemyndighet_land, keyed by saksnummer)
SELECT a.id, a.trygdemyndighet_land, a.rolle, a.saksnummer
FROM aktoer a
JOIN behandling b ON b.saksnummer = a.saksnummer
WHERE b.id = :behandlingId
AND a.trygdemyndighet_land IS NOT NULL;
```

### 4. No Valid Recipients

**Symptom**: `FunksjonellException: Finner ingen gyldige mottakere av A1`

**Cause**: No trygdemyndighetsland for Article 12 case

**Debug**:
```sql
-- Check if utsendtTilLand should be set
SELECT lp.lovvalg_bestemmelse,
       CASE
         WHEN lp.lovvalg_bestemmelse LIKE '%ART12%' THEN 'Should have land'
         WHEN lp.lovvalg_bestemmelse LIKE '%ART13%' THEN 'No land needed'
         WHEN lp.lovvalg_bestemmelse LIKE '%ART11%' THEN 'No land needed'
         ELSE 'Check logic'
       END as land_required
FROM lovvalg_periode lp
JOIN behandlingsresultat br ON lp.beh_resultat_id = br.behandling_id
WHERE br.behandling_id = :behandlingId;
```

### 5. Kafka Send Timeout

**Symptom**: `IntegrasjonException: Kunne ikke sende melding om utstedt A1`

**Cause**: Kafka producer timeout (15 seconds)

**Debug**:
- Check Kafka cluster health in NAIS console
- Verify topic exists: `teammelosys.melosys-utstedt-a1.v1`
- Check application logs for connection errors

## SQL Queries

### Find Published A1 Statistics by Period

```sql
-- Note: Statistics are not persisted in DB, only sent to Kafka
-- Use this to find treatments that should have published
SELECT b.id, f.gsak_saksnummer, vm.vedtak_dato, vm.vedtak_type,
       lp.lovvalg_bestemmelse, lp.fom_dato, lp.tom_dato
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
JOIN behandlingsresultat br ON br.behandling_id = b.id
JOIN vedtak_metadata vm ON vm.behandlingsresultat_id = br.behandling_id
JOIN lovvalg_periode lp ON lp.beh_resultat_id = br.behandling_id
WHERE vm.vedtak_dato > SYSDATE - 7
ORDER BY vm.vedtak_dato DESC;
```

### Find Treatments Eligible for A1 Publishing

```sql
SELECT b.id, f.gsak_saksnummer, vm.vedtak_dato,
       lp.lovvalg_bestemmelse as artikkel
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
JOIN behandlingsresultat br ON br.behandling_id = b.id
JOIN vedtak_metadata vm ON vm.behandlingsresultat_id = br.behandling_id
JOIN lovvalg_periode lp ON lp.beh_resultat_id = br.behandling_id
WHERE vm.vedtak_dato BETWEEN :fom AND :tom
AND lp.lovvalg_bestemmelse LIKE 'FO_883_2004%'
ORDER BY vm.vedtak_dato;
```

### Find UK-Related A1 Statistics

```sql
SELECT b.id, f.gsak_saksnummer, lp.lovvalg_bestemmelse
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
JOIN behandlingsresultat br ON br.behandling_id = b.id
JOIN lovvalg_periode lp ON lp.beh_resultat_id = br.behandling_id
WHERE lp.lovvalg_bestemmelse LIKE '%STORBRITANNIA%'
   OR lp.lovvalg_bestemmelse LIKE '%EFTA_GB%';
```

## Logging

Key log messages to search for:

```
# Message published
"Melding sendt på topic {} for behandling {}"

# A1 not produced (expected in some cases)
"Melding om utstedt A1 blir ikke sendt for behandling {}"

# Event received
"Mottatt hendelse om vedtak metadata lagret"

# UK cases
"Produserer melding om utstedt A1(GB-bestemmelser) for behandling {}"

# Admin republishing
"Sendt melding om utstedt A1 for {} behandlinger"
"Melding om utstedt A1 feilet for {} behandlinger"
```

## Admin Republishing

### Republish Single Treatment

```bash
curl -X POST "https://melosys-api.intern.nav.no/admin/utstedtA1/{behandlingId}/publiserMelding" \
  -H "Authorization: Bearer $TOKEN"
```

### Republish All Since Date

```bash
curl -X POST "https://melosys-api.intern.nav.no/admin/utstedtA1/publiserMelding/eksisterendeBehandlinger?fom=2024-01-01" \
  -H "Authorization: Bearer $TOKEN"
```

## Kafka Topic Monitoring

### Local Development

```bash
# View messages on local topic
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic teammelosys.melosys-utstedt-a1.v1-local \
  --from-beginning
```

### Kafka UI (Local)

Access: http://localhost:8087
- Navigate to Topics → teammelosys.melosys-utstedt-a1.v1-local
- View messages in real-time

## Schema Validation Errors

If JSON schema validation fails:

```
JsonSchemaValidator: Schema validation failed
```

Check the message against schema at:
`statistikk/src/main/resources/utstedt_a1/a1-utstedt-schema.json`

Common validation issues:
- Missing required field
- Invalid artikkel enum value
- Invalid date format (must be ISO-8601)
- aktorId longer than 13 characters

## Related Skills

- **vedtak**: VedtakMetadata and vedtakstype
- **behandlingsresultat**: Lovvalgsperiode data
- **lovvalg**: Article determination
