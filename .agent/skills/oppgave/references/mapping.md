# OppgaveGosysMapping Reference

The `OppgaveGosysMapping` class contains the complex mapping table that determines task properties
based on behandling context.

## Mapping Source

Confluence documentation: https://confluence.adeo.no/display/TEESSI/Oppgaver+i+Gosys

## Mapping Inputs

| Input | Source | Description |
|-------|--------|-------------|
| Sakstype | `fagsak.type` | EU_EOS, TRYGDEAVTALE, FTRL, etc. |
| Sakstema | `fagsak.tema` | MEDLEMSKAP_LOVVALG, TRYGDEAVGIFT, etc. |
| Behandlingstema | `behandling.tema` | MEDLEMSKAP, LOVVALG, PENSJON, etc. |
| Behandlingstype | `behandling.type` | FØRSTEGANG, NY_VURDERING, KLAGE, etc. |

## Mapping Outputs

| Output | Target | Description |
|--------|--------|-------------|
| Oppgavetype | `oppgave.oppgavetype` | BEH_SAK_MK, VUR, etc. |
| Tema | `oppgave.tema` | MED, UFM, TRY |
| Behandlingstema | `oppgave.behandlingstema` | ab0424, ab0483, etc. |
| Prioritet | `oppgave.prioritet` | LAV, NORM, HOY |

## Mapping Table Structure

```kotlin
data class GosysMappingRow(
    val sakstype: Sakstyper?,
    val sakstema: Sakstemaer?,
    val behandlingstema: Behandlingstema?,
    val behandlingstype: Behandlingstyper?,
    val oppgavetype: Oppgavetyper,
    val tema: Tema,
    val behandlingstemaCode: String,
    val prioritet: PrioritetType
)
```

## Example Mappings

### EU/EØS Yrkesaktiv

| Input | Value |
|-------|-------|
| Sakstype | EU_EOS |
| Sakstema | MEDLEMSKAP_LOVVALG |
| Behandlingstema | LOVVALG |
| Behandlingstype | FØRSTEGANG |
| **Output** | |
| Oppgavetype | BEH_SAK_MK |
| Tema | MED |
| Behandlingstema | ab0483 (EU_EOS_YRKESAKTIV) |
| Prioritet | NORM |

### Trygdeavtale Pensjonist

| Input | Value |
|-------|-------|
| Sakstype | TRYGDEAVTALE |
| Sakstema | MEDLEMSKAP_LOVVALG |
| Behandlingstema | PENSJON |
| Behandlingstype | FØRSTEGANG |
| **Output** | |
| Oppgavetype | BEH_SAK_MK |
| Tema | MED |
| Behandlingstema | ab0476 (AVTALAND_PENSJONIST) |
| Prioritet | NORM |

### Anmodning Unntak

| Input | Value |
|-------|-------|
| Sakstype | EU_EOS |
| Sakstema | MEDLEMSKAP_LOVVALG |
| Behandlingstema | LOVVALG |
| Behandlingstype | * (any with anmodning) |
| **Output** | |
| Oppgavetype | BEH_SAK_MK |
| Tema | MED |
| Behandlingstema | ab0460 (ANMODNING_UNNTAK) |
| Prioritet | NORM |

## Utleder Classes

### OppgavetypeUtleder
Determines `oppgavetype` based on behandling characteristics:
```kotlin
class OppgavetypeUtleder {
    fun utled(behandling: Behandling): Oppgavetyper
}
```

### OppgaveTemaUtleder
Determines `tema` (MED, UFM, TRY):
```kotlin
class OppgaveTemaUtleder {
    fun utled(fagsak: Fagsak, behandling: Behandling): Tema
}
```

### OppgaveBeskrivelseUtleder
Derives task description from context:
```kotlin
class OppgaveBeskrivelseUtleder {
    fun utled(behandling: Behandling): String
}
```

## Fallback Logic

When no exact mapping found:
1. Try without behandlingstype (wildcard)
2. Try without behandlingstema
3. Use default values

```kotlin
fun finnMapping(sakstype, sakstema, behTema, behType): GosysMappingRow {
    return rows.find { exact match }
        ?: rows.find { match without behType }
        ?: rows.find { match without behTema }
        ?: defaultMapping
}
```

## Common Mapping Issues

### Unsupported Combination
**Symptom**: `TekniskException: "Fant ikke mapping for ..."`

**Cause**: New sakstype/tema combination not in mapping table

**Resolution**: Add row to OppgaveGosysMapping

### Wrong Behandlingstema Code
**Symptom**: Task appears in wrong Gosys queue

**Investigation**:
```kotlin
val mapping = OppgaveGosysMapping.finnMapping(
    sakstype, sakstema, behTema, behType
)
log.info("Mapping: $mapping")
```

