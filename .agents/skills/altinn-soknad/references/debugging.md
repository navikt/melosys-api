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
-- Check if prosessinstans exists (Oracle: use FETCH FIRST, not LIMIT)
SELECT uuid, prosess_type, sist_fullfort_steg, data, registrert_dato, endret_dato
FROM prosessinstans
WHERE prosess_type = 'MOTTAK_SOKNAD_ALTINN'
ORDER BY endret_dato DESC
FETCH FIRST 10 ROWS ONLY;

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
SoknadMottakClient soknadMottakClient = ...;
MedlemskapArbeidEOSM søknad = soknadMottakClient.hentSøknad(søknadReferanse);
// Step through SoeknadMapper.lagSoeknad(søknad)
```

### 4. Behandlingstema Wrong

**Symptom**: Case created with wrong behandlingstema

**Check**:
```sql
SELECT b.beh_tema, mo.data
FROM behandling b
JOIN mottatteopplysninger mo ON mo.behandling_id = b.id
WHERE b.id = :behandlingId;
-- Look at arbeidsgiver.offentligVirksomhet in JSON
```

## SQL Queries

### Find Applications by Period

```sql
SELECT f.gsak_saksnummer, b.id as behandling_id,
       mo.data
FROM mottatteopplysninger mo
JOIN behandling b ON mo.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE mo.type = 'SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS'
AND mo.registrert_dato > SYSDATE - 7
ORDER BY mo.registrert_dato DESC;
```

### Find by Employer Org Number

```sql
SELECT f.gsak_saksnummer, b.id as behandling_id
FROM mottatteopplysninger mo
JOIN behandling b ON mo.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE mo.type = 'SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS'
AND mo.data LIKE '%"virksomhetsnummer":"123456789"%';
```

### Check Fullmektig Registration

There is no `fullmektig` table. A power-of-attorney holder is an `aktoer`
row with `rolle = 'FULLMEKTIG'` (the underlying mandate is in the `fullmakt`
table). Aktører are joined to the case via `saksnummer`:

```sql
SELECT f.gsak_saksnummer, a.rolle, a.orgnr, a.person_ident
FROM aktoer a
JOIN fagsak f ON a.saksnummer = f.saksnummer
WHERE f.gsak_saksnummer = '123456789'
AND a.rolle = 'FULLMEKTIG';
```

### Trace Saga Steps

`prosess_steg` is a kode/navn lookup table - do not join it to find the
progress of a process. `prosessinstans` stores the last completed step
directly in `sist_fullfort_steg`, and per-step failures land in
`prosessinstans_hendelser`:

```sql
-- Current state of the process
SELECT uuid, prosess_type, status, sist_fullfort_steg, endret_dato
FROM prosessinstans
WHERE behandling_id = :behandlingId
AND prosess_type = 'MOTTAK_SOKNAD_ALTINN';

-- Failures/exceptions logged per step
SELECT h.steg, h.type, h.melding, h.registrert_dato
FROM prosessinstans_hendelser h
JOIN prosessinstans pi ON h.prosessinstans_id = pi.uuid
WHERE pi.behandling_id = :behandlingId
ORDER BY h.registrert_dato;
```

## Kafka Topics

### Incoming Application Events

Topic: `teammelosys.soknad-mottak.v1` (config key `kafka.aiven.soknad-mottak.topic`).
Deserialized to `SoknadMottatt`, which carries only the søknad reference and the
event timestamp:

```json
{
  "soknadID": "uuid-from-altinn",
  "occuredOn": "2024-01-15T10:30:00Z"
}
```

`erForGammelTilForvaltningsmelding()` is true once `occuredOn` is >= 7 days old.

### Processing

Consumed by `SoknadMottattConsumer` (`service/.../soknad/`), which calls
`ProsessinstansService.opprettProsessinstansSøknadMottatt(...)` to start the
`MOTTAK_SOKNAD_ALTINN` saga. The XML is then fetched lazily by the saga via
`SoknadMottakClient`.

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
// Configured in SoknadMottakClientConfig (integrasjon/.../soknadmottak/)
@Bean
fun soknadMottakWebClient(
    webClientBuilder: WebClient.Builder,
    correlationIdOutgoingFilter: CorrelationIdOutgoingFilter,
    genericAuthFilterFactory: GenericAuthFilterFactory,
    @Value("\${MelosysSoknadMottak.url}") url: String
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
