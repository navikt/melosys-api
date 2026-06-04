# Kafka Debugging

## Common Issues

### 1. Container Stopped

**Symptom**: Messages not being processed, no errors in logs

**Cause**: SkippableKafkaErrorHandler stops the container on error

**Debug**:
```http
GET /admin/kafka/errors
# Lists failed messages for this pod; if a message is present its container is stopped
```

**Solution** (each of these restarts the stopped container as a side effect):
1. Check failed messages: `GET /admin/kafka/errors`
2. Retry once the cause is fixed: `POST /admin/kafka/errors/{key}/retry`
3. Or skip a poison message: `DELETE /admin/kafka/errors/{key}`
4. Or clear everything at once: `POST /admin/kafka/errors/resume-all`

### 2. Deserialization Error

**Symptom**: `FailedDeserializationException` in logs

**Cause**: Message doesn't match expected schema

**Debug**:
```http
GET /admin/kafka/errors
# Check rawMessage field for actual content
```

**Solution**:
- Fix producer to send correct schema
- Or skip message if corrupt

### 3. Message Processing Timeout

**Symptom**: `TimeoutException` when sending

**Cause**: Kafka broker unavailable or slow

**Debug**:
```kotlin
// Producer waits 15 seconds
val result = future[15L, TimeUnit.SECONDS]
```

**Solution**:
- Check Aiven status
- Check network connectivity
- Check SSL certificates

### 4. Duplicate Processing

**Symptom**: Same message processed multiple times

**Cause**: Error after processing but before commit

**Debug**:
```sql
-- Check for duplicate handling. prosessinstans has no kafka_key column;
-- the SED lock key is stored in sed_laas_referanse, and the timestamp
-- column is registrert_dato (PK is uuid).
SELECT * FROM prosessinstans
WHERE sed_laas_referanse = :key
ORDER BY registrert_dato;
```

**Solution**: Ensure idempotent processing

### 5. SSL Certificate Errors

**Symptom**: `SSLHandshakeException`

**Cause**: Expired or invalid certificates

**Debug**:
```bash
# Check certificate expiry
openssl s_client -connect kafka-broker:9093
```

**Solution**: Renew certificates in NAIS secrets

## Admin API Reference

All endpoints are on `KafkaErrorController` under base path `/admin/kafka/errors` and are
`@Protected`. Keys use the format `topic-partition-offset`. Errors are stored in-memory per pod,
so a `GET` reflects only the pod that served the request. There are no container start/stop/list
endpoints — retry, skip, and resume-all restart stopped containers via `KafkaContainerService`.

### Get Failed Messages

```http
GET /admin/kafka/errors
Authorization: Bearer {token}

Response:
{
  "podName": "melosys-api-xxxx",
  "errors": {
    "teammelosys.eessi.v1-0-12345": {
      "topic": "teammelosys.eessi.v1",
      "offset": 12345,
      "partition": 0,
      "record": { "...": "..." },
      "rawMessage": "...",
      "errorStack": "..."
    }
  }
}
```

### Retry Failed Message

```http
POST /admin/kafka/errors/teammelosys.eessi.v1-0-12345/retry
Authorization: Bearer {token}

# Removes the stored error and restarts the topic's container if stopped, so the
# message is reprocessed from its offset. 404 if the key is unknown.
Response: 200 OK
```

### Skip Failed Message

```http
DELETE /admin/kafka/errors/teammelosys.eessi.v1-0-12345
Authorization: Bearer {token}

# Marks the message to be skipped (committed at offset+1), removes it from the failed
# cache, and restarts the topic's container if stopped. NOTE: DELETE, not POST.
Response: 200 OK
```

### Resume All

```http
POST /admin/kafka/errors/resume-all
Authorization: Bearer {token}

# Groups failed messages per topic, clears them, and restarts each topic's stopped container.
Response: 200 OK
```

## Log Patterns

### Successful Send

```
Melding sendt på topic teammelosys.melosys-hendelser.v1
data: {...}
Offset: 12345
```

### Consumer Received

```
Mottatt ny melding fra eessi(aiven): MelosysEessiMelding(...)
```

### Error Handling

```
Feil ved konsumering av melding på topic teammelosys.xxx
```

### Container Stopped

```
Stopping container for topic teammelosys.xxx
```

## Kafka UI (Local)

Access: http://localhost:8087

Features:
- View topics and partitions
- Browse messages
- Check consumer group lag
- Monitor throughput

## Environment Configuration

### application-nais.yml

```yaml
kafka:
  aiven:
    brokers: ${KAFKA_BROKERS}
    keystorePath: ${KAFKA_KEYSTORE_PATH}
    truststorePath: ${KAFKA_TRUSTSTORE_PATH}
    credstorePassword: ${KAFKA_CREDSTORE_PASSWORD}
    eessi:
      topic: ${KAFKA_AIVEN_EESSIMELDING_TOPIC}
      groupid: melosys-eessi-consumer
```

### Local Configuration

```yaml
kafka:
  aiven:
    brokers: localhost:9092
    keystorePath: ""
    truststorePath: ""
    credstorePassword: ""
```

## Testing Kafka

### Unit Test Pattern

```kotlin
@Test
fun `should send message`() {
    producer.send(message)

    verify(kafkaTemplate).send(any<ProducerRecord<String, MessageType>>())
}
```

### Integration Test Pattern

```kotlin
@Test
fun `should consume message`() {
    // Produce test message
    kafkaTemplate.send(topic, message)

    // Wait for consumer
    val received = testConsumer.waitForMessage(Duration.ofSeconds(10))

    assertThat(received).isNotNull()
}
```

### KafkaOffsetChecker

```kotlin
// Wait for consumer to process all messages
kafkaOffsetChecker.waitForConsumerToCatchUp(
    topic = "teammelosys.xxx",
    groupId = "test-group",
    timeout = Duration.ofSeconds(30)
)
```

## Topic Management

Topics are managed in `melosys-iac` repository:

```yaml
# topics.yaml
topics:
  - name: teammelosys.eessi.v1
    partitions: 1
    replication: 3
    config:
      retention.ms: 604800000  # 7 days
```

## Related Skills

- **eessi-eux**: EESSI message details
- **altinn-soknad**: Application processing
- **inntekt-skatt**: Tax event handling
- **trygdeavgift**: Billing integration
