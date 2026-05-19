---
name: brev
description: |
  Expert knowledge of Brev (letter generation) in melosys-api.
  Use when: (1) Producing letters and decision documents (vedtaksbrev),
  (2) Understanding Dokgen templates and mapping,
  (3) Configuring letter recipients and distribution,
  (4) Debugging letter generation issues,
  (5) Adding new letter types or templates.
---

# Brev (Letter Generation) System

Melosys generates letters through melosys-dokgen service. Letters include decision letters (vedtaksbrev),
information letters (infobrev), and various notifications. After generation, letters are archived
in Joark and distributed to recipients.

## Quick Reference

### Module Structure
```
service/dokument/
├── DokgenService.java           # Main letter service (new)
├── DokumentService.java         # Legacy document service
├── DokumentServiceFasade.java   # Facade routing to correct service
├── BrevmottakerService.java     # Recipient logic
└── brev/
    ├── BrevbestillingDto.java   # Letter order request
    ├── BrevDataService.java     # Data preparation
    └── mapper/
        ├── DokgenMalMapper.kt           # Template data mapper
        ├── DokumentproduksjonsInfoMapper.java  # Template metadata
        └── *Mapper.java                 # Specific letter mappers

integrasjon/dokgen/
├── DokgenConsumer.java          # REST client to melosys-dokgen
└── dto/                         # Template DTOs

saksflyt/steg/brev/
├── SendOrienteringsbrevTrygdeavgift.kt
└── SendManglendeInnbetalingVarselBrev.kt
```

### Key Services

| Service | Purpose |
|---------|---------|
| `DokgenService` | Produces letters via melosys-dokgen (new) |
| `DokumentService` | Legacy: produces via Team Dokument's doksys |
| `DokumentServiceFasade` | Routes to correct service based on template availability |
| `BrevmottakerService` | Determines letter recipients |
| `DokgenMalMapper` | Maps behandling data to template DTOs |

### Letter Flow

```
1. BrevbestillingDto created (frontend or saga)
   ↓
2. DokumentServiceFasade.produserDokument()
   ↓
3. Check: erTilgjengeligDokgenmal()?
   ↓ Yes                    ↓ No
4a. DokgenService          4b. DokumentService (legacy)
   ↓                           ↓
5. DokgenMalMapper.lagDokgenDtoFraBestilling()
   ↓
6. DokgenConsumer.lagPdf()
   ↓
7. JoarkService.opprettJournalpost()
   ↓
8. Distribution (print/digital)
```

## Letter Types (Produserbaredokumenter)

### Decision Letters (Vedtaksbrev)

| Type | Template | Description |
|------|----------|-------------|
| `INNVILGELSE_FOLKETRYGDLOVEN` | innvilgelse_ftrl | Approve FTRL membership |
| `INNVILGELSE_EFTA_STORBRITANNIA` | innvilgelse_efta_storbritannia | Approve EFTA/GB |
| `VEDTAK_OPPHOERT_MEDLEMSKAP` | vedtak_opphoert_medlemskap | Terminate membership |
| `IKKE_YRKESAKTIV_VEDTAKSBREV` | ikke_yrkesaktiv_vedtaksbrev | Non-employed decision |
| `PLIKTIG_MEDLEM_FTRL` | pliktig_medlem_ftrl | Mandatory FTRL membership |
| `PENSJONIST_PLIKTIG_FTRL` | pensjonist_pliktig_ftrl | Pensioner mandatory |
| `PENSJONIST_FRIVILLIG_FTRL` | pensjonist_frivillig_ftrl | Pensioner voluntary |
| `AARSAVREGNING_VEDTAKSBREV` | aarsavregning_vedtaksbrev | Annual settlement |
| `TRYGDEAVTALE_*` | trygdeavtale_* | Bilateral treaty decisions |

### Rejection Letters

| Type | Template | Description |
|------|----------|-------------|
| `AVSLAG_MANGLENDE_OPPLYSNINGER` | avslag_manglende_opplysninger | Reject: missing info |
| `AVSLAG_EFTA_STORBRITANNIA` | avslag_efta_storbritannia | Reject EFTA/GB |

### Information Letters (Infobrev)

| Type | Template | Description |
|------|----------|-------------|
| `MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD` | saksbehandlingstid_soknad | Processing time notice |
| `MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE` | saksbehandlingstid_klage | Appeal processing notice |
| `MANGELBREV_BRUKER` | mangelbrev_bruker | Missing info request (person) |
| `MANGELBREV_ARBEIDSGIVER` | mangelbrev_arbeidsgiver | Missing info request (employer) |
| `ORIENTERING_ANMODNING_UNNTAK` | orientering_anmodning_unntak | Art. 16 exception notice |
| `ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK` | orientering_til_arbeidsgiver_om_vedtak | Decision notice to employer |
| `TRYGDEAVGIFT_INFORMASJONSBREV` | trygdeavgift_informasjonsbrev | Charge information |
| `VARSELBREV_MANGLENDE_INNBETALING` | varsel_manglende_innbetaling | Missing payment warning |
| `MELDING_HENLAGT_SAK` | henleggelse | Case dismissed notice |

### Free-Text Letters

| Type | Template | Description |
|------|----------|-------------|
| `FRITEKSTBREV` | fritekstbrev | Generic free-text |
| `GENERELT_FRITEKSTBREV_BRUKER` | fritekstbrev | To person |
| `GENERELT_FRITEKSTBREV_ARBEIDSGIVER` | fritekstbrev | To employer |
| `GENERELT_FRITEKSTBREV_VIRKSOMHET` | fritekstbrev | To company |
| `UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV` | trygdeavtale_fritekstbrev | To foreign authority |

## Key Operations

### Produce Letter
```kotlin
// Via facade (recommended)
dokumentServiceFasade.produserDokument(
    produserbartDokument = INNVILGELSE_FOLKETRYGDLOVEN,
    mottaker = Mottaker.medRolle(BRUKER),
    behandlingId = 123L,
    brevbestilling = brevbestillingDto
)

// DokgenService directly
dokgenService.produserOgDistribuerBrev(behandlingId, brevbestillingDto)
```

### Produce Draft (Utkast)
```kotlin
val pdfBytes = dokgenService.produserUtkast(behandlingId, brevbestillingDto)
// Returns PDF without archiving/distribution
```

### Check Template Availability
```kotlin
val erTilgjengelig = dokgenService.erTilgjengeligDokgenmal(INNVILGELSE_FOLKETRYGDLOVEN)
```

## Recipients (Mottaker)

### Mottakerroller

| Role | Description |
|------|-------------|
| `BRUKER` | Primary person (applicant) |
| `ARBEIDSGIVER` | Employer |
| `VIRKSOMHET` | Company/organization |
| `UTENLANDSK_TRYGDEMYNDIGHET` | Foreign authority |
| `NORSK_MYNDIGHET` | Norwegian authority |
| `FULLMEKTIG` | Power of attorney representative |
| `ANNEN_PERSON` | Other person |

### Copy Recipients (Kopimottakere)
```kotlin
brevbestillingDto.kopiMottakere = listOf(
    KopiMottakerDto(rolle = ARBEIDSGIVER, orgnr = "987654321"),
    KopiMottakerDto(rolle = FULLMEKTIG, ident = "12345678901")
)
```

## Template Mapping

### DokumentproduksjonsInfo
```kotlin
data class DokumentproduksjonsInfo(
    val dokgenMalnavn: String,      // Template name in melosys-dokgen
    val dokumentKategoriKode: String, // Joark category (IB, VB, etc.)
    val tittel: String,              // Main document title
    val vedleggTittel: String?,      // Attachment title (optional)
    val attestTittel: String?        // Certificate title (optional)
)
```

### Document Categories

| Code | Description | Usage |
|------|-------------|-------|
| `VB` | Vedtaksbrev | Decision letters |
| `IB` | Infobrev | Information/notice letters |

## Adding New Letter Types

1. **Add enum** in `melosys-internt-kodeverk`:
   ```kotlin
   enum class Produserbaredokumenter {
       NEW_LETTER_TYPE("Beskrivelse for saksbehandler")
   }
   ```

2. **Create template** in `melosys-dokgen`:
   - Add HTML template
   - Define JSON schema for data

3. **Add mapping** in `DokumentproduksjonsInfoMapper`:
   ```java
   .put(NEW_LETTER_TYPE,
       new DokumentproduksjonsInfo("template_name",
           DokumentKategoriKode.VB.getKode(),
           "Journalførings tittel"))
   ```

4. **Add data mapper** in `DokgenMalMapper`:
   ```kotlin
   NEW_LETTER_TYPE -> lagNewLetterDto(behandling, bestilling)
   ```

5. **Configure recipients** in `BrevmottakerService` if needed

## Common Issues

### Issue: Template Not Found

**Symptom**: `FunksjonellException: "ProduserbartDokument X er ikke støttet"`

**Check**:
1. Is enum in `melosys-internt-kodeverk`?
2. Is mapping in `DokumentproduksjonsInfoMapper`?
3. Does template exist in `melosys-dokgen`?

### Issue: Wrong Recipients

**Symptom**: Letter sent to wrong party

**Check**:
```kotlin
val mottakere = brevmottakerService.hentMottakere(behandling, brevbestillingDto)
log.info("Recipients: ${mottakere.map { it.rolle }}")
```

### Issue: Missing Data in Letter

**Symptom**: Blank fields in generated PDF

**Check**: Data mapping in `DokgenMalMapper.lagDokgenDtoFraBestilling()`

## Key Code Locations

| Component | Location |
|-----------|----------|
| Main Service | `service/.../dokument/DokgenService.java` |
| Legacy Service | `service/.../dokument/DokumentService.java` |
| Facade | `service/.../dokument/DokumentServiceFasade.java` |
| Template Mapper | `service/.../dokument/brev/mapper/DokgenMalMapper.kt` |
| Production Info | `service/.../dokument/brev/mapper/DokumentproduksjonsInfoMapper.java` |
| REST Consumer | `integrasjon/.../dokgen/DokgenConsumer.java` |
| Recipient Service | `service/.../dokument/BrevmottakerService.java` |

## Detailed Documentation

- **[Letter Types](references/letter-types.md)**: Complete letter type reference
- **[Templates](references/templates.md)**: Template mapping and DTOs
- **[Debugging](references/debugging.md)**: Troubleshooting guide
