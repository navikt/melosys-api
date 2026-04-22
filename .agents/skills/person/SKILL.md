---
name: person
description: |
  Expert knowledge of Person data integration (PDL) in melosys-api.
  Use when: (1) Working with person data from PDL (Person Data Lake),
  (2) Understanding identity types (fnr, aktørID, NPID),
  (3) Working with addresses, family relations, or citizenship,
  (4) Debugging person data lookup issues,
  (5) Understanding the mapping between PDL and domain models.
---

# Person (PDL Integration) System

Melosys retrieves person data from PDL (Person Data Lake) via GraphQL. The system handles
identity lookup, personal information, addresses, family relations, and citizenship data.
Person data is used throughout the application for case processing and letter generation.

## Quick Reference

### Module Structure

```
integrasjon/pdl/
├── PDLConsumer.java              # Main interface for PDL operations
├── PDLConsumerImpl.java          # Implementation with WebClient
├── PDLConsumerProducer.java      # Bean configuration
├── PDLAuthFilterAzure.java       # Azure AD authentication
├── PDLFeilkode.java              # Error codes
└── dto/
    ├── identer/                  # Identity DTOs
    │   ├── Ident.java
    │   ├── IdentGruppe.java
    │   ├── Identliste.java
    │   └── Query.java
    └── person/                   # Person DTOs
        ├── Person.java           # Main person record
        ├── Navn.java
        ├── Statsborgerskap.java
        ├── Sivilstand.java
        ├── ForelderBarnRelasjon.java
        ├── Adressebeskyttelse.java
        ├── Query.java            # GraphQL queries
        └── adresse/              # Address types
            ├── Bostedsadresse.java
            ├── Kontaktadresse.java
            ├── Oppholdsadresse.java
            ├── Vegadresse.java
            └── UtenlandskAdresse.java

service/persondata/
├── PersondataService.java        # Main service facade
├── PersondataFasade.java         # Interface
└── mapping/                      # PDL → domain mappers
    ├── PersonopplysningerOversetter.java
    ├── PersonMedHistorikkOversetter.java
    ├── NavnOversetter.java
    ├── StatsborgerskapOversetter.java
    └── adresse/
        ├── BostedsadresseOversetter.java
        ├── KontaktadresseOversetter.java
        └── OppholdsadresseOversetter.java

service/persondata/familie/
├── FamiliemedlemService.java     # Family relations
└── medlem/
    └── EktefelleEllerPartnerFamiliemedlemFilter.java
```

### Key Services

| Service | Purpose |
|---------|---------|
| `PDLConsumer` | GraphQL client for PDL queries |
| `PersondataService` | Main facade for person data operations |
| `FamiliemedlemService` | Fetches and filters family members |

### Identity Types

| Type | Description | Example |
|------|-------------|---------|
| `FOLKEREGISTERIDENT` | Norwegian national ID (fnr/dnr) | 12345678901 |
| `AKTORID` | Internal NAV actor ID | 1000012345678 |
| `NPID` | NAV person ID (for non-registered) | 01234567890 |

## PDL Consumer Operations

### Main Operations

```kotlin
// Fetch identities (fnr ↔ aktørID conversion)
val identliste = pdlConsumer.hentIdenter(ident)

// Fetch person data
val person = pdlConsumer.hentPerson(ident)

// Fetch person with address/citizenship history
val personMedHistorikk = pdlConsumer.hentPersonMedHistorikk(ident)

// Fetch family relations
val familierelasjoner = pdlConsumer.hentFamilierelasjoner(ident)

// Fetch specific data
val navn = pdlConsumer.hentNavn(ident)
val statsborgerskap = pdlConsumer.hentStatsborgerskap(ident)
val adressebeskyttelser = pdlConsumer.hentAdressebeskyttelser(ident)
```

### PersondataService Operations

```kotlin
// Get aktørID from fnr (or vice versa)
val aktørId = persondataService.hentAktørIdForIdent(fnr)
val fnr = persondataService.hentFolkeregisterident(aktørId)

// Get person data
val persondata = persondataService.hentPerson(ident)
val persondataMedFamilie = persondataService.hentPerson(ident, Informasjonsbehov.MED_FAMILIERELASJONER)

// Get person with history (for saksbehandler view)
val personMedHistorikk = persondataService.hentPersonMedHistorikk(behandlingID)

// Get family members
val familiemedlemmer = persondataService.hentFamiliemedlemmerFraBehandlingID(behandlingID)

// Check address protection
val erBeskyttet = persondataService.harStrengtFortroligAdresse(ident)
```

## Person Data Model

### PDL Person Record

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

### Address Types

| Type | Description | Priority |
|------|-------------|----------|
| `Kontaktadresse` | Mailing address (PDL/Freg) | 1 (highest) |
| `Oppholdsadresse` | Residence address | 2 |
| `Bostedsadresse` | Registered home address | 3 (lowest) |

### Address Protection (Adressebeskyttelse)

| Gradering | Description |
|-----------|-------------|
| `UGRADERT` | No protection |
| `FORTROLIG` | Protected (code 7) |
| `STRENGT_FORTROLIG` | Strictly protected (code 6) |
| `STRENGT_FORTROLIG_UTLAND` | Strictly protected abroad |

## Family Relations

### Sivilstandstype

| Type | Description |
|------|-------------|
| `GIFT` | Married |
| `REGISTRERT_PARTNER` | Registered partner |
| `SEPARERT` | Separated (married) |
| `SEPARERT_PARTNER` | Separated (partner) |
| `SKILT` | Divorced |
| `SKILT_PARTNER` | Dissolved partnership |
| `ENKE_ELLER_ENKEMANN` | Widow/widower |
| `UGIFT` | Unmarried |

### Familierelasjonsrolle

| Role | Description |
|------|-------------|
| `MOR` | Mother |
| `FAR` | Father |
| `MEDMOR` | Co-mother |
| `BARN` | Child |

## Data Mapping Flow

```
PDL GraphQL Response
    ↓
integrasjon/pdl/dto/person/Person.java (PDL DTOs)
    ↓
service/persondata/mapping/*Oversetter.java (Mappers)
    ↓
domain/person/Personopplysninger.kt (Domain model)
    ↓
Frontend API / Business logic
```

## Caching

PersondataService uses caching for identity lookups:

```java
@Cacheable("aktoerID")
public String hentAktørIdForIdent(String ident) { ... }

@Cacheable("folkeregisterIdent")
public Optional<String> finnFolkeregisterident(String ident) { ... }
```

## Common Issues

### Issue: Person Not Found

**Symptom**: `IkkeFunnetException: "Fant ikke ident i PDL."`

**Causes**:
- Invalid/non-existent identity
- Identity not in PDL (foreigner without registration)
- Wrong identity type used

**Check**:
```kotlin
// Try both identity types
val identer = pdlConsumer.hentIdenter(ident)
log.info("Available identities: ${identer.identer()}")
```

### Issue: Address Protection Blocking Access

**Symptom**: Empty address data or access denied

**Check**:
```kotlin
val adressebeskyttelser = pdlConsumer.hentAdressebeskyttelser(ident)
log.info("Protection levels: ${adressebeskyttelser.map { it.gradering() }}")
```

### Issue: Missing Family Members

**Symptom**: Family relations not appearing

**Causes**:
- Person is 18+ (parents not fetched)
- Sivilstand doesn't qualify for spouse lookup
- No valid `relatertPersonsIdent`

## Key Code Locations

| Component | Location |
|-----------|----------|
| PDL Consumer | `integrasjon/.../pdl/PDLConsumer.java` |
| PDL DTOs | `integrasjon/.../pdl/dto/` |
| GraphQL Queries | `integrasjon/.../pdl/dto/person/Query.java` |
| PersondataService | `service/.../persondata/PersondataService.java` |
| Mappers | `service/.../persondata/mapping/` |
| Domain Models | `domain/.../person/` |
| Family Service | `service/.../persondata/familie/FamiliemedlemService.java` |

## Detailed Documentation

- **[PDL API](references/pdl-api.md)**: GraphQL queries and DTOs
- **[Data Models](references/data-models.md)**: Domain model reference
- **[Debugging](references/debugging.md)**: Troubleshooting guide
