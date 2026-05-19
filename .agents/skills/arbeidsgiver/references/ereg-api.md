# EREG API Reference

## Overview

EREG (Enhetsregisteret) is the Norwegian Entity Register containing information
about all registered organizations in Norway. Melosys accesses EREG via REST API.

## REST Endpoint

```
GET /organisasjon/{orgnummer}
```

Returns organization details for the given 9-digit organization number.

## OrganisasjonRestConsumer

```kotlin
open class OrganisasjonRestConsumer(private val webClient: WebClient) {
    fun hentOrganisasjon(orgnummer: String): OrganisasjonResponse.Organisasjon {
        return webClient.get()
            .uri("/organisasjon/{orgnummer}", orgnummer)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono<OrganisasjonResponse.Organisasjon>()
            .block() ?: throw TekniskException("Ereg organisasjon Response er null")
    }
}
```

## Response DTOs

### Organisasjon (Base)

```kotlin
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = JuridiskEnhet::class, name = "JuridiskEnhet"),
    JsonSubTypes.Type(value = Organisasjonsledd::class, name = "Organisasjonsledd"),
    JsonSubTypes.Type(value = Virksomhet::class, name = "Virksomhet")
)
open class Organisasjon(
    val organisasjonsnummer: String,
    val navn: Navn?,
    val organisasjonDetaljer: OrganisasjonDetaljer?
)
```

### JuridiskEnhet (Legal Entity)

```kotlin
class JuridiskEnhet(
    val bestaarAvOrganisasjonsledd: List<BestaarAvOrganisasjonsledd>,
    val driverVirksomheter: List<DriverVirksomhet>,
    val fisjoner: List<JuridiskEnhetFisjon>,
    val fusjoner: List<JuridiskEnhetFusjon>,
    val knytninger: List<JuridiskEnhetKnytning>,
    val juridiskEnhetDetaljer: JuridiskEnhetDetaljer?,
    organisasjonsnummer: String,
    organisasjonDetaljer: OrganisasjonDetaljer
) : Organisasjon(organisasjonDetaljer, organisasjonsnummer)

data class JuridiskEnhetDetaljer(
    val sektorkode: String?,
    val enhetstype: String?,       // AS, ENK, ANS, etc.
    val harAnsatte: Boolean?,
    val kapitalopplysninger: List<Kapitalopplysninger>?
)
```

### Virksomhet (Business Unit)

```kotlin
class Virksomhet(
    val bestaarAvOrganisasjonsledd: List<BestaarAvOrganisasjonsledd>?,
    val inngaarIJuridiskEnheter: List<InngaarIJuridiskEnhet>?,
    val virksomhetDetaljer: VirksomhetDetaljer?,
    organisasjonsnummer: String,
    organisasjonDetaljer: OrganisasjonDetaljer
) : Organisasjon(organisasjonDetaljer, organisasjonsnummer)

class VirksomhetDetaljer {
    val oppstartsdato: LocalDate?
    val eierskiftedato: LocalDate?
    val nedleggelsesdato: LocalDate?
    val enhetstype: String?
    val ubemannetVirksomhet: Boolean?
}
```

### OrganisasjonDetaljer

```kotlin
data class OrganisasjonDetaljer(
    var registreringsdato: LocalDateTime?,
    var sistEndret: LocalDate?,
    var maalform: String?,                 // NB or NN
    var opphoersdato: LocalDate?,          // Closure date

    // Addresses
    var forretningsadresser: List<Adresse>?,
    var postadresser: List<Adresse>?,

    // Contact info
    var telefonnummer: List<Telefonnummer>?,
    var telefaksnummer: List<Telefonnummer>?,
    var mobiltelefonnummer: List<Telefonnummer>?,
    var epostadresser: List<Epostadresse>?,
    var internettadresser: List<Internettadresse>?,

    // Business info
    var naeringer: List<Naering>?,         // Industry codes (NACE)
    var enhetstyper: List<Enhetstype>?,
    var ansatte: List<Ansatte>?,           // Employee count

    // Names (with history)
    var navn: List<Navn>,

    // Special
    var navSpesifikkInformasjon: NAVSpesifikkInformasjon?,
    var stiftelsesdato: LocalDate?,
    var hjemlandregistre: List<Hjemlandregister>?,
    var formaal: List<Formaal>?
)
```

### Adresse

```kotlin
data class Adresse(
    val type: String?,               // FORRETNING, POST
    val bruksperiode: Bruksperiode,
    val gyldighetsperiode: Gyldighetsperiode,
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?,
    val poststed: String?,
    val kommunenummer: String?,
    val landkode: String?            // ISO 3166-1 alpha-2
)
```

### Navn

```kotlin
data class Navn(
    val bruksperiode: Bruksperiode,
    val gyldighetsperiode: Gyldighetsperiode,
    val sammensattnavn: String?,     // Full name
    val navnelinje1: String?,
    val navnelinje2: String?,
    val navnelinje3: String?,
    val navnelinje4: String?,
    val navnelinje5: String?
)
```

### Naering (Industry)

```kotlin
data class Naering(
    val bruksperiode: Bruksperiode,
    val gyldighetsperiode: Gyldighetsperiode,
    val naeringskode: String?,       // NACE code
    val hjelpeenhet: Boolean?
)
```

### Periods

```kotlin
data class Bruksperiode(
    val fom: LocalDateTime,
    val tom: LocalDateTime?
)

data class Gyldighetsperiode(
    val fom: LocalDate,
    val tom: LocalDate?
)
```

## Organization Types (Enhetstype)

| Code | Description |
|------|-------------|
| `AS` | Aksjeselskap (Limited company) |
| `ASA` | Allmennaksjeselskap (Public limited company) |
| `ENK` | Enkeltpersonforetak (Sole proprietorship) |
| `ANS` | Ansvarlig selskap (General partnership) |
| `DA` | Selskap med delt ansvar (Limited partnership) |
| `NUF` | Norskregistrert utenlandsk foretak (Norwegian-registered foreign enterprise) |
| `BA` | Borettslag (Housing cooperative) |
| `STI` | Stiftelse (Foundation) |
| `STAT` | Staten (Government) |
| `KOMM` | Kommune (Municipality) |
| `FKF` | Fylkeskommunalt foretak |
| `IKS` | Interkommunalt selskap |
| `ADOS` | Administrativ enhet - Loss (Administrative unit) |
| `BEDR` | Bedrift (Business - subunit) |

## Sector Codes (Sektorkode)

| Code | Description |
|------|-------------|
| `6100` | Statlig forvaltning |
| `6500` | Kommunal forvaltning |
| `7000` | Private foretak |
| `8200` | Private organisasjoner uten ervervsformål |

## Domain Model Mapping

### EregDtoTilSaksopplysningKonverter

Converts `OrganisasjonResponse.Organisasjon` to `OrganisasjonDokument`:

```kotlin
fun lagSaksopplysning(organisasjon: Organisasjon): Saksopplysning {
    return Saksopplysning().apply {
        dokument = OrganisasjonDokument(
            orgnummer = organisasjon.organisasjonsnummer,
            navn = finnNavn(organisasjon),
            sektorkode = finnSektorkode(organisasjon),
            enhetstype = finnEnhetstype(organisasjon),
            organisasjonDetaljer = OrganisasjonsDetaljer(
                orgnummer = organisasjon.organisasjonsnummer,
                navn = tilNavn(organisasjonDetaljer.navn),
                forretningsadresse = tilGeografiskAdresse(organisasjonDetaljer.forretningsadresser),
                postadresse = tilGeografiskAdresse(organisasjonDetaljer.postadresser),
                telefon = tilTelefon(organisasjonDetaljer.telefonnummer),
                epostadresse = tilEpost(organisasjonDetaljer.epostadresser),
                naering = organisasjonDetaljer.naeringer?.mapNotNull { it.naeringskode },
                opphoersdato = organisasjonDetaljer.opphoersdato
            )
        )
    }
}
```

## OrganisasjonDokument

The domain model used throughout the application:

```kotlin
class OrganisasjonDokument(
    val orgnummer: String,
    val navn: String,
    val enhetstype: String?,
    val sektorkode: String,
    val organisasjonDetaljer: OrganisasjonsDetaljer
) {
    fun getForretningsadresse(): StrukturertAdresse?
    fun getPostadresse(): StrukturertAdresse?
    fun harRegistrertPostadresse(): Boolean
    fun harRegistrertForretningsadresse(): Boolean
    fun harRegistrertAdresse(): Boolean
    fun hentTilgjengeligAdresse(): StrukturertAdresse?
}
```

## Validation

Organization numbers are validated:
- Must be exactly 9 digits
- 11-digit numbers (fnr) are rejected

```kotlin
private fun erUgyldigOrgnummer(orgnr: String) = orgnr.length == 11
```

## Error Handling

| Exception | Cause |
|-----------|-------|
| `TekniskException("orgnr er ikke gyldig")` | Invalid orgnummer format |
| `TekniskException("Ereg organisasjon Response er null")` | No response from EREG |
| `IkkeFunnetException` | Organization not found in EREG |
