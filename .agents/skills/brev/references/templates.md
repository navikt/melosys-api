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
    fun lagDokgenDtoFraBestilling(
        produserbardokument: Produserbaredokumenter,
        behandling: Behandling,
        bestilling: BrevbestillingDto
    ): Any {
        return when (produserbardokument) {
            MANGELBREV_BRUKER -> lagMangelbrevBruker(behandling, bestilling)
            INNVILGELSE_FOLKETRYGDLOVEN -> innvilgelseFtrlMapper.map(...)
            TRYGDEAVTALE_GB -> lagTrygdeavtaleDokument(behandling, bestilling)
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

## DokgenConsumer

REST client to melosys-dokgen:

```kotlin
@Component
class DokgenConsumer(
    private val webClient: WebClient
) {
    fun lagPdf(malnavn: String, data: Any): ByteArray {
        return webClient.post()
            .uri("/template/$malnavn/create-pdf")
            .bodyValue(data)
            .retrieve()
            .bodyToMono<ByteArray>()
            .block() ?: throw TekniskException("Feil ved PDF-generering")
    }

    fun lagPdfForStandardvedlegg(type: StandardvedleggType): ByteArray {
        return webClient.get()
            .uri("/standardvedlegg/${type.name.lowercase()}")
            .retrieve()
            .bodyToMono<ByteArray>()
            .block() ?: ByteArray(0)
    }
}
```

## Standard Attachments (Standardvedlegg)

Pre-generated PDF attachments:

| Type | Description |
|------|-------------|
| `RETTIGHETER_OG_PLIKTER` | Rights and obligations |
| `KLAGEVEILEDNING` | Appeal guidance |

```kotlin
enum class StandardvedleggType {
    RETTIGHETER_OG_PLIKTER,
    KLAGEVEILEDNING
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
