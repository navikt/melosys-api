# SED/EESSI Debugging Guide

## Common SQL Queries

### Find RINA Case Number for Behandling

There is **no** `seddokument` / `sed_dokument` table and no
`opplysningstype = 'RINA_SAKSNUMMER'` row (that literal only exists in test code).
SED data is stored as a SED-opplysning in the `saksopplysning` table
(`opplysning_type = 'SEDOPPL'`), serialized as XML in the `dokument` column
(domain type `SedDokument`, a `SaksopplysningDokument`). The RINA saksnummer lives
**inside that XML blob** (path `/sedDokument/...`), so it is not a plain column.
It is also embedded as the prefix of the saga lock reference
(`prosessinstans.sed_laas_referanse`, form `rinaSaksnummer_sedId_versjon`).

```sql
-- Locate the SED saksopplysning for a behandling (inspect dokument for the rina saksnummer)
SELECT s.id, s.opplysning_type, s.registrert_dato, s.dokument
FROM saksopplysning s
WHERE s.behandling_id = :behandlingId
AND s.opplysning_type = 'SEDOPPL'
ORDER BY s.registrert_dato DESC;
```

### Find All SED-Related Saga Steps
```sql
-- prosessinstans columns: prosess_type, sist_fullfort_steg, status, endret_dato, registrert_dato
SELECT pi.uuid, pi.prosess_type, pi.sist_fullfort_steg, pi.status, pi.endret_dato
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND (
    pi.prosess_type LIKE '%SED%'
    OR pi.sist_fullfort_steg LIKE '%SED%'
    OR pi.sist_fullfort_steg LIKE '%UTLAND%'
    OR pi.sist_fullfort_steg LIKE '%ANMODNING%'
)
ORDER BY pi.registrert_dato DESC;
```

### Find Failed SED Steps
```sql
-- behandling joins fagsak via the saksnummer column (FK fk_behandling_fagsak)
SELECT pi.uuid, pi.prosess_type, pi.sist_fullfort_steg, pi.status,
       b.id as behandling_id, f.saksnummer
FROM prosessinstans pi
JOIN behandling b ON b.id = pi.behandling_id
JOIN fagsak f ON f.saksnummer = b.saksnummer
WHERE pi.status = 'FEILET'
AND pi.sist_fullfort_steg IN (
    'SEND_VEDTAK_UTLAND',
    'SEND_ANMODNING_OM_UNNTAK',
    'SEND_SVAR_ANMODNING_UNNTAK',
    'SED_MOTTAK_RUTING',
    'OPPRETT_SED_GRUNNLAG'
)
ORDER BY pi.endret_dato DESC;
```

### Find Incoming SEDs (Journalposter)

There is **no** `journalpost` table in melosys-api — journalpost metadata lives in
SAF/Joark (external). melosys-api only keeps the initierende journalpost id on the
behandling (`behandling.initierende_journalpost_id`, plus
`behandling.initierende_dokument_id`):
```sql
SELECT b.id, b.initierende_journalpost_id, b.initierende_dokument_id, b.registrert_dato
FROM behandling b
WHERE b.id = :behandlingId;
```

### Find SED Documents

SED documents are SED-opplysninger in the `saksopplysning` table (no dedicated
`sed_dokument` table); `sed_type` and `rina_saksnummer` live inside `dokument`
(the kildesystem is on the related `saksopplysning_kilde.kildesystem`):
```sql
SELECT s.id, s.registrert_dato, sk.kildesystem, s.dokument
FROM saksopplysning s
LEFT JOIN saksopplysning_kilde sk ON sk.saksopplysning_id = s.id
WHERE s.behandling_id = :behandlingId
AND s.opplysning_type = 'SEDOPPL'
ORDER BY s.registrert_dato DESC;
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
```java
// Check available institutions (takes bucType name + a collection of landkoder)
List<Institusjon> institusjoner = eessiService.hentEessiMottakerinstitusjoner(bucType.name(), Set.of(landkode));
log.info("Institusjoner for {}/{}: {}", bucType, landkode, institusjoner);
```

**Resolution**:
- Verify country is EU/EEA/EFTA member
- Check melosys-eessi institution registry
- May need manual institution lookup

### Issue: Cannot Add SED to Existing BUC

**Symptom**: SED creation fails on existing RINA case

**Investigation**:
```java
// Check BUC status. erBucAapen takes the long arkivsakID; kanOppretteSedTyperPåBuc
// takes (rinaSaksnummer, sedType) and returns a boolean.
boolean erÅpen = eessiService.erBucAapen(arkivsakID);
boolean kanOpprette = eessiService.kanOppretteSedTyperPåBuc(rinaSaksnummer, ønsketSedType);

log.info("BUC arkivsak={} rina={}: åpen={}, kanOpprette={}", arkivsakID, rinaSaksnummer, erÅpen, kanOpprette);
```

**Causes**:
- BUC is closed
- SED type not valid for BUC type
- BUC workflow already completed

### Issue: Wrong BUC Type Selected

**Symptom**: SED sent to wrong workflow

**Investigation**:
```sql
-- Check lovvalgsbestemmelse (table is lovvalg_periode; FK column is beh_resultat_id)
SELECT lp.lovvalg_bestemmelse, lp.tillegg_bestemmelse, lp.lovvalgsland
FROM lovvalg_periode lp
WHERE lp.beh_resultat_id = :behResultatId;
```

**Check mapping**:
```kotlin
val bucType = BucType.fraBestemmelse(bestemmelse)
// Verify this is the correct BUC for the case
```

### Issue: GB/UK Special Handling

**Symptom**: Wrong BUC or content for UK case

**Note**: Post-Brexit UK uses the EEA EFTA convention. There is no separate
`fraEftaBestemmelse` — convention bestemmelser go through the same
`BucType.fraBestemmelse(..)` (private EFTA helpers internally). For UK convention
periods, EessiService also appends "Issued under the EEA EFTA Convention." to the
SED's ytterligere informasjon.

**Investigation**:
```kotlin
// Same call regardless of country; EFTA/UK bestemmelser are handled inside
val bucType = BucType.fraBestemmelse(bestemmelse)
```

### Issue: SED Data Incomplete

**Symptom**: SED created but missing data, or PDF generation fails

**Investigation**:
```java
// SedDataBygger.lag(..) / lagUtkast(..) take a SedDataGrunnlag, not a Behandling
SedDataGrunnlag grunnlag = dataGrunnlagFactory.av(behandling);
SedDataDto sedData = sedDataBygger.lag(grunnlag, behandlingsresultat, periodeType);
log.info("SedData: bruker={}, arbeidssteder={}", sedData.getBruker(), sedData.getArbeidssteder());
```

**Common missing data**:
- Arbeidssted (workplace) not set
- Virksomhet (company) missing orgnr
- Address incomplete

### Issue: Concurrent SED Operations

**Symptom**: Race condition on same RINA case

**Note**: Uses the SED lock reference for concurrency control. The lock column is
`sed_laas_referanse` (form `rinaSaksnummer_sedId_versjon`); prosessinstanser
sharing the same RINA prefix are serialised.

**Investigation**:
```sql
-- Check for prosessinstanser locked on the same RINA case (prefix before the first '_')
SELECT pi.uuid, pi.sed_laas_referanse, pi.status, pi.endret_dato
FROM prosessinstans pi
WHERE pi.sed_laas_referanse LIKE :rinaSaksnummer || '\_%' ESCAPE '\'
AND pi.status IN ('KLAR', 'UNDER_BEHANDLING')
ORDER BY pi.endret_dato DESC;
```

## Log Patterns

### EESSI REST Calls
```bash
# All EESSI calls (the REST collaborator is EessiClient, not EessiConsumer)
grep "EessiClient" application.log

# Specific operations
grep "opprettBucOgSed\|sendSedPåEksisterendeBuc" application.log

# Errors
grep "EessiClient" application.log | grep -i "error\|exception"
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
| EESSI Client (REST collaborator) | `integrasjon/.../eessi/EessiClient.kt` |
| EESSI Service | `service/.../dokument/sed/EessiService.java` |
| SED Data Builder | `service/.../dokument/sed/bygger/SedDataBygger.java` |
| SED Types | `domain/.../eessi/SedType.kt` |
| BUC Types | `domain/.../eessi/BucType.kt` |
| Send Steps | `saksflyt/.../steg/sed/Send*.java` |
| Receive Steps | `saksflyt/.../steg/sed/mottak/SedMottak*.java` |

## Manual Operations

### Check Institution Availability
```java
// List institutions (bucType name + collection of landkoder)
List<Institusjon> institusjoner = eessiService.hentEessiMottakerinstitusjoner(
    BucType.LA_BUC_01.name(), Set.of("SE")
);
```

### Generate SED PDF
```java
// genererSedPdf takes the long behandlingID + SedType (not a Behandling)
byte[] pdf = eessiService.genererSedPdf(behandlingID, SedType.A003);
```

### Check RINA Case Status
```java
boolean erÅpen = eessiService.erBucAapen(arkivsakID);                          // long arkivsakID
boolean kanOpprette = eessiService.kanOppretteSedTyperPåBuc(rinaSaksnummer, SedType.A012); // (rina, sedType) -> boolean
```

## melosys-eessi Service

SED operations go through melosys-eessi service:
- Handles RINA API communication
- Manages institution registry
- Generates SED XML/PDF
- Tracks saksrelasjoner

