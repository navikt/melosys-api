---
skill: altinn-soknad
description: Altinn A1 application form processing for posted workers (utsendte arbeidstakere)
triggers:
  - altinn søknad
  - altinn skjema
  - elektronisk søknad
  - MedlemskapArbeidEOS
  - SoeknadMapper
  - posted workers application
  - utsendt arbeidstaker søknad
---

# Altinn Søknad Skill

Expert knowledge of Altinn electronic A1 application processing for posted workers (utsendte arbeidstakere) in EU/EEA.

## Quick Reference

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `AltinnSoeknadService` | `service/src/main/java/.../altinn/` | Main service processing Altinn applications |
| `SoeknadMapper` | `service/src/main/java/.../altinn/` | Maps Altinn XML to internal Soeknad domain |
| `SoknadMottakConsumer` | `integrasjon/src/main/kotlin/.../soknadmottak/` | Fetches applications from soknad-mottak API |
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
4. MOTTA_SOKNAD_ALTINN saga starts
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

```java
class Soeknad {
    Soeknadsland soeknadsland;        // Target country/countries
    Periode periode;                   // Application period
    OpplysningerOmBrukeren personOpplysninger;
    ArbeidPaaLand arbeidPaaLand;      // If type=LAND
    List<MaritimtArbeid> maritimtArbeid;  // If OFFSHORE/SKIPSFART
    List<LuftfartBase> luftfartBaser; // If LUFTFART
    LoennOgGodtgjoerelse loennOgGodtgjoerelse;
    List<ForetakUtland> foretakUtland;  // Foreign companies
    JuridiskArbeidsgiverNorge juridiskArbeidsgiverNorge;
    Utenlandsoppdraget utenlandsoppdraget;
    ArbeidssituasjonOgOevrig arbeidssituasjonOgOevrig;
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
-- Find søknad by fagsak
SELECT mo.*, mo.mottatte_opplysninger_data
FROM mottatte_opplysninger mo
JOIN behandling b ON mo.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE f.gsak_saksnr = '123456789'
AND mo.opplysninger_type = 'SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS';

-- Check original XML
SELECT xml_fra_altinn
FROM mottatte_opplysninger
WHERE behandling_id = :behandlingId;
```

### Trace Application Processing

```sql
-- Find prosessinstans for søknad
SELECT pi.*, ps.steg
FROM prosessinstans pi
JOIN prosess_steg ps ON ps.prosessinstans_id = pi.id
WHERE pi.prosesstype = 'MOTTA_SOKNAD_ALTINN'
AND pi.data LIKE '%søknadRef%'
ORDER BY ps.opprettet_tid;
```

## Related Skills

- **saksflyt**: Saga pattern for processing
- **journalforing**: Document archiving
- **fagsak**: Case creation
- **behandling**: Case treatment lifecycle

## External Documentation

- [Altinn Elektronisk Søknad](https://confluence.adeo.no/spaces/TEESSI/pages/340512270) - Original Altinn form design
- [Nav.no Søknadsdialog](https://confluence.adeo.no/spaces/TEESSI/pages/704162290) - New Nav.no form (replacing Altinn)
