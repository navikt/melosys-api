---
name: kodeverk
description: |
  Expert knowledge of Kodeverk (code registry/enums) system in melosys-api.
  Use when: (1) Understanding enums (Sakstyper, Behandlingstyper, Behandlingstema, etc.),
  (2) Validating legal combinations of case/treatment types,
  (3) Debugging "ugyldig kombinasjon" errors,
  (4) Adding new enum values or combinations,
  (5) Mapping between internal codes and external systems (MEDL, Oppgave, SED).
---

# Kodeverk System

Kodeverk manages all enums and validates legal combinations of case types, treatment types,
and statuses. Enums are defined in the external `melosys-internt-kodeverk` library.

## Quick Reference

### Module Structure
```
melosys-internt-kodeverk (external library)
└── no.nav.melosys.domain.kodeverk.*   # All enum definitions

service/lovligekombinasjoner/
├── LovligeKombinasjonerSaksbehandlingService.kt  # Validation service
├── LovligeSakskombinasjoner.java                  # Valid case combinations
├── LovligeBehandlingsKombinasjoner.java           # Valid treatment combinations
└── LovligeBehandlingstatus.kt                     # Valid statuses

service/kodeverk/
└── KodeverkService.kt                # External kodeverk (Landkoder, etc.)
```

### Core Enums

**Case Level:**
| Enum | Values | Description |
|------|--------|-------------|
| `Sakstyper` | EU_EOS, FTRL, TRYGDEAVTALE | Case type |
| `Sakstemaer` | MEDLEMSKAP_LOVVALG, TRYGDEAVGIFT, UNNTAK | Case theme |
| `Saksstatuser` | UNDER_BEHANDLING, HENLAGT, AVSLUTTET, OPPHØRT, ANNULLERT | Case status |

**Treatment Level:**
| Enum | Values | Description |
|------|--------|-------------|
| `Behandlingstyper` | FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE, ÅRSAVREGNING | Treatment type |
| `Behandlingstema` | YRKESAKTIV, PENSJONIST, UTSENDT_ARBEIDSTAKER, ARBEID_FLERE_LAND, ... | Treatment theme |
| `Behandlingsstatus` | UNDER_BEHANDLING, AVVENT_DOK_PART, AVVENT_DOK_UTL, AVVENT_FAGLIG_AVKLARING | Treatment status |

**Actor Level:**
| Enum | Values | Description |
|------|--------|-------------|
| `Aktoersroller` | BRUKER, VIRKSOMHET | Actor role |

## Legal Combinations

### Validation Service

`LovligeKombinasjonerSaksbehandlingService` validates all combinations:

```kotlin
// Get valid options
val sakstyper = service.hentMuligeSakstyper()
val sakstemaer = service.hentMuligeSakstemaer(hovedpart, sakstype, saksnummer)
val behandlingstemaer = service.hentMuligeBehandlingstemaer(...)
val behandlingstyper = service.hentMuligeBehandlingstyper(...)

// Validate
service.validerOpprettelseOgEndring(sakstype, sakstema, behType, behTema)
```

### Combination Structure

```
Sakstype (EU_EOS, FTRL, TRYGDEAVTALE)
└── Sakstema (MEDLEMSKAP_LOVVALG, TRYGDEAVGIFT)
    └── Behandlingstema (YRKESAKTIV, PENSJONIST, ...)
        └── Behandlingstype (FØRSTEGANG, NY_VURDERING, ...)
```

### Actor Role Dependencies

Different combinations for BRUKER vs VIRKSOMHET:

```kotlin
// BRUKER (individual)
EU_EOS + MEDLEMSKAP_LOVVALG → [YRKESAKTIV, PENSJONIST, UTSENDT_*, ...]

// VIRKSOMHET (organization)
EU_EOS + MEDLEMSKAP_LOVVALG → [VIRKSOMHET]
FTRL + TRYGDEAVGIFT → [VIRKSOMHET]
```

## Behandlingstema Reference

### EU/EØS Cases
| Tema | Description |
|------|-------------|
| `YRKESAKTIV` | Employed person |
| `IKKE_YRKESAKTIV` | Non-employed |
| `PENSJONIST` | Pensioner |
| `UTSENDT_ARBEIDSTAKER` | Posted worker (Art. 12) |
| `UTSENDT_SELVSTENDIG` | Posted self-employed |
| `ARBEID_FLERE_LAND` | Multi-state worker (Art. 13) |
| `ARBEID_TJENESTEPERSON_ELLER_FLY` | Civil servant or flight crew |

### Exception Handling (Art. 16)
| Tema | Description |
|------|-------------|
| `ANMODNING_OM_UNNTAK_HOVEDREGEL` | Exception request |
| `REGISTRERING_UNNTAK` | Register exception |
| `REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING` | Posting exception |
| `REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE` | Other exception |

### Decision/Designation
| Tema | Description |
|------|-------------|
| `BESLUTNING_LOVVALG_NORGE` | Norway designated |
| `BESLUTNING_LOVVALG_ANNET_LAND` | Other country designated |

## Behandlingstyper Reference

| Type | Description | When Used |
|------|-------------|-----------|
| `FØRSTEGANG` | First-time treatment | New case |
| `NY_VURDERING` | Re-assessment | Changed circumstances |
| `KLAGE` | Appeal | Dispute decision |
| `HENVENDELSE` | Inquiry | Question/info request |
| `ÅRSAVREGNING` | Annual settlement | Yearly reconciliation |
| `ENDRET_PERIODE` | Changed period | Period modification |
| `MANGLENDE_INNBETALING_TRYGDEAVGIFT` | Missing payment | Payment follow-up |

## Special Rules

### ÅRSAVREGNING Restrictions
```kotlin
// Only allowed for:
// FTRL + YRKESAKTIV (with feature toggle)
// EU_EOS + PENSJONIST (with feature toggle)

// Cannot combine with other behandlingstyper
```

### Second Treatment Rules
After certain themes, only limited types allowed:
```kotlin
val BEHANDLINGSTEMA_FOR_ANNENGANGS = setOf(
    REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
    REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
    BESLUTNING_LOVVALG_NORGE,
    BESLUTNING_LOVVALG_ANNET_LAND,
    ANMODNING_OM_UNNTAK_HOVEDREGEL
)
// → Only NY_VURDERING, KLAGE, HENVENDELSE allowed
```

### Inactive Case Handling
For closed cases, only HENVENDELSE allowed:
```kotlin
val TILLATTE_SAKSSTATUSER_HENVENDELSE = setOf(
    HENLAGT, AVSLUTTET, OPPHØRT, ANNULLERT, HENLAGT_BORTFALT
)
```

## Code Mappings

### SED → Behandlingstema
```kotlin
// SedTypeTilBehandlingstemaMapper
A001 → ANMODNING_OM_UNNTAK_HOVEDREGEL
A003 → BESLUTNING_LOVVALG_NORGE / BESLUTNING_LOVVALG_ANNET_LAND
A009 → REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
A010 → REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
```

### Behandlingstype → Document Code
```kotlin
// BehandlingstypeKodeMapper
FØRSTEGANG → SOEKNAD
KLAGE → KLAGE
NY_VURDERING → NY_VURDERING
ENDRET_PERIODE → ENDRET_PERIODE
```

### Oppgave Codes
```kotlin
// OppgaveBehandlingstype
EOS_LOVVALG_NORGE → "ae0112"
```

## External Kodeverk

`KodeverkService` handles external NAV registries:

```kotlin
// Decode external code
val beskrivelse = kodeverkService.dekod(FellesKodeverk.LANDKODER, "NOR")

// Get valid codes
val landkoder = kodeverkService.hentGyldigeKoderForKodeverk(FellesKodeverk.LANDKODER)
```

### FellesKodeverk Types
| Type | Description |
|------|-------------|
| `LANDKODER` | Country codes |
| `YRKER` | Occupations |
| `SIVILSTANDER` | Marital status |
| `KJØNNSTYPER` | Gender types |
| `POSTNUMMER` | Postal codes |
| `SKIPSTYPER` | Ship types |

## Feature Toggles

Kodeverk availability controlled by toggles:
| Toggle | Controls |
|--------|----------|
| `MELOSYS_ÅRSAVREGNING` | Årsavregning availability |
| `MELOSYS_ÅRSAVREGNING_UTEN_FLYT` | FTRL årsavregning |
| `MELOSYS_ÅRSAVREGNING_EØS_PENSJONIST` | EØS pensjonist årsavregning |
| `BEHANDLINGSTYPE_KLAGE` | Klage availability |

## Common Issues

### Invalid Combination Error

**Symptom**: `FunksjonellException: "Ugyldig kombinasjon av ..."`

**Investigation**:
```kotlin
// Check valid combinations
val gyldige = service.hentMuligeBehandlingstemaer(sakstype, sakstema, ...)
log.info("Valid temaer: $gyldige")
```

### Missing Enum Value

**Symptom**: Treatment option not available

**Check**:
1. Is enum in `melosys-internt-kodeverk`?
2. Is combination in `LovligeBehandlingsKombinasjoner`?
3. Is feature toggle enabled?

## Detailed Documentation

- **[Enums](references/enums.md)**: Complete enum reference
- **[Combinations](references/combinations.md)**: Legal combination tables
- **[Debugging](references/debugging.md)**: Investigation steps
