# OppgaveGosysMapping Reference

The `OppgaveGosysMapping` class contains the complex mapping table that determines task properties
based on behandling context.

## Mapping Source

Confluence documentation: "Oppgaver i Gosys"
(https://confluence.adeo.no/spaces/TEESSI/pages/478253092). The `rows` list is generated
from this table by `OppgaveGosysMappingCodeGenerator`.

## Mapping Inputs

| Input | Type | Description |
|-------|------|-------------|
| Sakstype | `Sakstyper` | EU_EOS, TRYGDEAVTALE, FTRL |
| Sakstema | `Sakstemaer` | MEDLEMSKAP_LOVVALG, TRYGDEAVGIFT, UNNTAK |
| Behandlingstema | `Behandlingstema` | UTSENDT_ARBEIDSTAKER, ARBEID_FLERE_LAND, PENSJONIST, YRKESAKTIV, … |
| Behandlingstype | `Behandlingstyper` | FØRSTEGANG, NY_VURDERING, KLAGE, ÅRSAVREGNING, HENVENDELSE, … |

## Mapping Outputs (`OppgaveGosysMapping.Oppgave`)

| Output | Field | Description |
|--------|-------|-------------|
| Oppgavetype | `oppgaveType` | BEH_SAK_MK, BEH_SED, VURD_HENV, VURD_MAN_INNB, BEH_ARSAVREG |
| Tema | `tema` | MED, UFM, TRY |
| Behandlingstema (ab-code) | `oppgaveBehandlingstema` | `OppgaveBehandlingstema` enum, e.g. EU_EOS_YRKESAKTIV = "ab0483" |
| Beskrivelsefelt | `beskrivelsefelt` | TOMT, SED, SED_ELLER_TOMT, BEHANDLINGSTEMA, … |

Note: prioritet is set on the `Oppgave` domain entity (defaulting to `PrioritetType.NORM`),
not by this mapping.

## Mapping Table Structure

```kotlin
internal data class TableRow(
    val sakstype: Sakstyper,
    val sakstema: Sakstemaer,
    val behandlingstype: Set<Behandlingstyper>,
    val behandlingstema: Set<Behandlingstema>,
    val oppgave: Oppgave
)

internal data class Oppgave(
    val oppgaveBehandlingstema: OppgaveBehandlingstema?,
    val tema: Tema,
    val oppgaveType: Oppgavetyper,
    val beskrivelsefelt: Beskrivelsefelt,
    val regelTruffet: Regel = Regel.FRA_TABELL
)
```

## Example Mappings

### EU/EØS Yrkesaktiv

| Input | Value |
|-------|-------|
| Sakstype | EU_EOS |
| Sakstema | MEDLEMSKAP_LOVVALG |
| Behandlingstema | UTSENDT_ARBEIDSTAKER / ARBEID_FLERE_LAND / ARBEID_KUN_NORGE / … |
| Behandlingstype | FØRSTEGANG / NY_VURDERING / ENDRET_PERIODE / KLAGE |
| **Output** | |
| oppgaveType | BEH_SAK_MK |
| tema | MED |
| oppgaveBehandlingstema | ab0483 (EU_EOS_YRKESAKTIV) |
| beskrivelsefelt | BEHANDLINGSTEMA |

### Trygdeavtale Pensjonist

| Input | Value |
|-------|-------|
| Sakstype | TRYGDEAVTALE |
| Sakstema | MEDLEMSKAP_LOVVALG |
| Behandlingstema | PENSJONIST |
| Behandlingstype | FØRSTEGANG / NY_VURDERING / KLAGE |
| **Output** | |
| oppgaveType | BEH_SAK_MK |
| tema | MED |
| oppgaveBehandlingstema | ab0476 (AVTALAND_PENSJONIST_ELLER_UFORETRYGDET) |
| beskrivelsefelt | TOMT |

### EU/EØS Anmodning om unntak

| Input | Value |
|-------|-------|
| Sakstype | EU_EOS |
| Sakstema | UNNTAK |
| Behandlingstema | ANMODNING_OM_UNNTAK_HOVEDREGEL |
| Behandlingstype | FØRSTEGANG (NY_VURDERING / KLAGE for re-vurdering) |
| **Output** | |
| oppgaveType | BEH_SED |
| tema | UFM |
| oppgaveBehandlingstema | ab0491 (EU_EOS_SOKNAD_OM_UNNTAK) |
| beskrivelsefelt | SED |

## Utleder Classes

### OppgavetypeUtleder
Determines `oppgaveType` by delegating to the mapping:
```kotlin
class OppgavetypeUtleder(private val oppgaveGosysMapping: OppgaveGosysMapping = OppgaveGosysMapping()) {
    fun utledOppgavetype(
        sakstype: Sakstyper, sakstema: Sakstemaer,
        behandlingstema: Behandlingstema, behandlingstype: Behandlingstyper
    ): Oppgavetyper = oppgaveGosysMapping.finnOppgave(sakstype, sakstema, behandlingstema, behandlingstype).oppgaveType
}
```

### OppgaveTemaUtleder
Determines `tema` (MED, UFM, TRY):
```kotlin
class OppgaveTemaUtleder {
    fun utledTema(
        sakstype: Sakstyper, sakstema: Sakstemaer?,
        behandlingstema: Behandlingstema, behandlingstype: Behandlingstyper
    ): Tema
}
```

### OppgaveBeskrivelseUtleder
Derives the task description (`utledBeskrivelse(...)`) from the resolved `beskrivelsefelt`
and behandling context.

## Fallback Logic

`finnOppgaveOrNull` resolves in order:
1. `finnOppgaveFraTabell` — exact match on sakstype + sakstema + behandlingstype ∈ set + behandlingstema ∈ set
2. `finnOppgaveVedBehandlingstypeHenvendelseOgVirksomhet` — special-case VIRKSOMHET + HENVENDELSE
3. `finnOppgaveVedBehandlingstypeHenvendelse` — HENVENDELSE matched on sakstype + behandlingstema

```kotlin
fun finnOppgaveOrNull(sakstype, sakstema, behandlingstema, behandlingstype): Oppgave? =
    finnOppgaveFraTabell(sakstype, sakstema, behandlingstema, behandlingstype)
        ?: finnOppgaveVedBehandlingstypeHenvendelseOgVirksomhet(sakstype, sakstema, behandlingstema, behandlingstype)
        ?: finnOppgaveVedBehandlingstypeHenvendelse(sakstype, sakstema, behandlingstema, behandlingstype)
```

`finnOppgave(...)` calls `finnOppgaveOrNull(...)` and throws `IllegalStateException` if nothing matches.

## Common Mapping Issues

### Unsupported Combination
**Symptom**: `IllegalStateException: "Fant ikke oppgave mapping for sakstype:… sakstema:… behandlingstema:… behandlingstype:…"`

**Cause**: New sakstype/sakstema/behandlingstema/behandlingstype combination not in the `rows` table

**Resolution**: Add a `TableRow` to `OppgaveGosysMapping` (regenerate from the Confluence table)

### Wrong Behandlingstema Code
**Symptom**: Task appears in wrong Gosys queue

**Investigation**:
```kotlin
val oppgave = OppgaveGosysMapping().finnOppgave(sakstype, sakstema, behandlingstema, behandlingstype)
log.info("oppgaveType=${oppgave.oppgaveType}, tema=${oppgave.tema}, behandlingstema=${oppgave.oppgaveBehandlingstema?.kode}")
```

