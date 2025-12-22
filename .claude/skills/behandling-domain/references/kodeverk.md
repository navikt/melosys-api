# Behandling Kodeverk Reference

## Table of Contents
1. [Behandlingsstatus](#behandlingsstatus)
2. [Behandlingstype](#behandlingstype)
3. [Behandlingstema](#behandlingstema)
4. [Behandlingsresultattyper](#behandlingsresultattyper)
5. [Sakstype and Sakstema](#sakstype-and-sakstema)
6. [Legal Combinations](#legal-combinations)

## Behandlingsstatus

Location: `domain/src/main/java/.../kodeverk/behandlinger/Behandlingsstatus.java`

| Status | Description | UI Text |
|--------|-------------|---------|
| `OPPRETTET` | Behandling created, not yet started | "Behandlingen er opprettet" |
| `UNDER_BEHANDLING` | Active processing | "Behandlingen pågår" |
| `AVSLUTTET` | Treatment completed | "Behandlingen er avsluttet" |
| `IVERKSETTER_VEDTAK` | Executing verdict (async saga running) | "Iverksetter vedtak" |
| `MIDLERTIDIG_LOVVALGSBESLUTNING` | Art. 13 provisional decision | "Midlertidig lovvalgsbeslutning" |
| `AVVENT_DOK_UTL` | Awaiting foreign authority | "Avventer svar fra utenlandsk trygdemyndighet" |
| `AVVENT_DOK_PART` | Awaiting user/party | "Avventer svar" |

### Status Predicates

```kotlin
// In Behandling entity
fun erAktiv() = status in listOf(OPPRETTET, UNDER_BEHANDLING)
fun erInaktiv() = !erAktiv()
fun erAvsluttet() = status == AVSLUTTET
fun erRedigerbar() = status in listOf(OPPRETTET, UNDER_BEHANDLING, AVVENT_DOK_PART, AVVENT_DOK_UTL)
fun erVenterForDokumentasjon() = status in listOf(AVVENT_DOK_PART, AVVENT_DOK_UTL)
```

---

## Behandlingstype

Location: `domain/src/main/java/.../kodeverk/behandlinger/Behandlingstype.java`

| Type | Description | When Used |
|------|-------------|-----------|
| `FØRSTEGANG` | First-time treatment | New case, first behandling |
| `NY_VURDERING` | Re-evaluation | New documents/info for existing case |
| `KLAGE` | Complaint/appeal | User disagrees with verdict |
| `ENDRET_PERIODE` | Changed period | Period modification request |
| `HENVENDELSE` | Inquiry | General inquiry, no verdict expected |
| `MANGLENDE_INNBETALING_TRYGDEAVGIFT` | Missing payment | Auto-created for unpaid fees |
| `ÅRSAVREGNING` | Annual reconciliation | Year-end settlement |

### Type Restrictions

```kotlin
// Cannot change TO FØRSTEGANG if not first behandling in sak
// Cannot change FROM MANGLENDE_INNBETALING_TRYGDEAVGIFT
```

---

## Behandlingstema

Location: `domain/src/main/java/.../kodeverk/behandlinger/Behandlingstema.java`

### EU/EØS Themes
| Theme | Description |
|-------|-------------|
| `UTSENDT_ARBEIDSTAKER` | Posted worker (Art. 12) |
| `UTSENDT_SELVSTENDIG` | Posted self-employed (Art. 12) |
| `ARBEID_FLERE_LAND` | Work in multiple countries (Art. 13) |
| `ARBEID_KUN_NORGE` | Work only in Norway (Art. 11) |
| `ARBEID_TJENESTEPERSON_ELLER_FLY` | Civil servant or flight crew |
| `REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING` | Register exception - posting |
| `REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE` | Register exception - other |
| `BESLUTNING_LOVVALG_NORGE` | Norway designated (Art. 13) |
| `BESLUTNING_LOVVALG_ANNET_LAND` | Other country designated (Art. 13) |
| `ANMODNING_OM_UNNTAK_HOVEDREGEL` | Request for exception |

### FTRL Themes
| Theme | Description |
|-------|-------------|
| `YRKESAKTIV` | Employed person |
| `IKKE_YRKESAKTIV` | Non-employed (family, students) |
| `PENSJONIST` | Pensioner |

### Common Themes
| Theme | Description |
|-------|-------------|
| `TRYGDETID` | Insurance period inquiry |
| `FORESPØRSEL_TRYGDEMYNDIGHET` | Foreign authority inquiry |
| `VIRKSOMHET` | Company-related |

---

## Behandlingsresultattyper

Location: `domain/src/main/java/.../kodeverk/behandlinger/Behandlingsresultattyper.java`

### Positive Outcomes
| Type | Description |
|------|-------------|
| `MEDLEM_I_FOLKETRYGDEN` | Approved membership |
| `FASTSATT_LOVVALGSLAND` | Law selection established |
| `GODKJENT_PERIODE` | Period approved |
| `DELVIS_GODKJENT` | Partially approved |

### Negative Outcomes
| Type | Description |
|------|-------------|
| `AVSLAG_MANGLENDE_OPPL` | Rejected - missing info |
| `AVSLAG_SØKNAD` | Application rejected |
| `UTPEKING_NORGE_AVVIST` | Norway designation rejected |

### Terminations
| Type | Description |
|------|-------------|
| `OPPHØRT` | Membership terminated |
| `DELVIS_OPPHØRT` | Partially terminated |
| `ANNULLERT` | Annulled |

### Administrative
| Type | Description |
|------|-------------|
| `FERDIGBEHANDLET` | Processed (no new verdict) |
| `HENLEGGELSE_BORTFALT` | Dismissed |
| `FASTSATT_TRYGDEAVGIFT` | Fee established |

---

## Sakstype and Sakstema

### Sakstype
| Type | Description |
|------|-------------|
| `EU_EOS` | EU/EEA regulation cases |
| `FTRL` | Folketrygdloven cases |
| `TRYGDEAVTALE` | Bilateral agreement cases |

### Sakstema
| Tema | Description |
|------|-------------|
| `MEDLEMSKAP_LOVVALG` | Membership/law selection |
| `UNNTAK` | Exception from membership |
| `TRYGDEAVGIFT` | Social insurance fee |

---

## Legal Combinations

### Sakstype → Behandlingstema Mapping

**EU_EOS + MEDLEMSKAP_LOVVALG**:
- UTSENDT_ARBEIDSTAKER, UTSENDT_SELVSTENDIG
- ARBEID_FLERE_LAND, ARBEID_KUN_NORGE
- ARBEID_TJENESTEPERSON_ELLER_FLY
- BESLUTNING_LOVVALG_NORGE, BESLUTNING_LOVVALG_ANNET_LAND
- ANMODNING_OM_UNNTAK_HOVEDREGEL

**EU_EOS + UNNTAK**:
- REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
- REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
- FORESPØRSEL_TRYGDEMYNDIGHET

**EU_EOS + TRYGDEAVGIFT**:
- PENSJONIST

**FTRL + MEDLEMSKAP_LOVVALG**:
- YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST

**TRYGDEAVTALE + MEDLEMSKAP_LOVVALG**:
- YRKESAKTIV, IKKE_YRKESAKTIV

### Behandlingstype Restrictions

| Cannot change to | When |
|------------------|------|
| FØRSTEGANG | Not first behandling in sak |
| Any | behandlingstype = MANGLENDE_INNBETALING_TRYGDEAVGIFT |
| Any | behandlingstema in [REGISTRERING_UNNTAK_*, BESLUTNING_LOVVALG_*, ANMODNING_OM_UNNTAK_HOVEDREGEL (if EU_EOS)] |

### Status-Based Restrictions

Cannot change sakstype/behandlingstema when:
- behandlingsstatus = AVSLUTTET
- behandlingsstatus = IVERKSETTER_VEDTAK
- behandlingsstatus = MIDLERTIDIG_LOVVALGSBESLUTNING
- anmodningsperioderErSendtUtlandet = true
