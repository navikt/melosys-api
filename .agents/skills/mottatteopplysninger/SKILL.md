---
name: mottatteopplysninger
description: |
  Received application data (MottatteOpplysninger) patterns for A1 applications, SED-based cases, and exception requests. Use when working with application forms, person/employer data, work locations, or modifying MottatteOpplysningerData subclasses. Triggers: "mottatteopplysninger", "søknad data", "application form", "MottatteOpplysningerData".
---

# MottatteOpplysninger Domain

MottatteOpplysninger stores received application data for a Behandling. Data is persisted as JSON and deserialized to type-specific subclasses.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    MottatteOpplysninger (Entity)                     │
│  - id, behandling, type, versjon, jsonData, originalData            │
│  - mottatteOpplysningerData (Transient - deserialized from JSON)    │
└──────────────────────────────────┬──────────────────────────────────┘
                                   │
            ┌──────────────────────┴──────────────────────┐
            │      MottatteOpplysningerKonverterer        │
            │  - Maps type → class for deserialization    │
            │  - Triggered by MottatteOpplysningerListener│
            └──────────────────────┬──────────────────────┘
                                   │
┌──────────────────────────────────┴──────────────────────────────────┐
│                   MottatteOpplysningerData (Base)                    │
│  - soeknadsland, periode, personOpplysninger                        │
│  - arbeidPaaLand, foretakUtland, maritimtArbeid, luftfartBaser      │
│  - juridiskArbeidsgiverNorge, selvstendigArbeid, bosted, oppholdUtl │
└─────────────────────────────────────────────────────────────────────┘
        │               │               │               │
        ▼               ▼               ▼               ▼
   ┌─────────┐    ┌───────────┐  ┌─────────────┐  ┌──────────────┐
   │ Soeknad │    │SedGrunnlag│  │Anmodning-   │  │SøknadNorge-  │
   │  (EØS)  │    │  (SED)    │  │EllerAttest  │  │EllerUtenforEØS│
   └─────────┘    └───────────┘  └─────────────┘  └──────────────┘
```

## Type Mapping

| Mottatteopplysningertyper | Class | Use Case |
|---------------------------|-------|----------|
| `SØKNAD_A1_YRKESAKTIVE_EØS` | `Soeknad` | A1 for employed in EEA |
| `SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS` | `Soeknad` | A1 for posted workers |
| `SØKNAD_YRKESAKTIVE_NORGE_ELLER_UTENFOR_EØS` | `SøknadNorgeEllerUtenforEØS` | FTRL/bilateral |
| `SØKNAD_IKKE_YRKESAKTIV` | `SøknadIkkeYrkesaktiv` | Non-occupationally active |
| `SED` | `SedGrunnlag` | From SED documents |
| `ANMODNING_ELLER_ATTEST` | `AnmodningEllerAttest` | Exception requests |

## Key Files

| File | Purpose |
|------|---------|
| `domain/.../MottatteOpplysninger.java` | JPA entity |
| `domain/.../MottatteOpplysningerData.java` | Base data class |
| `domain/.../MottatteOpplysningerKonverterer.java` | JSON ↔ Object conversion |
| `domain/.../jpa/MottatteOpplysningerListener.java` | JPA lifecycle hooks |
| `service/.../MottatteOpplysningerService.kt` | Business logic |
| `frontend-api/.../MottatteOpplysningerController.java` | REST endpoints |

## Data Structure

### Base Fields (MottatteOpplysningerData)
```kotlin
soeknadsland: Soeknadsland          // Country of application
periode: Periode                     // Application period (fom/tom)
personOpplysninger: OpplysningerOmBrukeren  // Person info, family
arbeidPaaLand: ArbeidPaaLand        // Physical work locations
foretakUtland: List<ForetakUtland>  // Foreign employers
maritimtArbeid: List<MaritimtArbeid> // Maritime work
luftfartBaser: List<LuftfartBase>   // Aviation bases
juridiskArbeidsgiverNorge: JuridiskArbeidsgiverNorge
selvstendigArbeid: SelvstendigArbeid // Self-employment
bosted: Bosted                       // Residence
oppholdUtland: OppholdUtland        // Foreign stays
```

### Soeknad Extensions
```kotlin
loennOgGodtgjoerelse: LoennOgGodtgjoerelse  // Salary info
arbeidsgiversBekreftelse: ArbeidsgiversBekreftelse
utenlandsoppdraget: Utenlandsoppdraget
arbeidssituasjonOgOevrig: ArbeidssituasjonOgOevrig
```

### SedGrunnlag Extensions
```kotlin
overgangsregelbestemmelser: List<Overgangsregelbestemmelser>
ytterligereInformasjon: String?
```

### AnmodningEllerAttest Extensions
```kotlin
avsenderland: Land_iso2?   // Sending country
lovvalgsland: Land_iso2?   // Applicable legislation country
```

## Common Patterns

### Creating MottatteOpplysninger
```kotlin
// Via service (recommended)
mottatteOpplysningerService.opprettSøknadEllerAnmodningEllerAttest(
    behandling, periode, soeknadsland
)

// For SED-based
mottatteOpplysningerService.opprettSedGrunnlag(behandlingId, sedGrunnlag)

// For Altinn søknad
mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(
    behandlingID, originalData, soeknad, eksternReferanseID
)
```

### Accessing Data
```kotlin
val mottatteOpplysninger = mottatteOpplysningerService.hentMottatteOpplysninger(behandlingId)
val data = mottatteOpplysninger.mottatteOpplysningerData

// Type-safe casting
when (data) {
    is Soeknad -> data.loennOgGodtgjoerelse
    is SedGrunnlag -> data.overgangsregelbestemmelser
    is AnmodningEllerAttest -> data.avsenderland
}
```

### Updating Data
```kotlin
// Update via JSON (from frontend)
mottatteOpplysningerService.oppdaterMottatteOpplysninger(behandlingId, jsonNode)

// Update via object
val mo = hentMottatteOpplysninger(behandlingId)
mo.mottatteOpplysningerData.periode = newPeriode
mottatteOpplysningerService.oppdaterMottatteOpplysninger(mo)
```

## Serialization Notes

- Uses Jackson with `KotlinModule` and `JavaTimeModule`
- `@JsonIgnoreProperties(ignoreUnknown = true)` on base class
- Converter uses `EnumMap<Mottatteopplysningertyper, Class>` for type mapping
- JPA listener auto-converts on load/persist

## Helper Methods

```java
// On MottatteOpplysningerData
hentAlleOrganisasjonsnumre()           // All org numbers
hentUtenlandskeArbeidsstederLandkode() // Foreign work location countries
hentUtenlandskeArbeidsgivereUuid()     // Foreign employer UUIDs
hentFnrMedfolgendeBarn()               // Accompanying children FNR
hentMedfølgendeBarn()                  // Map<UUID, MedfolgendeFamilie>
hentMedfølgendeEktefelle()             // Map<UUID, MedfolgendeFamilie>
```

## Testing

Test factory: `domain/src/test/kotlin/.../MottatteOpplysningerTestFactory.kt`

```kotlin
// Create test instance
val soeknad = Soeknad().apply {
    periode = Periode().apply {
        fom = LocalDate.now()
        tom = LocalDate.now().plusMonths(12)
    }
    personOpplysninger.fnr = "12345678901"
}
```
