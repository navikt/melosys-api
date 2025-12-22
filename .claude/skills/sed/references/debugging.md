# SED/EESSI Debugging Guide

## Common SQL Queries

### Find RINA Case Number for Behandling
```sql
SELECT so.verdi as rina_saksnummer, so.opplysningstype, so.registrert_dato
FROM saksopplysning so
WHERE so.behandling_id = :behandlingId
AND so.opplysningstype = 'RINA_SAKSNUMMER'
ORDER BY so.registrert_dato DESC;
```

### Find All SED-Related Saga Steps
```sql
SELECT pi.id, pi.type, pi.sist_utforte_steg, pi.status, pi.endret_dato
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND (
    pi.type LIKE '%SED%'
    OR pi.sist_utforte_steg LIKE '%SED%'
    OR pi.sist_utforte_steg LIKE '%UTLAND%'
    OR pi.sist_utforte_steg LIKE '%ANMODNING%'
)
ORDER BY pi.registrert_dato DESC;
```

### Find Failed SED Steps
```sql
SELECT pi.id, pi.type, pi.sist_utforte_steg, pi.status,
       b.id as behandling_id, f.saksnummer
FROM prosessinstans pi
JOIN behandling b ON b.id = pi.behandling_id
JOIN fagsak f ON f.saksnummer = b.fagsak_saksnummer
WHERE pi.status = 'FEILET'
AND pi.sist_utforte_steg IN (
    'SEND_VEDTAK_UTLAND',
    'SEND_ANMODNING_OM_UNNTAK',
    'SEND_SVAR_ANMODNING_UNNTAK',
    'SED_MOTTAK_RUTING',
    'OPPRETT_SED_GRUNNLAG'
)
ORDER BY pi.endret_dato DESC;
```

### Find Incoming SEDs (Journalposter)
```sql
SELECT jp.id, jp.mottakskanal, jp.registrert_dato, jp.tittel
FROM journalpost jp
WHERE jp.behandling_id = :behandlingId
AND jp.mottakskanal = 'EESSI'
ORDER BY jp.registrert_dato DESC;
```

### Find SED Documents
```sql
SELECT sd.id, sd.sed_type, sd.rina_saksnummer, sd.registrert_dato
FROM sed_dokument sd
WHERE sd.behandling_id = :behandlingId
ORDER BY sd.registrert_dato DESC;
```

## Common Issues

### Issue: Missing Mottakerinstitusjoner

**Symptom**:
```
FunksjonellException: "Fant ingen mottakerinstitusjoner for BUC LA_BUC_01 og land SE"
```

**Cause**:
- Country not EESSI-ready
- Wrong BUC type for country
- Institution registry outdated

**Investigation**:
```kotlin
// Check available institutions
val institusjoner = eessiService.hentMottakerinstitusjoner(bucType, landkode)
log.info("Institusjoner for $bucType/$landkode: $institusjoner")
```

**Resolution**:
- Verify country is EU/EEA/EFTA member
- Check melosys-eessi institution registry
- May need manual institution lookup

### Issue: Cannot Add SED to Existing BUC

**Symptom**: SED creation fails on existing RINA case

**Investigation**:
```kotlin
// Check BUC status
val erÅpen = eessiService.erBucAapen(rinaSaksnummer)
val tilgjengelige = eessiService.kanOppretteSedTyperPåBuc(rinaSaksnummer)

log.info("BUC $rinaSaksnummer: åpen=$erÅpen, tilgjengelige=$tilgjengelige")
```

**Causes**:
- BUC is closed
- SED type not valid for BUC type
- BUC workflow already completed

### Issue: Wrong BUC Type Selected

**Symptom**: SED sent to wrong workflow

**Investigation**:
```sql
-- Check lovvalgsbestemmelse
SELECT lp.bestemmelse, lp.tilleggsbestemmelse
FROM lovvalgsperiode lp
WHERE lp.behandlingsresultat_id = :behandlingId;
```

**Check mapping**:
```kotlin
val bucType = BucType.fraBestemmelse(bestemmelse)
// Verify this is correct BUC for the case
```

### Issue: GB/UK Special Handling

**Symptom**: Wrong BUC or content for UK case

**Note**: Post-Brexit UK uses EFTA convention

**Investigation**:
```kotlin
// Check if using EFTA mapping
val bucType = if (land == Land_iso2.GB) {
    BucType.fraEftaBestemmelse(bestemmelse)
} else {
    BucType.fraBestemmelse(bestemmelse)
}
```

### Issue: SED Data Incomplete

**Symptom**: SED created but missing data, or PDF generation fails

**Investigation**:
```kotlin
// Check SedDataBygger output
val sedData = sedDataBygger.build(behandling, sedType)
log.info("SedData: bruker=${sedData.bruker}, adresser=${sedData.adresser}")
```

**Common missing data**:
- Arbeidssted (workplace) not set
- Virksomhet (company) missing orgnr
- Address incomplete

### Issue: Concurrent SED Operations

**Symptom**: Race condition on same RINA case

**Note**: Uses LåsReferanse for concurrency control

**Investigation**:
```sql
-- Check for concurrent prosessinstanser
SELECT pi.id, pi.laas_referanse, pi.status, pi.endret_dato
FROM prosessinstans pi
WHERE pi.laas_referanse LIKE '%RINA%'
AND pi.status IN ('KLAR', 'UNDER_BEHANDLING')
ORDER BY pi.endret_dato DESC;
```

## Log Patterns

### EESSI REST Calls
```bash
# All EESSI calls
grep "EessiConsumer" application.log

# Specific operations
grep "opprettBucOgSed\|sendSedPåEksisterendeBuc" application.log

# Errors
grep "EessiConsumer" application.log | grep -i "error\|exception"
```

### SED Building
```bash
# SedDataBygger
grep "SedDataBygger" application.log

# EessiService
grep "EessiService" application.log
```

### Saga Step Execution
```bash
# SED saga steps
grep "SendVedtakUtland\|SedMottakRuting\|OpprettSedGrunnlag" application.log

# With correlation ID
grep "<correlationId>" application.log | grep -i "sed\|eessi"
```

## Key Code Locations

| Component | Location |
|-----------|----------|
| EESSI Consumer | `integrasjon/.../eessi/EessiConsumerImpl.kt` |
| EESSI Service | `service/.../dokument/sed/EessiService.java` |
| SED Data Builder | `service/.../dokument/sed/bygger/SedDataBygger.java` |
| SED Types | `domain/.../eessi/SedType.kt` |
| BUC Types | `domain/.../eessi/BucType.kt` |
| Send Steps | `saksflyt/.../steg/sed/Send*.kt` |
| Receive Steps | `saksflyt/.../steg/sed/SedMottak*.kt` |

## Manual Operations

### Check Institution Availability
```kotlin
@Autowired lateinit var eessiService: EessiService

// List institutions
val institusjoner = eessiService.hentMottakerinstitusjoner(
    BucType.LA_BUC_01, "SE"
)
```

### Generate SED PDF
```kotlin
// Generate PDF for review
val pdf = eessiService.genererSedPdf(behandling, SedType.A003)
```

### Check RINA Case Status
```kotlin
// BUC status
val erÅpen = eessiService.erBucAapen(rinaSaksnummer)
val tilgjengelige = eessiService.kanOppretteSedTyperPåBuc(rinaSaksnummer)
```

## melosys-eessi Service

SED operations go through melosys-eessi service:
- Handles RINA API communication
- Manages institution registry
- Generates SED XML/PDF
- Tracks saksrelasjoner

Check melosys-eessi logs for low-level RINA issues.
