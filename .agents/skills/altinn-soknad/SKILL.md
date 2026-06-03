---
name: altinn-soknad
description: |
  Expert knowledge of Altinn A1 application processing for posted workers in melosys-api.
  Use when: (1) Understanding Altinn søknad/skjema processing,
  (2) Debugging MedlemskapArbeidEOS form mapping,
  (3) Understanding SoeknadMapper transformations,
  (4) Investigating posted worker application issues,
  (5) Understanding the MOTTAK_SOKNAD_ALTINN saga flow.
---

# Altinn Søknad Skill

Expert knowledge of Altinn electronic A1 application processing for posted workers (utsendte arbeidstakere) in EU/EEA.

## Quick Reference

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `AltinnSoeknadService` | `service/src/main/java/.../altinn/` | Main service processing Altinn applications |
| `SoeknadMapper` | `service/src/main/java/.../altinn/` | Maps Altinn XML to internal Soeknad domain |
| `SoknadMottattConsumer` | `service/src/main/java/.../soknad/` | Kafka entry point; consumes soknad-mottak topic and starts the saga |
| `SoknadMottakClient` | `integrasjon/src/main/kotlin/.../soknadmottak/` | Fetches applications from soknad-mottak API |
| `soknad-altinn` module | `soknad-altinn/` | XSD schema and JAXB bindings for form |
| `OpprettFagsakOgBehandlingFraAltinnSøknad` | `saksflyt/src/.../steg/behandling/` | Saga step for processing |

### Form Structure (MedlemskapArbeidEOS_M)

```
Innhold
├── arbeidsgiver          # Employer information
│   ├── virksomhetsnummer # Org number
│   ├── kontaktperson     # Contact person
│   ├── samletVirksomhetINorge  # Norwegian operations stats
│   └── offentligVirksomhet     # Is public sector?
├── arbeidstaker          # Employee information
│   ├── foedselsnummer    # Norwegian ID
│   ├── utenlandskIDnummer
│   ├── barn              # Children (medfølgende)
│   └── fødested/fødeland
├── midlertidigUtsendt    # Posted worker details
│   ├── arbeidsland       # Work country
│   ├── arbeidssted       # Work location (LAND/OFFSHORE/SKIPSFART/LUFTFART)
│   ├── utenlandsoppdraget # Assignment details
│   └── loennOgGodtgjoerelse # Salary info
└── fullmakt              # Power of attorney
    ├── fullmektigVirksomhetsnummer  # Advisory firm org number
    └── fullmaktFraArbeidstaker
```

### Processing Flow

```
1. Altinn form submitted
   ↓
2. melosys-skjema-api receives and stores in PostgreSQL
   ↓
3. Kafka event triggers melosys-api
   ↓
4. MOTTAK_SOKNAD_ALTINN saga starts
   ↓
5. OpprettFagsakOgBehandlingFraAltinnSøknad step:
   - Fetches XML from soknad-mottak API
   - Creates Fagsak and Behandling
   - Maps form data to MottatteOpplysninger
   ↓
6. OpprettOgFerdigstillAltinnJournalpost step:
   - Archives documents in Joark
   ↓
7. Behandling ready for saksbehandler
```

### Behandlingstema Determination

```kotlin
if (offentligVirksomhet == true) {
    Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
} else {
    Behandlingstema.UTSENDT_ARBEIDSTAKER
}
```

### ArbeidsstedType Mapping

| Type | Description | Mapped To |
|------|-------------|-----------|
| `LAND` | Work on land | `ArbeidPaaLand` with fysiske arbeidssteder |
| `OFFSHORE` | Offshore platforms | `MaritimtArbeid` with innretningstype |
| `SKIPSFART` | Ships/maritime | `MaritimtArbeid` with flaggland |
| `LUFTFART` | Aviation | `LuftfartBaser` with hjemmebase |

## Domain Model

### Soeknad (Internal Representation)

`Soeknad` (`domain/.../mottatteopplysninger/Soeknad.kt`, Kotlin) is a thin subclass of
`MottatteOpplysningerData`. It declares only four fields of its own:

```kotlin
class Soeknad : MottatteOpplysningerData() {
    var loennOgGodtgjoerelse: LoennOgGodtgjoerelse? = LoennOgGodtgjoerelse()
    var arbeidsgiversBekreftelse = ArbeidsgiversBekreftelse()
    var utenlandsoppdraget = Utenlandsoppdraget()
    var arbeidssituasjonOgOevrig = ArbeidssituasjonOgOevrig()
}
```

The remaining data lives on the inherited `MottatteOpplysningerData`
(`domain/.../mottatteopplysninger/MottatteOpplysningerData.java`):

```java
class MottatteOpplysningerData {
    Soeknadsland soeknadsland;             // Target country/countries
    Periode periode;                        // Application period
    OpplysningerOmBrukeren personOpplysninger;
    ArbeidPaaLand arbeidPaaLand;           // If type=LAND
    List<ForetakUtland> foretakUtland;      // Foreign companies
    OppholdUtland oppholdUtland;
    SelvstendigArbeid selvstendigArbeid;
    JuridiskArbeidsgiverNorge juridiskArbeidsgiverNorge;
    List<MaritimtArbeid> maritimtArbeid;    // If OFFSHORE/SKIPSFART
    List<LuftfartBase> luftfartBaser;       // If LUFTFART
    Bosted bosted;
}
```

### Key Mappings

| Altinn Field | Domain Field | Notes |
|--------------|--------------|-------|
| `arbeidsland` | `Soeknadsland.landkoder` | ISO-2 country code |
| `periodeUtland` | `Periode` | fom/tom LocalDate |
| `loennNorskArbg` | `LoennOgGodtgjoerelse.norskBruttoLoennPerMnd` | Salary in NOK |
| `erstatterTidligereUtsendte` | `Utenlandsoppdraget.erstatter` | Replacement worker |

## Common Debugging

### Check Received Application

```sql
-- Find søknad by fagsak (behandling joins fagsak via saksnummer, not fagsak_id)
SELECT mo.id, mo.type, mo.data
FROM mottatteopplysninger mo
JOIN behandling b ON mo.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE f.gsak_saksnummer = '123456789'
AND mo.type = 'SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS';

-- Check original XML (stored in original_data; JSON is in data)
SELECT original_data
FROM mottatteopplysninger
WHERE behandling_id = :behandlingId;
```

### Trace Application Processing

```sql
-- Find prosessinstans for søknad.
-- prosess_steg is a kode/navn lookup table, so don't join it - prosessinstans
-- carries data and sist_fullfort_steg (last completed step) directly.
SELECT uuid, prosess_type, sist_fullfort_steg, data, registrert_dato, endret_dato
FROM prosessinstans
WHERE prosess_type = 'MOTTAK_SOKNAD_ALTINN'
AND data LIKE '%søknadRef%'
ORDER BY endret_dato DESC;
```

## Detailed references

- [references/form-structure.md](references/form-structure.md) - Full MedlemskapArbeidEOS_M form structure and field reference.
- [references/mapping.md](references/mapping.md) - SoeknadMapper Altinn-to-domain field mappings, including the salary-bug handling.
- [references/debugging.md](references/debugging.md) - Common issues, SQL queries, Kafka topics, and soknad-mottak integration points.

## Related Skills

- **saksflyt**: Saga pattern for processing
- **journalforing**: Document archiving
- **fagsak**: Case creation
- **behandling**: Case treatment lifecycle

## External Documentation

- [Altinn Elektronisk Søknad](https://confluence.adeo.no/spaces/TEESSI/pages/340512270) - Original Altinn form design
- [Nav.no Søknadsdialog](https://confluence.adeo.no/spaces/TEESSI/pages/704162290) - New Nav.no form (replacing Altinn)
