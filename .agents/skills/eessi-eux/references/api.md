# EESSI API Reference

## EessiConsumer Interface

The main interface for communicating with melosys-eessi (which forwards to EUX/RINA).

### Create BUC and SED

```kotlin
fun opprettBucOgSed(
    sedDataDto: SedDataDto,           // SED content
    vedlegg: Collection<Vedlegg>,     // PDF attachments
    bucType: BucType,                 // LA_BUC_02, etc.
    forsøkSend: Boolean,              // true = send immediately
    oppdaterEksisterendeOmFinnes: Boolean  // true = update if BUC exists
): OpprettSedDto

// Response
data class OpprettSedDto(
    val rinaSaksnummer: String,  // RINA case ID
    val rinaUrl: String          // URL to RINA case
)
```

### Send SED on Existing BUC

```kotlin
fun sendSedPåEksisterendeBuc(
    sedDataDto: SedDataDto,
    rinaSaksnummer: String,     // Existing RINA case
    sedType: SedType            // A012, A004, etc.
)
```

### Get Related BUCs

```kotlin
fun hentTilknyttedeBucer(
    gsakSaksnummer: Long,       // NAV case number
    statuser: List<String>      // Filter by status (empty = all)
): List<BucInformasjon>
```

### Get Recipient Institutions

```kotlin
fun hentMottakerinstitusjoner(
    bucType: String,            // "LA_BUC_02"
    landkoder: Collection<String>  // ["DE", "SE"]
): List<Institusjon>

data class Institusjon(
    val id: String,      // Institution ID (e.g., "DE:12345")
    val navn: String,    // Institution name
    val landkode: String // Country code
)
```

### Get SED from Journalpost

```kotlin
fun hentMelosysEessiMeldingFraJournalpostID(
    journalpostID: String
): MelosysEessiMelding
```

### Save Case Relationship

```kotlin
fun lagreSaksrelasjon(saksrelasjonDto: SaksrelasjonDto)

data class SaksrelasjonDto(
    val gsakSaksnummer: Long,    // NAV case number
    val rinaSaksnummer: String,  // RINA case ID
    val bucType: String
)
```

### Get Case Relationships

```kotlin
fun hentSakForGsakSaksnummer(gsakSaksnummer: Long): List<SaksrelasjonDto>
fun hentSakForRinasaksnummer(rinaSaksnummer: String): List<SaksrelasjonDto>
```

### Generate SED PDF

```kotlin
fun genererSedPdf(
    sedDataDto: SedDataDto,
    sedType: SedType
): ByteArray
```

### Close BUC

```kotlin
fun lukkBuc(rinaSaksnummer: String)
```

### Get Possible Actions

```kotlin
fun hentMuligeAksjoner(rinaSaksnummer: String): List<String>
// Returns strings like "A012 Create" indicating allowed operations
```

### Get SED Content

```kotlin
fun hentSedGrunnlag(
    rinaSaksnummer: String,
    rinaDokumentID: String
): SedGrunnlagDto
```

## EessiService Methods

Higher-level service wrapping EessiConsumer:

### Send SED with Attachments

```kotlin
fun opprettOgSendSed(
    behandlingID: Long,
    mottakerInstitusjoner: List<String>,
    bucType: BucType,
    vedlegg: Collection<Vedlegg>,
    ytterligereInformasjon: String?
)
```

### Check EESSI Readiness

```kotlin
// Check if country is connected to EESSI for specific BUC
fun landErEessiReady(bucType: String, landkoder: Collection<Land_iso2>): Boolean
```

### Validate Recipients

```kotlin
fun validerOgAvklarMottakerInstitusjonerForBuc(
    valgteMottakerinstitusjoner: Set<String>,
    mottakerland: Collection<Land_iso2>,
    bucType: BucType
): Set<String>  // Returns validated institution IDs
```

### Send Specific SED Types

```kotlin
// Art. 16 exception response
fun sendAnmodningUnntakSvar(behandlingId: Long, ytterligereInformasjon: String?)

// Art. 13 confirmation
fun sendGodkjenningArbeidFlereLand(behandlingID: Long, ytterligereInformasjon: String?)

// Objection response
fun sendAvslagUtpekingSvar(behandlingId: Long, utpekingAvvis: UtpekingAvvis)
```

## REST Endpoints (melosys-eessi)

The EessiConsumerImpl calls these endpoints on melosys-eessi:

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/buc/{bucType}` | Create BUC and SED |
| POST | `/buc/{bucID}/sed/{sedType}` | Send SED on existing BUC |
| GET | `/buc/{bucType}/institusjoner` | Get institutions for BUC |
| POST | `/sak` | Save case relationship |
| GET | `/sak?gsakSaksnummer=X` | Get relationships by NAV case |
| GET | `/sak?rinaSaksnummer=X` | Get relationships by RINA case |
| POST | `/sed/{sedType}/pdf` | Generate SED PDF |
| GET | `/buc/{rinaSaksnummer}/aksjoner` | Get possible actions |
| POST | `/buc/{rinaSaksnummer}/lukk` | Close BUC |
| GET | `/buc/{rinaSaksnummer}/sed/{rinaDokumentID}/grunnlag` | Get SED content |

## Kafka Integration

### Incoming Messages

Topic: `${kafka.aiven.eessi.topic}`

```kotlin
@KafkaListener(topics = "\${kafka.aiven.eessi.topic}")
fun mottaMeldingAiven(consumerRecord: ConsumerRecord<String, MelosysEessiMelding>)
```

### MelosysEessiMelding

```kotlin
data class MelosysEessiMelding(
    val sedType: String,           // "A003"
    val bucType: String?,          // "LA_BUC_02"
    val rinaSaksnummer: String,
    val rinaDokumentID: String,
    val journalpostId: String,
    val avsender: Avsender?,
    val lovvalgsland: String?,     // Country that has legislation
    // ... person data, periods, etc.
)
```
