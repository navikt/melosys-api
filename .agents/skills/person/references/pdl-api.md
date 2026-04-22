# PDL API Reference

## Overview

PDL (Person Data Lake) is NAV's master data source for person information.
Melosys accesses PDL via GraphQL using Azure AD authentication.

## Authentication

```java
// PDLAuthFilterAzure adds Azure AD token to requests
public class PDLAuthFilterAzure implements ExchangeFilterFunction {
    // Exchanges token for PDL-scoped token
    // Adds "Authorization: Bearer <token>" header
}
```

## GraphQL Queries

### HENT_IDENTER_QUERY

Fetches all identities for a person:

```graphql
query($ident: ID!) {
  hentIdenter(ident: $ident, grupper: [AKTORID, FOLKEREGISTERIDENT, NPID], historikk: false) {
    identer {
      ident
      historisk
      gruppe
    }
  }
}
```

### HENT_PERSON_QUERY

Main query for person data (current state):

```graphql
query($ident: ID!) {
  hentPerson(ident: $ident) {
    adressebeskyttelse { gradering, metadata { master } }
    bostedsadresse {
      gyldigFraOgMed, gyldigTilOgMed, coAdressenavn
      vegadresse { adressenavn, husnummer, husbokstav, postnummer }
      utenlandskAdresse { adressenavnNummer, postkode, bySted, landkode }
      metadata { master, historisk }
    }
    doedsfall { doedsdato }
    foedested { foedeland, foedested }
    foedselsdato { foedselsdato, foedselsaar }
    folkeregisteridentifikator { identifikasjonsnummer }
    forelderBarnRelasjon { relatertPersonsIdent, relatertPersonsRolle, minRolleForPerson }
    kjoenn { kjoenn }
    kontaktadresse {
      gyldigFraOgMed, gyldigTilOgMed, coAdressenavn
      vegadresse { ... }
      utenlandskAdresse { ... }
      postboksadresse { postboks, postnummer }
    }
    navn { fornavn, mellomnavn, etternavn }
    oppholdsadresse { ... }
    sivilstand { type, relatertVedSivilstand, gyldigFraOgMed }
    statsborgerskap { land, gyldigFraOgMed, gyldigTilOgMed }
  }
}
```

### HENT_PERSON_HISTORIKK_QUERY

Fetches person with historical data:

```graphql
query($ident: ID!, $historikk: Boolean!) {
  hentPerson(ident: $ident) {
    bostedsadresse(historikk: $historikk) { ... }
    kontaktadresse(historikk: $historikk) { ... }
    oppholdsadresse(historikk: $historikk) { ... }
    statsborgerskap(historikk: $historikk) { ... }
    sivilstand(historikk: $historikk) { ... }
    # ... other fields
  }
}
```

### HENT_FAMILIERELASJONER_QUERY

Fetches family relations for a person:

```graphql
query($ident: ID!, $historikk: Boolean!) {
  hentPerson(ident: $ident) {
    foedested { foedeland, foedested }
    foedselsdato { foedselsdato, foedselsaar }
    folkeregisteridentifikator { identifikasjonsnummer }
    forelderBarnRelasjon {
      relatertPersonsIdent
      relatertPersonsRolle
      minRolleForPerson
    }
    sivilstand(historikk: $historikk) {
      type
      relatertVedSivilstand
      gyldigFraOgMed
    }
  }
}
```

### Other Specialized Queries

| Query | Purpose |
|-------|---------|
| `HENT_BARN_QUERY` | Child details with foreldreansvar |
| `HENT_FORELDER_QUERY` | Parent basic info |
| `HENT_EKTEFELLE_ELLER_PARTNER_QUERY` | Spouse/partner details |
| `HENT_NAVN_QUERY` | Name only |
| `HENT_STATSBORGERSKAP_QUERY` | Citizenship with history |
| `HENT_ADRESSEBESKYTTELSE_QUERY` | Protection status |

## PDL DTOs

### Ident

```java
public record Ident(String ident, IdentGruppe gruppe) {
    public boolean erAktørID() { return gruppe == AKTORID; }
    public boolean erFolkeregisterIdent() { return gruppe == FOLKEREGISTERIDENT; }
}

public enum IdentGruppe {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID
}
```

### Person

```java
public record Person(
    Collection<Adressebeskyttelse> adressebeskyttelse,
    Collection<Bostedsadresse> bostedsadresse,
    Collection<Doedsfall> doedsfall,
    Collection<Foedested> foedested,
    Collection<Foedselsdato> foedselsdato,
    Collection<Folkeregisteridentifikator> folkeregisteridentifikator,
    Collection<Folkeregisterpersonstatus> folkeregisterpersonstatus,
    Collection<ForelderBarnRelasjon> forelderBarnRelasjon,
    Collection<Foreldreansvar> foreldreansvar,
    Collection<Kjoenn> kjoenn,
    Collection<Kontaktadresse> kontaktadresse,
    Collection<Navn> navn,
    Collection<Oppholdsadresse> oppholdsadresse,
    Collection<Sivilstand> sivilstand,
    Collection<Statsborgerskap> statsborgerskap,
    Collection<UtenlandskIdentifikasjonsnummer> utenlandskIdentifikasjonsnummer
)
```

### Navn

```java
public record Navn(
    String fornavn,
    String mellomnavn,
    String etternavn,
    Metadata metadata
) implements HarMetadata
```

### Statsborgerskap

```java
public record Statsborgerskap(
    String land,              // ISO 3166-1 alpha-3 (e.g., "NOR")
    LocalDate bekreftelsesdato,
    LocalDate gyldigFraOgMed,
    LocalDate gyldigTilOgMed,
    Metadata metadata
) implements HarMetadata
```

### Sivilstand

```java
public record Sivilstand(
    Sivilstandstype type,
    String relatertVedSivilstand,  // Ident of spouse/partner
    LocalDate gyldigFraOgMed,
    LocalDate bekreftelsesdato,
    Metadata metadata
) implements HarMetadata {
    public boolean erGyldigForEktefelleEllerPartner() {
        // Returns true if type is GIFT, SEPARERT, SEPARERT_PARTNER, or REGISTRERT_PARTNER
        // and relatertVedSivilstand is not null
    }
}

public enum Sivilstandstype {
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

### ForelderBarnRelasjon

```java
public record ForelderBarnRelasjon(
    String relatertPersonsIdent,
    Familierelasjonsrolle relatertPersonsRolle,  // MOR, FAR, MEDMOR, BARN
    Familierelasjonsrolle minRolleForPerson,
    Metadata metadata
) implements HarMetadata {
    public boolean erBarn() { return relatertPersonsRolle == BARN; }
    public boolean erForelder() {
        return relatertPersonsRolle == MOR || relatertPersonsRolle == FAR || relatertPersonsRolle == MEDMOR;
    }
}
```

### Adressebeskyttelse

```java
public record Adressebeskyttelse(
    AdressebeskyttelseGradering gradering,
    Metadata metadata
) implements HarMetadata {
    public boolean erStrengtFortrolig() {
        return gradering == STRENGT_FORTROLIG || gradering == STRENGT_FORTROLIG_UTLAND;
    }
}

public enum AdressebeskyttelseGradering {
    FORTROLIG,           // Code 7 (protected address)
    STRENGT_FORTROLIG,   // Code 6 (strictly protected)
    STRENGT_FORTROLIG_UTLAND,
    UGRADERT
}
```

## Address DTOs

### Bostedsadresse

```java
public record Bostedsadresse(
    LocalDateTime gyldigFraOgMed,
    LocalDateTime gyldigTilOgMed,
    String coAdressenavn,
    Vegadresse vegadresse,
    Matrikkeladresse matrikkeladresse,
    UtenlandskAdresse utenlandskAdresse,
    UkjentBosted ukjentBosted,
    Metadata metadata
) implements HarMetadata
```

### Vegadresse (Street Address)

```java
public record Vegadresse(
    String adressenavn,
    String husnummer,
    String husbokstav,
    String tilleggsnavn,
    String postnummer
)
```

### UtenlandskAdresse (Foreign Address)

```java
public record UtenlandskAdresse(
    String adressenavnNummer,
    String bygningEtasjeLeilighet,
    String postboksNummerNavn,
    String postkode,
    String bySted,
    String regionDistriktOmraade,
    String landkode  // ISO 3166-1 alpha-3
)
```

### Kontaktadresse

```java
public record Kontaktadresse(
    LocalDateTime gyldigFraOgMed,
    LocalDateTime gyldigTilOgMed,
    String coAdressenavn,
    KontaktadresseType type,
    Vegadresse vegadresse,
    Postboksadresse postboksadresse,
    PostadresseIFrittFormat postadresseIFrittFormat,
    UtenlandskAdresse utenlandskAdresse,
    UtenlandskAdresseIFrittFormat utenlandskAdresseIFrittFormat,
    Metadata metadata
) implements HarMetadata
```

## Metadata

```java
public record Metadata(
    String master,        // "PDL", "FREG"
    Boolean historisk,
    Collection<Endring> endringer
) {
    public LocalDateTime datoSistRegistrert() {
        // Returns most recent endring.registrert
    }
}

public record Endring(
    Endringstype type,        // OPPRETT, KORRIGER, ANNULLER
    LocalDateTime registrert,
    String kilde
)
```

## Error Handling

### PDL Error Codes

| Code | Description |
|------|-------------|
| `not_found` | Person not found in PDL |
| `bad_request` | Invalid query or parameters |
| `unauthorized` | Token issues |

### Error Handling in PDLConsumerImpl

```java
private void håndterFeil(GraphQLResponse<?> response) {
    if (response == null) {
        throw new IntegrasjonException("Respons fra PDL er null!");
    }
    if (!CollectionUtils.isEmpty(response.errors())) {
        if (response.errors().stream().anyMatch(NOT_FOUND)) {
            throw new IkkeFunnetException("Fant ikke ident i PDL.");
        }
        throw new IntegrasjonException("Kall mot PDL feilet: " + ...);
    }
}
```
