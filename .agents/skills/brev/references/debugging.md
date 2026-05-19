# Brev Debugging Guide

## Common Issues

### Issue: Template Not Found

**Symptom**:
```
FunksjonellException: "ProduserbartDokument X er ikke støttet"
```

**Cause**: Letter type not registered in `DokumentproduksjonsInfoMapper`

**Investigation**:
```kotlin
val støttede = dokumentproduksjonsInfoMapper.tilgjengeligeMalerIDokgen()
log.info("Supported templates: $støttede")
log.info("Requested: $produserbartDokument, supported: ${støttede.contains(produserbartDokument)}")
```

**Resolution**: Add mapping to `DokumentproduksjonsInfoMapper`

### Issue: PDF Generation Failed

**Symptom**:
```
TekniskException: "Feil ved generering av PDF"
```

**Causes**:
- melosys-dokgen service unavailable
- Invalid data for template
- Template rendering error

**Investigation**:
```kotlin
// Check service health
val health = webClient.get()
    .uri("/actuator/health")
    .retrieve()
    .bodyToMono<String>()
    .block()

// Log the data being sent
log.info("Template: $malnavn, Data: ${objectMapper.writeValueAsString(data)}")
```

**Check melosys-dokgen logs** for template errors.

### Issue: Wrong Letter Content

**Symptom**: Letter has incorrect data or blank fields

**Cause**: Mapping error in `DokgenMalMapper`

**Investigation**:
```kotlin
// Log intermediate data
val dto = dokgenMalMapper.lagDokgenDtoFraBestilling(produserbartDokument, behandling, bestilling)
log.info("Generated DTO: ${objectMapper.writeValueAsString(dto)}")
```

**Common mapping issues**:
- Missing behandlingsresultat data
- Null person data from PDL
- Wrong period dates

### Issue: Letter Sent to Wrong Recipient

**Symptom**: Letter appears in wrong party's archive

**Cause**: Incorrect mottaker configuration

**Investigation**:
```kotlin
// Check recipient logic
val mottakere = dokgenService.hentMottakere(behandling, brevbestillingDto)
mottakere.forEach { m ->
    log.info("Recipient: rolle=${m.rolle}, id=${m.id}, navn=${m.navn}")
}
```

**Check**:
1. `BrevbestillingDto.mottaker` value
2. `BrevmottakerService` logic for the letter type
3. Copy recipients (kopiMottakere)

### Issue: Letter Not Journaled

**Symptom**: Letter produced but not in SAF/Joark

**Cause**: Journaling step failed

**Investigation**:
```sql
-- Check for saksopplysning with document
SELECT so.* FROM saksopplysning so
WHERE so.behandling_id = :behandlingId
AND so.type = 'DOKUMENT';

-- Check prosessinstans if in saga
SELECT pi.* FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND pi.type LIKE '%BREV%';
```

**Check**:
- Joark service availability
- Correct saksnummer
- Valid bruker ID

### Issue: Distribution Failed

**Symptom**: Letter journaled but not sent to recipient

**Cause**: Distribution service error

**Investigation**:
```kotlin
// Check distribution type
log.info("Distribusjonstype: ${brevbestillingDto.distribusjonstype}")
// VEDTAK_FYSISK, VIKTIG, ANNET
```

**Check**:
- Recipient address validity
- Digital mailbox status (KRR)
- Print service availability

### Issue: Legacy vs Dokgen Routing

**Symptom**: Wrong service handling the letter

**Cause**: `erTilgjengeligDokgenmal()` returning unexpected value

**Investigation**:
```kotlin
val erDokgen = dokgenService.erTilgjengeligDokgenmal(produserbartDokument)
log.info("Letter $produserbartDokument uses Dokgen: $erDokgen")
```

**The facade routes**:
- `true` → `DokgenService`
- `false` → `DokumentService` (legacy)

## Log Patterns

### DokgenService Operations
```bash
grep "DokgenService\|dokgenConsumer" application.log
```

### Template Mapping
```bash
grep "DokgenMalMapper\|lagDokgenDtoFraBestilling" application.log
```

### PDF Generation
```bash
grep "lagPdf\|create-pdf" application.log
```

### Recipients
```bash
grep "hentMottakere\|BrevmottakerService" application.log
```

## SQL Queries

### Find Letters for Behandling
```sql
-- Via saksopplysning
SELECT so.id, so.type, so.verdi, so.registrert_dato
FROM saksopplysning so
WHERE so.behandling_id = :behandlingId
AND so.type = 'DOKUMENT'
ORDER BY so.registrert_dato DESC;
```

### Find Saga Steps for Letters
```sql
SELECT pi.id, pi.type, pi.sist_utforte_steg, pi.status
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND (pi.type LIKE '%BREV%' OR pi.sist_utforte_steg LIKE '%BREV%')
ORDER BY pi.registrert_dato DESC;
```

### Find All Letters for Sak
```sql
-- Letters are in Joark, query via SAF
-- Use: joarkService.hentJournalposterTilknyttetSak(saksnummer)
```

## Key Code Locations

| Component | Location |
|-----------|----------|
| Main Service | `service/.../dokument/DokgenService.java` |
| Facade | `service/.../dokument/DokumentServiceFasade.java` |
| Template Mapper | `service/.../dokument/brev/mapper/DokgenMalMapper.kt` |
| Production Info | `service/.../dokument/brev/mapper/DokumentproduksjonsInfoMapper.java` |
| Recipient Service | `service/.../dokument/BrevmottakerService.java` |
| REST Consumer | `integrasjon/.../dokgen/DokgenConsumer.java` |
| Saga Steps | `saksflyt/.../steg/brev/*.kt` |

## Manual Operations

### Produce Test Letter
```kotlin
// Create minimal bestilling
val bestilling = BrevbestillingDto().apply {
    produserbardokument = MELDING_HENLAGT_SAK
    mottaker = BRUKER
    bestillersId = "Z123456"
    begrunnelseKode = "ANNET"
}

val pdf = dokgenService.produserUtkast(behandlingId, bestilling)
// Returns PDF bytes without journaling
```

### Check Template Data
```kotlin
val dto = dokgenMalMapper.lagDokgenDtoFraBestilling(
    INNVILGELSE_FOLKETRYGDLOVEN,
    behandling,
    bestilling
)
println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto))
```

### Force Dokgen vs Legacy
```kotlin
// Check routing
val brukDokgen = dokumentproduksjonsInfoMapper
    .tilgjengeligeMalerIDokgen()
    .contains(produserbartDokument)
```

## Environment Differences

| Aspect | Local | Dev (Q1/Q2) | Prod |
|--------|-------|-------------|------|
| melosys-dokgen | localhost:8084 | dokgen-q1/q2 | dokgen |
| Mock | In melosys-docker-compose | N/A | N/A |
| Distribution | Mocked | Test recipients | Real |

## Mock Setup

In local development:
- melosys-dokgen runs separately or is mocked
- Document distribution is mocked
- Joark/SAF are mocked in melosys-docker-compose

To test PDF generation locally:
1. Start melosys-dokgen: `./gradlew bootRun`
2. Or use mock in docker-compose
