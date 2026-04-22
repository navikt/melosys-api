# Altinn Søknad Debugging

## Common Issues

### 1. Søknad Not Found

**Symptom**: `Kunne ikke hente body GET /soknader/{id}`

**Possible Causes**:
- søknad-mottak service unavailable
- Invalid søknad reference ID
- Application not yet synced from Altinn

**Debug Steps**:
```sql
-- Check if prosessinstans exists
SELECT pi.*, pi.data
FROM prosessinstans pi
WHERE pi.prosesstype = 'MOTTA_SOKNAD_ALTINN'
ORDER BY opprettet_tid DESC
LIMIT 10;

-- Check soknad-mottak logs
-- In Kibana: app:melosys-skjema-api AND søknadID
```

### 2. Missing Fødselsnummer

**Symptom**: `FunksjonellException: Søknader fra Altinn må inneholde fnr`

**Cause**: Arbeidstaker.foedselsnummer is blank in submitted form

**Solution**: Form validation should prevent this; check Altinn form validation rules

### 3. Mapping Errors

**Symptom**: NullPointerException in SoeknadMapper

**Debug**:
```java
// Check which field is null
// Common: virksomhetIUtlandet, loennOgGodtgjoerelse, arbeidssted

// Test with sample XML
AltinnSoeknadService service = ...;
MedlemskapArbeidEOSM søknad = service.soknadMottakConsumer.hentSøknad(id);
// Step through SoeknadMapper.lagSoeknad(søknad)
```

### 4. Behandlingstema Wrong

**Symptom**: Case created with wrong behandlingstema

**Check**:
```sql
SELECT b.tema, mo.mottatte_opplysninger_data
FROM behandling b
JOIN mottatte_opplysninger mo ON mo.behandling_id = b.id
WHERE b.id = :behandlingId;
-- Look at arbeidsgiver.offentligVirksomhet in JSON
```

## SQL Queries

### Find Applications by Period

```sql
SELECT f.gsak_saksnr, b.id as behandling_id,
       mo.mottatte_opplysninger_data
FROM mottatte_opplysninger mo
JOIN behandling b ON mo.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE mo.opplysninger_type = 'SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS'
AND mo.opprettet_tid > SYSDATE - 7
ORDER BY mo.opprettet_tid DESC;
```

### Find by Employer Org Number

```sql
SELECT f.gsak_saksnr, b.id as behandling_id
FROM mottatte_opplysninger mo
JOIN behandling b ON mo.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE mo.opplysninger_type = 'SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS'
AND mo.mottatte_opplysninger_data LIKE '%"virksomhetsnummer":"123456789"%';
```

### Check Fullmektig Registration

```sql
SELECT f.gsak_saksnr, fu.*, fu.fullmaktstype
FROM fullmektig fu
JOIN fagsak f ON fu.fagsak_id = f.id
WHERE f.gsak_saksnr = '123456789';
```

### Trace Saga Steps

```sql
SELECT ps.steg, ps.status, ps.opprettet_tid, ps.feilmelding
FROM prosess_steg ps
JOIN prosessinstans pi ON ps.prosessinstans_id = pi.id
WHERE pi.behandling_id = :behandlingId
AND pi.prosesstype = 'MOTTA_SOKNAD_ALTINN'
ORDER BY ps.opprettet_tid;
```

## Kafka Topics

### Incoming Application Events

Topic: `melosys.soknad-mottatt`

```json
{
  "soknadId": "uuid-from-altinn",
  "skjemaType": "NAV_MedlemskapArbeidEOS_M",
  "mottattTidspunkt": "2024-01-15T10:30:00Z"
}
```

### Processing

Consumed by `SoknadMottattConsumer` which triggers the `MOTTA_SOKNAD_ALTINN` saga.

## Integration Points

### soknad-mottak API

```
GET /soknader/{søknadID}
Accept: application/xml
Returns: MedlemskapArbeidEOSM XML

GET /soknader/{søknadID}/dokumenter
Accept: application/json
Returns: Collection<AltinnDokument>
```

### WebClient Configuration

```kotlin
// Configured in SoknadMottakConfig
@Bean
fun soknadMottakWebClient(
    @Value("\${melosys.integrasjon.soknad-mottak.url}") url: String
): WebClient
```

## Test Resources

Sample test XML: `service/src/test/resources/altinn/NAV_MedlemskapArbeidEOS.xml`

```xml
<melding dataFormatId="6320" dataFormatVersion="46081">
  <Innhold>
    <arbeidsgiver>
      <virksomhetsnummer>123456789</virksomhetsnummer>
      ...
    </arbeidsgiver>
    <arbeidstaker>
      <foedselsnummer>12345678901</foedselsnummer>
      ...
    </arbeidstaker>
    ...
  </Innhold>
</melding>
```
