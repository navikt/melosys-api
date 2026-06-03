---
name: sed
description: |
  Expert knowledge of SED/EESSI integration in melosys-api.
  Use when: (1) Understanding SED document types (A001, A003, H-series, X008),
  (2) Debugging SED sending/receiving flows and RINA integration,
  (3) Working with BUC (Business Use Case) creation and management,
  (4) Understanding saga steps for SED handling (SEND_VEDTAK_UTLAND, SED_MOTTAK_RUTING),
  (5) Troubleshooting mottakerinstitusjoner (recipient institutions) issues.
---

# SED/EESSI Integration

SED (Structured Electronic Document) is the EU standard for exchanging social security information.
EESSI (Electronic Exchange of Social Security Information) is the system that handles this exchange.
Melosys integrates via melosys-eessi service which communicates with RINA.

## Quick Reference

### Module Structure
```
integrasjon/eessi/
├── EessiClient.kt              # WebClient REST collaborator (calls melosys-eessi)
├── EessiClientConfig.kt        # WebClient/token config
└── dto/                        # Request/response DTOs

service/dokument/sed/
├── EessiService.java           # Main business logic (orchestrates EessiClient)
├── bygger/
│   └── SedDataBygger.java      # Constructs SedDataDto from treatment
├── SedTypeTilBehandlingstemaMapper.java
└── SedDataGrunnlagFactory.java

domain/eessi/
├── SedType.kt                  # 46 SED type definitions
├── BucType.kt                  # BUC types with bestemmelse mapping
└── melding/
    └── MelosysEessiMelding.kt  # Incoming message wrapper

saksflyt/steg/sed/
├── SendVedtakUtland.java       # Send decision abroad
├── SendAnmodningOmUnntak.java  # Send exception request
├── OpprettSedGrunnlag.java     # Map SED to mottatteopplysninger
└── mottak/
    └── SedMottakRuting.java    # Route incoming SEDs
```

### Key SED Types

**A-Series (Applicable Legislation)**:

| SED | Purpose | When Used |
|-----|---------|-----------|
| `A001` | Request for exception (Søknad/anmodning om unntak) | Anmodning om unntak (Art. 16), starts LA_BUC_01 |
| `A003` | Decision on applicable legislation (A1) | Vedtak utland (A1) |
| `A004` | Refusal of A003 decision (Avslag) | Sent in LA_BUC_02 |
| `A009` | Posting notification (Melding om utstasjonering, Art. 12) | LA_BUC_04 |
| `A011` | Approval of exception request (Innvilgelse av søknad om unntak) | Positive response to A001 in LA_BUC_01 |
| `A012` | Change to decision (Endringsmelding) | Update to a sent A003 |

**H-Series (Healthcare)**:

| SED | Purpose |
|-----|---------|
| `H001` | Request for healthcare coverage |
| `H003` | Healthcare entitlement certificate |
| `H020` | Insurance period statement |
| `H121` | Healthcare cost claim |

**X-Series (Administrative)**:

| SED | Purpose |
|-----|---------|
| `X008` | Invalidation/cancellation |
| `X009` | Reminder/follow-up |

### BUC Types (Business Use Cases)

BUC groups related SEDs into a case workflow:

| BUC | Purpose | Common SEDs |
|-----|---------|-------------|
| `LA_BUC_01` | Søknad om unntak (exception request, Art. 16) | A001, A002, A011 |
| `LA_BUC_02` | Beslutning om lovvalg / arbeid i flere land (Art. 13) | A003, A004 |
| `LA_BUC_04` | Melding om utstasjonering (posting notification, Art. 12) | A009 |
| `LA_BUC_05` | Lovvalg etter hovedregel (Art. 11) | A003 |
| `LA_BUC_06` | Forespørsel om mer informasjon | A005, A006 |
| `H_BUC_01` | Healthcare coordination (handled in melosys-eessi) | H001, H003 |
| `UB_BUC_01` | Unilateral benefits | Various |

## Sending SEDs

### Flow Overview
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌──────┐
│ Behandling  │───►│ EessiService│───►│melosys-eessi│───►│ RINA │
│ (vedtak)    │    │ (build SED) │    │ (REST call) │    │      │
└─────────────┘    └─────────────┘    └─────────────┘    └──────┘
```

### Key Operations

**Create BUC and Send SED** (EessiService.java):
```java
// Takes behandlingID + BucType (not a Behandling/SedType).
// The SED type is derived from the behandling's resultat/periode inside the service.
String rinaUrl = eessiService.opprettBucOgSed(
    behandlingID,                 // long
    BucType.LA_BUC_02,            // BucType
    List.of("SE:FK"),            // mottakerInstitusjoner
    dokumentReferanser            // Collection<DokumentReferanse>
);
```

**Send SED on an existing BUC**: EessiService exposes purpose-specific methods
(e.g. `sendGodkjenningArbeidFlereLand`, `sendAnmodningUnntakSvar`,
`sendAvslagUtpekingSvar`). These internally call `eessiClient.sendSedPåEksisterendeBuc(sedDataDto, rinaSaksnummer, sedType)`
— note `sendSedPåEksisterendeBuc` lives on `EessiClient`, not on `EessiService`.
```java
eessiService.sendGodkjenningArbeidFlereLand(behandlingID, ytterligereInformasjon); // sends A012
```

**Get Recipient Institutions** (note: method takes bucType name + a collection of landkoder):
```java
List<Institusjon> institusjoner = eessiService.hentEessiMottakerinstitusjoner(
    BucType.LA_BUC_01.name(),     // String bucType
    Set.of("SE")                  // Collection<String> landkoder
);
```

### SedDataBygger

Constructs the SED payload (`SedDataDto`) from treatment data. The input is a
`SedDataGrunnlag` produced by `SedDataGrunnlagFactory.av(behandling)`:

```java
// SedDataBygger.lag(...) builds the full SedDataDto; lagUtkast(...) builds a draft
SedDataGrunnlag grunnlag = dataGrunnlagFactory.av(behandling);
SedDataDto sedData = sedDataBygger.lag(grunnlag, behandlingsresultat, periodeType);

// Contains:
// - Bruker (person info)
// - Adresser (contact, residence, temporary)
// - Arbeidssted/Virksomhet (workplace/company)
// - Lovvalgsperiode (law choice period)
// - VedtakDto (decision)
```

## Receiving SEDs

### Flow Overview
```
┌──────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ RINA │───►│ Journalpost │───►│ Saga Steps  │───►│ Behandling  │
│      │    │ (EESSI)     │    │ (routing)   │    │ (created)   │
└──────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

### Saga Steps for Receiving

| Step | Purpose |
|------|---------|
| `SED_MOTTAK_RUTING` | Determine handling strategy |
| `OPPRETT_SED_GRUNNLAG` | Map SED to mottatteopplysninger |
| `OPPRETT_SEDDOKUMENT` | Store SED reference |
| `SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH` | Create case if new |
| `SED_MOTTAK_OPPRETT_NY_BEHANDLING` | Create new treatment |
| `BESTEM_BEHANDLINGMÅTE_SED` | Determine next steps |

### SED Type → Treatment Topic Mapping

Only A001/A003/A009/A010 map to a behandlingstema; every other SED type (including
the H-series) returns empty. There is no `Behandlingstema.HELSE` — H-series
healthcare flows are not driven through this mapper in melosys-api.

```java
// SedTypeTilBehandlingstemaMapper.finnBehandlingstemaForSedType(sedType, lovvalgsland)
A001 → Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
A003 → BESLUTNING_LOVVALG_NORGE | BESLUTNING_LOVVALG_ANNET_LAND  // depends on lovvalgsland
A009 → Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
A010 → Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
// all other SED types → Optional.empty()
```

## RINA Integration

### Saksrelasjon (Case Relationship)

Links the archive case (arkivsakID) to a RINA case. The third argument is the
**bucType** (as a String), not a SED type:

```java
eessiService.lagreSaksrelasjon(
    arkivsakID,                   // Long
    rinaSaksnummer,               // String
    BucType.LA_BUC_02.name()      // String bucType
);
```

### BUC Status Checks

```java
// Check if the BUC for an arkivsak is open (takes the long arkivsakID, not a rinaSaksnummer)
boolean erÅpen = eessiService.erBucAapen(arkivsakID);

// Check whether a specific SED type can be created on a BUC (returns boolean)
boolean kanOpprettes = eessiService.kanOppretteSedTyperPåBuc(rinaSaksnummer, SedType.A012);

// Get related BUCs (statuser filter; empty list = all)
List<BucInformasjon> bucer = eessiService.hentTilknyttedeBucer(arkivsakID, List.of());
```

### LåsReferanse for Concurrency

`Prosessinstans.låsReferanse` is a plain `String` (DB column `sed_laas_referanse`).
For SEDs it has the form `rinaSaksnummer_sedID_sedVersjon`. `LåsReferanseFactory.lagLåsReferanse(..)`
parses it into a `SedLåsReferanse`, whose `gruppePrefiks` is the **RINA saksnummer**.
Prosessinstanser that share a gruppePrefiks (same RINA case) are serialised, so
concurrent operations on the same RINA case are queued rather than run in parallel.

```kotlin
val ref = LåsReferanseFactory.lagLåsReferanse(prosessinstans.hentLåsReferanse) // -> SedLåsReferanse
ref.gruppePrefiks // == rinaSaksnummer
```

## Common Issues

### 1. Missing Mottakerinstitusjoner

**Symptom**: `FunksjonellException: "Fant ingen mottakerinstitusjoner for ..."`

**Cause**: Country not EESSI-ready or wrong BUC type

**Investigation**:
```java
List<Institusjon> institusjoner = eessiService.hentEessiMottakerinstitusjoner(bucType.name(), Set.of(landkode));
// Empty list = country not configured / not EESSI-ready
```

### 2. Invalid SED Type for BUC

**Symptom**: Cannot create SED on existing BUC

**Investigation**:
```java
boolean kanOpprettes = eessiService.kanOppretteSedTyperPåBuc(rinaSaksnummer, ønsketSedType);
// false = the desired SED type cannot be created (Create action) on this BUC
```

### 3. GB/EFTA Convention Special Handling

**Symptom**: Wrong BUC type or text for UK cases

**Note**: UK uses EFTA convention after Brexit, requires special mapping:
- Different BUC types
- Special text in SED content

### 4. PDF Generation Failure

**Symptom**: SED created but no PDF

**Cause**: Incomplete data in SedDataDto

**Investigation**: Check SedDataBygger for required fields

## Saga Steps Reference

### Sending Steps

| Step | Description |
|------|-------------|
| `SEND_VEDTAK_UTLAND` | Send A003 decision abroad |
| `SEND_ANMODNING_OM_UNNTAK` | Send A001 exception request |
| `SEND_SVAR_ANMODNING_UNNTAK` | Send A011/A004 response |
| `VIDERESEND_SØKNAD` | Forward application |
| `UTPEKING_SEND_AVSLAG` | Send designation refusal |
| `SEND_GODKJENNING_REGISTRERING_UNNTAK` | Send approval |

### Receiving Steps

| Step | Description |
|------|-------------|
| `SED_MOTTAK_RUTING` | Route incoming SED |
| `OPPRETT_SED_GRUNNLAG` | Create data from SED |
| `OPPRETT_SEDDOKUMENT` | Store SED reference |
| `SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH` | Create new case |
| `BESTEM_BEHANDLINGMÅTE_SED` | Determine automation |

## Debugging

### Find RINA Case for Behandling

The RINA saksnummer is **not** stored as a `saksopplysning` row with
`opplysningstype = 'RINA_SAKSNUMMER'` (that string only exists in test code) and
there is no `seddokument`/`sed_dokument` table. SED data is a SED-opplysning in the
`saksopplysning` table (`opplysning_type = 'SEDOPPL'`), serialized as XML in
`dokument_xml`; the RINA saksnummer lives inside that XML (domain type
`SedDokument`). It is also the prefix of the saga lock reference
(`prosessinstans.sed_laas_referanse`, see below).

```sql
SELECT s.id, s.registrert_dato, s.dokument_xml
FROM saksopplysning s
WHERE s.behandling_id = :behandlingId
AND s.opplysning_type = 'SEDOPPL'
ORDER BY s.registrert_dato DESC;
```

### Check SED Sending History
```sql
-- prosessinstans columns: prosess_type, sist_fullfort_steg, status, registrert_dato
SELECT pi.uuid, pi.prosess_type, pi.sist_fullfort_steg, pi.status
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
AND (pi.prosess_type LIKE '%SED%' OR pi.sist_fullfort_steg LIKE '%SED%')
ORDER BY pi.registrert_dato DESC;
```

### Check Saksrelasjon
```sql
-- Via melosys-eessi service
-- Links GSAK to RINA cases
```

## Detailed Documentation

- **[SED Types](references/sed-types.md)**: Complete SED type reference
- **[BUC Types](references/buc-types.md)**: BUC mapping and workflows
- **[Debugging](references/debugging.md)**: SQL queries, log grep patterns, common issues
