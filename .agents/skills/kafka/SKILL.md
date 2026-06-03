---
name: kafka
description: |
  Expert knowledge of Kafka event streaming patterns in melosys-api.
  Use when: (1) Understanding Kafka producers and consumers,
  (2) Debugging Kafka message processing issues,
  (3) Understanding topic configuration and message schemas,
  (4) Investigating SkippableKafkaErrorHandler and error handling,
  (5) Understanding Aiven Kafka setup and security configuration.
---

# Kafka Skill

Expert knowledge of Kafka event streaming patterns in melosys-api.

## Quick Reference

### Kafka Topics

| Topic | Direction | Message Type | Purpose |
|-------|-----------|--------------|---------|
| `teammelosys.eessi.v1` | Consumer | MelosysEessiMelding | EESSI SED messages from EUX |
| `teammelosys.soknad-mottak.v1` | Consumer | SoknadMottatt | A1 applications from Altinn |
| `teammelosys.skattehendelser.v1` | Consumer | Skattehendelse | Tax events for årsavregning |
| `teammelosys.manglende-fakturabetaling.v1` | Consumer | ManglendeFakturabetalingMelding | Missing payments from faktureringskomponenten |
| `teammelosys.melosys-hendelser.v1` | Producer | MelosysHendelse | Events to faktureringskomponenten |
| `teammelosys.popp-hendelser.v1` | Producer | PensjonsopptjeningHendelse | Pension events to POPP |
| `teammelosys.melosys-utstedt-a1.v1` | Producer | UtstedtA1Melding | A1 statistics to DVH |

### Key Components

All Kafka components live under `integrasjon/src/main/kotlin/no/nav/melosys/integrasjon/kafka/`.

| Component | Purpose |
|-----------|---------|
| `KafkaConfig` | Producer/consumer factory configuration |
| `SkippableKafkaErrorHandler` | Error handling with skip capability |
| `KafkaErrorController` | Admin API (`/admin/kafka/errors`) for inspecting, retrying, and skipping failed messages |
| `KafkaContainerService` | Starts stopped listener containers for a topic (internal service, called by `KafkaErrorController`) |

## Consumer Pattern

### Standard Consumer Setup

```kotlin
@KafkaListener(
    clientIdPrefix = "aiven-melosys-xxx-consumer",
    topics = ["\${kafka.aiven.xxx.topic}"],
    containerFactory = "aivenXxxListenerContainerFactory"
)
@Transactional
fun handleMessage(consumerRecord: ConsumerRecord<String, MessageType>) {
    val message = consumerRecord.value()
    // Process message
}
```

### Container Factory Pattern

Each message type has a dedicated container factory:

```kotlin
@Bean
fun aivenXxxListenerContainerFactory(
    objectMapper: ObjectMapper,
    kafkaProperties: KafkaProperties,
    @Value("\${kafka.aiven.xxx.groupid}") groupId: String
): KafkaConsumerContainerFactory<MessageType> =
    kafkaListenerContainerFactoryStopOnError<MessageType>(
        objectMapper, kafkaProperties, groupId
    )
```

### Consumer Configuration

```kotlin
ConsumerConfig.GROUP_ID_CONFIG to groupId
ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false  // Manual ack
ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 1  // One at a time
```

## Producer Pattern

### Standard Producer Setup

```kotlin
@Component
class KafkaXxxProducer(
    @Value("\${kafka.aiven.xxx.topic}") private val topicName: String,
    @Qualifier("xxxHendelse") private val kafkaTemplate: KafkaTemplate<String, MessageType>
) {
    fun send(message: MessageType) {
        val record = ProducerRecord<String, MessageType>(topicName, message)
        record.headers().add(CORRELATION_ID, MDCOperations.getCorrelationId().encodeToByteArray())

        val future = kafkaTemplate.send(record)
        val result = future[15L, TimeUnit.SECONDS]
        log.info("Sent to $topicName, offset: ${result.recordMetadata.offset()}")
    }
}
```

### Producer Factory Pattern

```kotlin
@Bean
fun producerFactoryXxx(objectMapper: ObjectMapper): ProducerFactory<String, MessageType> =
    DefaultKafkaProducerFactory(
        mutableMapOf<String, Any>(
            CommonClientConfigs.CLIENT_ID_CONFIG to "melosys-xxx-producer",
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to brokersUrl,
        ) + securityConfig(),
        StringSerializer(),
        JsonSerializer(objectMapper)
    )
```

## Error Handling

### SkippableKafkaErrorHandler

Custom error handler that:
1. Stops container on error (CommonContainerStoppingErrorHandler)
2. Stores failed messages for inspection
3. Allows manual skip of problematic messages

```kotlin
@Component
class SkippableKafkaErrorHandler : CommonContainerStoppingErrorHandler() {
    val failedMessages = ConcurrentHashMap<String, Failed>()

    // Key format: "topic-partition-offset"
    fun markToSkip(key: String): Boolean
}
```

### Failed Message Structure

```kotlin
data class Failed(
    val topic: String,
    val offset: Long?,
    val partition: Int?,
    val record: Map<String, Any?>?,
    val rawMessage: String?,
    val errorStack: String?
)
```

### Admin API Endpoints

All endpoints live under `KafkaErrorController` at base path `/admin/kafka/errors` (`@Protected`).
Failed-message keys use the format `topic-partition-offset`.

```http
GET /admin/kafka/errors
# Lists failed messages for this pod. Returns {podName, errors}
# where errors is a map of key -> Failed (topic, offset, partition, record, rawMessage, errorStack)

POST /admin/kafka/errors/{key}/retry
# Removes the stored error and, if the topic's container is stopped, restarts it
# so the message is reprocessed from its offset

DELETE /admin/kafka/errors/{key}
# Skip: marks the message to be skipped next time it's seen, removes it from the
# failed cache, and restarts the topic's container if stopped (NOTE: DELETE, not POST)

POST /admin/kafka/errors/resume-all
# Clears all failed messages grouped per topic and restarts each topic's stopped container
```

> Errors are held in-memory per pod, so a `GET` reports only the pod it hit. There are no
> container start/stop/list endpoints — container restarts happen as a side effect of retry,
> skip, and resume-all via `KafkaContainerService`.

## Security Configuration

### Aiven SSL Setup

```kotlin
private fun securityConfig(): Map<String, Any> =
    if (isLocal) mapOf() else mapOf(
        SECURITY_PROTOCOL_CONFIG to "SSL",
        SSL_TRUSTSTORE_LOCATION_CONFIG to truststorePath,
        SSL_TRUSTSTORE_PASSWORD_CONFIG to credstorePassword,
        SSL_TRUSTSTORE_TYPE_CONFIG to "JKS",
        SSL_KEYSTORE_LOCATION_CONFIG to keystorePath,
        SSL_KEYSTORE_PASSWORD_CONFIG to credstorePassword,
        SSL_KEY_PASSWORD_CONFIG to credstorePassword,
        SSL_KEYSTORE_TYPE_CONFIG to "PKCS12"
    )
```

### Local Development

For local profiles (local, local-mock, test), security is disabled and connects to local Kafka.

## Message Types

### MelosysEessiMelding (Consumer)

EESSI messages from EUX containing SED documents.

### SoknadMottatt (Consumer)

A1 applications from Altinn skjema-api.

### Skattehendelse (Consumer)

Tax events from melosys-skattehendelser for årsavregning.

### ManglendeFakturabetalingMelding (Consumer)

Missing payment notifications from faktureringskomponenten.

### MelosysHendelse (Producer)

Events sent to faktureringskomponenten for billing.

### PensjonsopptjeningHendelse (Producer)

Pension registration events sent to POPP.

### UtstedtA1Melding (Producer)

A1 statistics sent to DVH.

## Debugging

When a consumer hits an error, `SkippableKafkaErrorHandler` stops the container, so symptoms are
"messages stop being processed" rather than a crash.

### View Failed Messages

```http
GET /admin/kafka/errors
```

### Retry, Skip, or Resume

```http
# Retry a single message (restarts the stopped container)
POST /admin/kafka/errors/teammelosys.eessi.v1-0-12345/retry

# Skip a poison message, then the container restarts and resumes at offset+1
DELETE /admin/kafka/errors/teammelosys.eessi.v1-0-12345

# Clear every failed message per topic and restart all stopped containers
POST /admin/kafka/errors/resume-all
```

### Kafka UI

Local development: http://localhost:8087

For container-stopped / deserialization / SSL troubleshooting and log patterns, see
[references/debugging.md](references/debugging.md).

## Testing

### Integration Test Pattern

```kotlin
@KafkaListener(
    topics = ["\${kafka.aiven.xxx.topic}"],
    groupId = "\${kafka.aiven.xxx.groupid}",
    containerFactory = "xxxListenerContainerFactory"
)
private fun testListener(record: ConsumerRecord<String, MessageType>) {
    records.add(record)
}

fun waitForMessage(timeout: Duration): MessageType? =
    records.poll(timeout.toMillis(), TimeUnit.MILLISECONDS)?.value()
```

### KafkaOffsetChecker

Utility for waiting until consumer has caught up:

```kotlin
kafkaOffsetChecker.waitForConsumerToCatchUp(topic, groupId, timeout)
```

## Related Skills

- **eessi-eux**: EESSI message processing
- **altinn-soknad**: Application message processing
- **inntekt-skatt**: Tax event processing
- **trygdeavgift**: Billing message handling
- **statistikk**: A1 statistics publishing
