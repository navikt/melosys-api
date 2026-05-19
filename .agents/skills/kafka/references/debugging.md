# Kafka Debugging

## Common Issues

### 1. Container Stopped

**Symptom**: Messages not being processed, no errors in logs

**Cause**: SkippableKafkaErrorHandler stops container on error

**Debug**:
```http
GET /admin/kafka/containers
```

**Solution**:
1. Check failed messages: `GET /admin/kafka/failed`
2. Fix or skip: `POST /admin/kafka/skip/{key}`
3. Restart: `POST /admin/kafka/container/{topic}/start`

### 2. Deserialization Error

**Symptom**: `FailedDeserializationException` in logs

**Cause**: Message doesn't match expected schema

**Debug**:
```http
GET /admin/kafka/failed
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
-- Check for duplicate handling
SELECT * FROM prosessinstans
WHERE kafka_key = :key
ORDER BY opprettet_tid;
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

### List Containers

```http
GET /admin/kafka/containers
Authorization: Bearer {token}

Response:
{
  "teammelosys.eessi.v1": {
    "running": true,
    "paused": false
  }
}
```

### Get Failed Messages

```http
GET /admin/kafka/failed
Authorization: Bearer {token}

Response:
{
  "teammelosys.eessi.v1-0-12345": {
    "topic": "teammelosys.eessi.v1",
    "offset": 12345,
    "partition": 0,
    "rawMessage": "...",
    "errorStack": "..."
  }
}
```

### Skip Failed Message

```http
POST /admin/kafka/skip/teammelosys.eessi.v1-0-12345
Authorization: Bearer {token}

Response: 200 OK
```

### Start Container

```http
POST /admin/kafka/container/teammelosys.eessi.v1/start
Authorization: Bearer {token}

Response: 200 OK
```

### Stop Container

```http
POST /admin/kafka/container/teammelosys.eessi.v1/stop
Authorization: Bearer {token}

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
