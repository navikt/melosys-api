---
name: statistikk
description: |
  Expert knowledge of A1 statistics publishing to data warehouse in melosys-api.
  Use when: (1) Understanding utstedt A1 Kafka publishing,
  (2) Debugging UtstedtA1Service or Kafka producer issues,
  (3) Understanding DVH/data warehouse integration,
  (4) Investigating A1 statistics message format and schema.
---

# Statistikk Skill

Expert knowledge of statistics publishing for issued A1 certificates to the data warehouse.

## Quick Reference

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `UtstedtA1Service` | `statistikk/src/main/java/.../utstedt_a1/service/` | Creates and sends A1 statistics messages |
| `UtstedtA1EventListener` | `statistikk/src/main/java/.../utstedt_a1/service/` | Listens for vedtak events |
| `UtstedtA1AivenProducer` | `statistikk/src/main/java/.../utstedt_a1/integrasjon/` | Kafka producer for Aiven |
| `UtstedtA1AdminController` | `statistikk/src/main/java/.../utstedt_a1/api/` | Admin endpoint for republishing |
| `UtstedtA1Melding` | `statistikk/src/main/java/.../utstedt_a1/integrasjon/dto/` | Kafka message DTO |

### Kafka Topics

| Topic | Environment | Purpose |
|-------|-------------|---------|
| `teammelosys.melosys-utstedt-a1.v1` | prod-gcp | A1 issued statistics |
| `teammelosys.melosys-utstedt-a1.v1-q1` | dev-gcp (q1) | A1 issued statistics |
| `teammelosys.melosys-utstedt-a1.v1-q2` | dev-gcp (q2) | A1 issued statistics |
| `teammelosys.melosys-utstedt-a1.v1-local` | local | A1 issued statistics |

## Processing Flow

```
1. VedtakMetadata is saved → VedtakMetadataLagretEvent published
   ↓
2. UtstedtA1EventListener receives event (@Async, @TransactionalEventListener)
   ↓
3. UtstedtA1Service.sendMeldingOmUtstedtA1(behandlingID) called
   ↓
4. Check if behandlingsresultat.a1Produseres() is true
   ↓
5. Build UtstedtA1Melding with:
   - Serienummer: saksnummer + behandlingId
   - Artikkel: Mapped from LovvalgBestemmelse
   - Periode: From lovvalgsperiode
   - UtsendtTilLand: From trygdemyndighetsland
   - TypeUtstedelse: FØRSTEGANG, ENDRING, or ANNULLERING
   ↓
6. Validate against JSON schema (a1-utstedt-schema.json)
   ↓
7. UtstedtA1AivenProducer publishes to Kafka topic
```

## UtstedtA1Melding Schema

### Message Structure

```json
{
  "serienummer": "MEL-123456",
  "saksnummer": "MEL-123",
  "behandlingId": 456,
  "aktorId": "1234567891234",
  "artikkel": "12.1",
  "periode": {
    "fom": "2024-01-01",
    "tom": "2024-12-31"
  },
  "utsendtTilLand": "SE",
  "datoUtstedelse": "2024-01-15",
  "typeUtstedelse": "FØRSTEGANG",
  "meldingOpprettetTidspunkt": "2024-01-15T10:30:00.000Z"
}
```

### Field Descriptions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `serienummer` | String | Yes | Unique ID: saksnummer + behandlingId |
| `saksnummer` | String | Yes | Fagsak saksnummer (MEL-*) |
| `behandlingId` | Long | Yes | Treatment ID |
| `aktorId` | String | Yes | Person's aktør-ID (13 chars) |
| `artikkel` | String | Yes | EU regulation article |
| `periode` | Object | Yes | A1 validity period (fom/tom) |
| `utsendtTilLand` | String | Conditional | ISO-2 country code (for Art. 12) |
| `datoUtstedelse` | Date | Yes | Date of issuance/annulment |
| `typeUtstedelse` | String | Yes | FØRSTEGANG, ENDRING, ANNULLERING |
| `meldingOpprettetTidspunkt` | DateTime | Yes | Message creation timestamp |

### Artikkel Values

Mapped from EU Regulation 883/2004:

| Value | Description |
|-------|-------------|
| `11.3a` | Work in one member state |
| `11.3b` | Self-employed in one member state |
| `11.4` | Civil servants |
| `12.1` | Posted workers (employees) |
| `12.2` | Posted workers (self-employed) |
| `13.1` | Work in two or more states (employee) |
| `13.2` | Work in two or more states (self-employed) |
| `13.3` | Work in two or more states (mixed) |
| `13.4` | Civil servants in multiple states |
| `16` | Exceptions/agreements |

### TypeUtstedelse Values

| Value | Description |
|-------|-------------|
| `FØRSTEGANG` | First-time issuance |
| `ENDRING` | Modification of existing A1 |
| `ANNULLERING` | Annulment of existing A1 |

## Special Handling

### UK/Great Britain Cases

For UK-related cases (EFTA Storbritannia convention), messages are sent twice:
1. Once with the GB-specific bestemmelse
2. Once with the standard 883/2004 equivalent

```java
if (erStorbritannia) {
    // Send with GB-specific article
    sendMeldingOmUtstedtA1(behandlingsresultat);
}
// Send with standard article
sendMeldingOmUtstedtA1(behandlingsresultat);
```

### UtsendtTilLand Logic

Land is only included for certain articles:

```java
// Land should NOT be sent for:
// - Article 13 (multiple countries, no single destination)
// - Article 11 (unless also 12 - edge case)
private boolean landSkalIkkeSendes(Lovvalgsperiode lovvalgsperiode) {
    return lovvalgsperiode.erArtikkel13()
        || (lovvalgsperiode.erArtikkel11() && !lovvalgsperiode.erArtikkel12());
}
```

## Admin API

### Republish Single Treatment

```http
POST /admin/utstedtA1/{behandlingID}/publiserMelding
Authorization: Bearer {token}
```

### Republish Multiple Treatments

```http
POST /admin/utstedtA1/publiserMelding/eksisterendeBehandlinger?fom=2024-01-01
Authorization: Bearer {token}

Response:
{
  "sendteBehandlinger": [123, 456],
  "feiledeBehandlinger": [789]
}
```

## Configuration

### Kafka Configuration

```yaml
kafka:
  aiven:
    a1-utstedt:
      topic: ${KAFKA_AIVEN_UTSTEDT_A1_TOPIC:teammelosys.melosys-utstedt-a1.v1}
```

### Producer Configuration

```java
@Bean
@Qualifier("aivenUtstedtA1")
KafkaTemplate<String, UtstedtA1Melding> aivenKafkaTemplate
```

## JSON Schema Validation

Messages are validated against: `utstedt_a1/a1-utstedt-schema.json`

Schema enforces:
- Required fields presence
- Field types and formats
- Valid enum values for artikkel and typeUtstedelse
- Date formats (ISO-8601)
- String length limits

## Integration Points

### Event Source

Triggered by: `VedtakMetadataLagretEvent`
- Published when vedtak metadata is saved
- Event contains behandlingID

### Dependencies

| Service | Purpose |
|---------|---------|
| `BehandlingsresultatService` | Fetch behandlingsresultat |
| `LandvelgerService` | Get trygdemyndighetsland |
| `VedtakMetadataRepository` | Query existing vedtak for republishing |

## Related Skills

- **vedtak**: VedtakMetadata triggers statistics
- **behandlingsresultat**: Source of A1 data
- **lovvalg**: Lovvalgsperiode and articles
- **kodeverk**: Land_iso2, Lovvalgbestemmelser enums
