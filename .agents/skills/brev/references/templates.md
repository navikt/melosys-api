# Templates Reference

## melosys-dokgen Service

Templates are managed in the separate `melosys-dokgen` repository:
- HTML templates with Handlebars
- JSON schemas for data validation
- Produces PDF via gotenberg

## Template Names (malnavn)

Templates are mapped in `DokumentproduksjonsInfoMapper`:

| malnavn | Letter Types |
|---------|--------------|
| `saksbehandlingstid_soknad` | MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD |
| `saksbehandlingstid_klage` | MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE |
| `mangelbrev_bruker` | MANGELBREV_BRUKER |
| `mangelbrev_arbeidsgiver` | MANGELBREV_ARBEIDSGIVER |
| `innvilgelse_ftrl` | INNVILGELSE_FOLKETRYGDLOVEN |
| `vedtak_opphoert_medlemskap` | VEDTAK_OPPHOERT_MEDLEMSKAP |
| `trygdeavtale_gb` | TRYGDEAVTALE_GB |
| `trygdeavtale_us` | TRYGDEAVTALE_US |
| `trygdeavtale_ca` | TRYGDEAVTALE_CAN |
| `trygdeavtale_au` | TRYGDEAVTALE_AU |
| `innhenting_av_inntektsopplysninger` | INNHENTING_AV_INNTEKTSOPPLYSNINGER |
| `orientering_anmodning_unntak` | ORIENTERING_ANMODNING_UNNTAK |
| `orientering_til_arbeidsgiver_om_vedtak` | ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK |
| `fritekstbrev` | FRITEKSTBREV, GENERELT_FRITEKSTBREV_* |
| `fritekstvedlegg` | GENERELT_FRITEKSTVEDLEGG |
| `trygdeavtale_fritekstbrev` | UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV |
| `avslag_manglende_opplysninger` | AVSLAG_MANGLENDE_OPPLYSNINGER |
| `henleggelse` | MELDING_HENLAGT_SAK |
| `ikke_yrkesaktiv_vedtaksbrev` | IKKE_YRKESAKTIV_VEDTAKSBREV |
| `ikke_yrkesaktiv_pliktig_ftrl` | IKKE_YRKESAKTIV_PLIKTIG_FTRL |
| `innvilgelse_efta_storbritannia` | INNVILGELSE_EFTA_STORBRITANNIA |
| `ikke_yrkesaktiv_frivillig_ftrl` | IKKE_YRKESAKTIV_FRIVILLIG_FTRL |
| `pliktig_medlem_ftrl` | PLIKTIG_MEDLEM_FTRL |
| `pensjonist_pliktig_ftrl` | PENSJONIST_PLIKTIG_FTRL |
| `pensjonist_frivillig_ftrl` | PENSJONIST_FRIVILLIG_FTRL |
| `trygdeavgift_informasjonsbrev` | TRYGDEAVGIFT_INFORMASJONSBREV |
| `varsel_manglende_innbetaling` | VARSELBREV_MANGLENDE_INNBETALING |
| `avslag_efta_storbritannia` | AVSLAG_EFTA_STORBRITANNIA |
| `aarsavregning_vedtaksbrev` | AARSAVREGNING_VEDTAKSBREV |

## DokgenMalMapper

Maps behandling data to template-specific DTOs:

```kotlin
@Component
class DokgenMalMapper(
    private val dokgenMapperDatahenter: DokgenMapperDatahenter,
    private val innvilgelseFtrlMapper: InnvilgelseFtrlMapper,
    // ... other mappers
) {
    // Public entry point used by DokgenService
    fun mapBehandling(
        mottattBrevbestilling: DokgenBrevbestilling,
        mottaker: Mottaker
    ): DokgenDto {
        // beriker bestillingen med persondata, deretter:
        return lagDokgenDtoFraBestilling(brevbestillingBuilder.build())
    }

    // Internal dispatch on produserbartDokument (single DokgenBrevbestilling arg)
    private fun lagDokgenDtoFraBestilling(brevbestilling: DokgenBrevbestilling): DokgenDto {
        return when (brevbestilling.produserbartDokument) {
            MANGELBREV_BRUKER -> lagMangelbrevBruker(brevbestilling)
            INNVILGELSE_FOLKETRYGDLOVEN -> innvilgelseFtrlMapper.map(...)
            TRYGDEAVTALE_GB -> lagTrygdeavtaleDokument(brevbestilling, ...)
            // ... other mappings
        }
    }
}
```

## Template DTOs

Each template has a corresponding DTO in `integrasjon/dokgen/dto/`:

### Common Fields
```kotlin
// Most templates include:
data class CommonBrevDto(
    val mottaker: MottakerDto,
    val saksnummer: String,
    val avsender: AvsenderDto,
    val dato: LocalDate,
    val fritekst: String?
)
```

### Vedtaksbrev Fields
```kotlin
data class VedtaksbrevDto(
    // Person info
    val brukerNavn: String,
    val fnr: String,

    // Decision
    val vedtaksdato: LocalDate,
    val klagefrist: LocalDate,

    // Period
    val periodeFom: LocalDate,
    val periodeTom: LocalDate?,

    // Result
    val innvilget: Boolean,
    val lovvalgsland: String?,

    // Justification
    val begrunnelse: String?
)
```

### MottakerDto
```kotlin
data class MottakerDto(
    val navn: String,
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?,
    val poststed: String?,
    val land: String?
)
```

## Specific Mappers

### InnvilgelseFtrlMapper
Maps data for FTRL approval letters:
- Membership type (frivillig/pliktig)
- Period dates
- Charge information

### TrygdeavtaleMapper
Maps data for bilateral treaty letters:
- Country-specific fields
- Certificate data
- Attestation

### ÅrsavregningVedtakMapper
Maps data for annual settlement letters:
- Previous year charges
- Adjustments
- New amounts

## DokgenClient

REST client to melosys-dokgen (`integrasjon/.../dokgen/DokgenClient.java`).
The real contract takes a typed `DokgenDto` (not an untyped `Any`) and posts to
`/mal/{malNavn}/lag-pdf`:

```java
@Retryable
public class DokgenClient {

    public byte[] lagPdf(String malNavn, DokgenDto dokgenDto,
                         boolean bestillKopi, boolean bestillUtkast) {
        return webClient.post()
            .uri("/mal/{malNavn}/lag-pdf?somKopi={bestillKopi}&utkast={bestillUtkast}",
                 malNavn, bestillKopi, bestillUtkast)
            .bodyValue(dokgenDto)
            .retrieve()
            .bodyToMono(byte[].class)
            .block();
    }

    public byte[] lagPdfForStandardvedlegg(String malNavn, StandardvedleggDto standardvedlegg) {
        // posts to the same /mal/{malNavn}/lag-pdf endpoint with the standardvedlegg body
    }
}
```

## Standard Attachments (Standardvedlegg)

Pre-generated PDF attachments:

| Type | malnavn | Description |
|------|---------|-------------|
| `VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE` | info_om_rettigheter_innvilgelse | Rights and obligations (approval) |
| `VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_AVSLAG` | info_om_rettigheter_avslag | Rights and obligations (rejection) |

```kotlin
// domain/.../brev/StandardvedleggType.kt
enum class StandardvedleggType(
    val malnavn: String,
    val journalføringstittel: String,
    val frontendTittel: String
) {
    VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_AVSLAG(...),
    VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE(...)
}
```

## Adding New Template

### 1. Create Template in melosys-dokgen
```
templates/
└── new_template/
    ├── template.html      # Handlebars template
    └── schema.json        # JSON schema for data
```

### 2. Create DTO in melosys-api
```kotlin
// integrasjon/dokgen/dto/NewTemplateDto.kt
data class NewTemplateDto(
    val mottaker: MottakerDto,
    val saksnummer: String,
    // ... template-specific fields
)
```

### 3. Add Mapping
```kotlin
// DokgenMalMapper.kt
NEW_LETTER_TYPE -> lagNewTemplateDto(behandling, bestilling)

private fun lagNewTemplateDto(
    behandling: Behandling,
    bestilling: BrevbestillingDto
): NewTemplateDto {
    return NewTemplateDto(
        mottaker = lagMottaker(bestilling),
        saksnummer = behandling.fagsak.saksnummer,
        // ... map other fields
    )
}
```

### 4. Register in DokumentproduksjonsInfoMapper
```java
.put(NEW_LETTER_TYPE,
    new DokumentproduksjonsInfo(
        "new_template",           // malnavn
        DokumentKategoriKode.VB.getKode(),
        "Journal title"))
```
