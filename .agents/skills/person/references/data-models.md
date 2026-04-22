# Person Data Models Reference

## Domain Models

Located in `domain/src/main/kotlin/no/nav/melosys/domain/person/`

### Persondata Interface

```kotlin
interface Persondata : SaksopplysningDokument {
    fun erPersonDød(): Boolean
    fun harStrengtAdressebeskyttelse(): Boolean
    fun manglerGyldigRegistrertAdresse(): Boolean
    fun hentFolkeregisterident(): String?
    fun hentAlleStatsborgerskap(): Set<Land>
    fun hentKjønnType(): KjoennType
    val fornavn: String?
    val mellomnavn: String?
    val etternavn: String?
    val sammensattNavn: String?
    fun hentFamiliemedlemmer(): Set<Familiemedlem>?
    val fødselsdato: LocalDate?
    fun finnBostedsadresse(): Optional<Bostedsadresse>
    fun finnKontaktadresse(): Optional<Kontaktadresse>
    fun finnOppholdsadresse(): Optional<Oppholdsadresse>
    fun hentGjeldendePostadresse(): Postadresse?
}
```

### Personopplysninger

Main implementation of Persondata:

```kotlin
data class Personopplysninger(
    var adressebeskyttelser: Collection<Adressebeskyttelse>,
    var bostedsadresse: Bostedsadresse?,
    var dødsfall: Doedsfall?,
    var familiemedlemmer: Set<Familiemedlem>?,
    var fødsel: Foedsel?,
    var folkeregisteridentifikator: Folkeregisteridentifikator?,
    var kjønn: KjoennType?,
    var kontaktadresser: Collection<Kontaktadresse>,
    var navn: Navn?,
    var oppholdsadresser: Collection<Oppholdsadresse>,
    var statsborgerskap: Collection<Statsborgerskap>
) : Persondata
```

### PersonMedHistorikk

Used for displaying historical person data:

```kotlin
data class PersonMedHistorikk(
    val bostedsadresser: Collection<Bostedsadresse>,
    val dødsfall: Doedsfall?,
    val fødsel: Foedsel?,
    val folkeregisteridentifikator: Folkeregisteridentifikator,
    val folkeregisterpersonstatuser: Collection<Folkeregisterpersonstatus>,
    val kjønn: KjoennType,
    val kontaktadresser: Collection<Kontaktadresse>,
    val navn: Navn,
    val oppholdsadresser: Collection<Oppholdsadresse>,
    val sivilstand: Collection<Sivilstand>,
    val statsborgerskap: Collection<Statsborgerskap>
) : SaksopplysningDokument
```

## Supporting Types

### Navn

```kotlin
data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val master: String?
) {
    fun tilSammensattNavn(): String {
        // Returns "Fornavn Mellomnavn Etternavn" (skips null mellomnavn)
    }
}
```

### Foedsel

```kotlin
data class Foedsel(
    val fødeland: String?,      // ISO country code
    val fødested: String?,
    val fødselsdato: LocalDate?,
    val fødselsår: Int?
)
```

### Doedsfall

```kotlin
data class Doedsfall(
    val dødsdato: LocalDate?,
    val master: String?
)
```

### Folkeregisteridentifikator

```kotlin
data class Folkeregisteridentifikator(
    val identifikasjonsnummer: String,
    val master: String?
)
```

### KjoennType

```kotlin
enum class KjoennType {
    MANN,
    KVINNE,
    UKJENT
}
```

### Statsborgerskap

```kotlin
data class Statsborgerskap(
    val landkode: String,           // ISO 3166-1 alpha-3
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?
)
```

### Sivilstand

```kotlin
data class Sivilstand(
    val type: Sivilstandstype,
    val relatertVedSivilstand: String?,
    val gyldigFraOgMed: LocalDate?,
    val bekreftelsesdato: LocalDate?,
    val master: String?,
    val historisk: Boolean
)
```

### Sivilstandstype

```kotlin
enum class Sivilstandstype {
    ENKE_ELLER_ENKEMANN,
    GIFT,
    GJENLEVENDE_PARTNER,
    REGISTRERT_PARTNER,
    SEPARERT,
    SEPARERT_PARTNER,
    SKILT,
    SKILT_PARTNER,
    UGIFT,
    UOPPGITT
}
```

### Folkeregisterpersonstatus

```kotlin
data class Folkeregisterpersonstatus(
    val status: String,
    val gyldighetstidspunkt: LocalDate?,
    val historisk: Boolean
)
```

## Address Models

Located in `domain/src/main/kotlin/no/nav/melosys/domain/person/adresse/`

### Adressebeskyttelse

```kotlin
data class Adressebeskyttelse(
    val gradering: Gradering
) {
    fun erStrengtFortrolig(): Boolean {
        return gradering == Gradering.STRENGT_FORTROLIG ||
               gradering == Gradering.STRENGT_FORTROLIG_UTLAND
    }

    enum class Gradering {
        FORTROLIG,
        STRENGT_FORTROLIG,
        STRENGT_FORTROLIG_UTLAND,
        UGRADERT
    }
}
```

### Bostedsadresse

```kotlin
data class Bostedsadresse(
    val gyldigFraOgMed: LocalDateTime?,
    val gyldigTilOgMed: LocalDateTime?,
    val coAdressenavn: String?,
    val strukturertAdresse: StrukturertAdresse?,
    val master: String?,
    val historisk: Boolean
)
```

### Kontaktadresse

```kotlin
data class Kontaktadresse(
    val gyldigFraOgMed: LocalDateTime?,
    val gyldigTilOgMed: LocalDateTime?,
    val coAdressenavn: String?,
    val strukturertAdresse: StrukturertAdresse?,
    val semistrukturertAdresse: SemistrukturertAdresse?,
    val master: String?,
    val registrertDato: LocalDateTime?
) {
    fun erGyldig(): Boolean {
        // Checks if current date is within validity period
    }
}
```

### Oppholdsadresse

```kotlin
data class Oppholdsadresse(
    val gyldigFraOgMed: LocalDateTime?,
    val gyldigTilOgMed: LocalDateTime?,
    val coAdressenavn: String?,
    val strukturertAdresse: StrukturertAdresse?,
    val master: String?,
    val registrertDato: LocalDateTime?
)
```

### StrukturertAdresse

```kotlin
data class StrukturertAdresse(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?,
    val poststed: String?,
    val land: Land?
)
```

## Family Models

Located in `domain/src/main/kotlin/no/nav/melosys/domain/person/familie/`

### Familiemedlem

```kotlin
data class Familiemedlem(
    val relasjon: Relasjon,
    val navn: Navn?,
    val fødselsdato: LocalDate?,
    val fødeland: String?,
    val folkeregisterident: String?,
    val foreldreansvar: Foreldreansvar?
)

enum class Relasjon {
    BARN,
    EKTEFELLE,
    MOR,
    FAR,
    MEDMOR,
    PARTNER
}
```

### Foreldreansvar

```kotlin
data class Foreldreansvar(
    val ansvar: String  // e.g., "felles", "mor", "far"
)
```

## Informasjonsbehov

Controls what data is fetched:

```kotlin
enum class Informasjonsbehov {
    INGEN,              // Minimal data
    STANDARD,           // Standard person data
    MED_FAMILIERELASJONER  // Include family members
}
```

## Master Source

```kotlin
enum class Master {
    PDL,    // Person Data Lake (authoritative for PDL-managed data)
    FREG    // Folkeregisteret (National Registry)
}
```

## Address Priority Rules

When determining the postal address (`hentGjeldendePostadresse()`):

1. **Kontaktadresse from PDL** (highest priority)
2. **Kontaktadresse from Freg** (newest by gyldighetstidspunkt)
3. **Oppholdsadresse from PDL**
4. **Oppholdsadresse from Freg**
5. **Bostedsadresse** (lowest priority)

Each address type must:
- Be valid (current date within gyldig period)
- Have valid strukturert or semistrukturert adresse
