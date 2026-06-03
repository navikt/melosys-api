# Behandling Kodeverk Reference

## Table of Contents
1. [Behandlingsstatus](#behandlingsstatus)
2. [Behandlingstyper](#behandlingstyper)
3. [Behandlingstema](#behandlingstema)
4. [Behandlingsresultattyper](#behandlingsresultattyper)
5. [Sakstyper and Sakstemaer](#sakstyper-and-sakstemaer)
6. [Legal Combinations](#legal-combinations)

> All enums below come from the external `melosys-internt-kodeverk` artifact
> (package `no.nav.melosys.domain.kodeverk` and `...kodeverk.behandlinger`),
> not from source under `domain/src/main/`.

## Behandlingsstatus

Enum: `no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus` (melosys-internt-kodeverk).
The predicate methods below live on the `Behandling` entity
(`domain/src/main/kotlin/no/nav/melosys/domain/Behandling.kt`).

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
// In Behandling entity (Behandling.kt)
fun erInaktiv() = erAvsluttet() || status == MIDLERTIDIG_LOVVALGSBESLUTNING
fun erAktiv() = !erInaktiv()
fun erAvsluttet() = status == AVSLUTTET
fun erRedigerbar() = erAktiv() && status != IVERKSETTER_VEDTAK &&
    !(status == ANMODNING_UNNTAK_SENDT && tema != IKKE_YRKESAKTIV)
fun erVenterForDokumentasjon() = status in listOf(AVVENT_DOK_PART, AVVENT_DOK_UTL, ANMODNING_UNNTAK_SENDT)
```

---

## Behandlingstyper

Enum: `no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper` (melosys-internt-kodeverk).

| Type | Description | When Used |
|------|-------------|-----------|
| `FØRSTEGANG` | First-time treatment | New case, first behandling |
| `NY_VURDERING` | Re-evaluation | New documents/info for existing case |
| `HENVENDELSE` | Inquiry | General inquiry, no verdict expected |
| `KLAGE` | Complaint/appeal | User disagrees with verdict |
| `ENDRET_PERIODE` | Changed period | Period modification request |
| `MANGLENDE_INNBETALING_TRYGDEAVGIFT` | Missing payment | Auto-created for unpaid fees |
| `ÅRSAVREGNING` | Annual reconciliation | Year-end settlement |
| `SATSENDRING` | Rate change | Avgiftssats changed |

### Type Restrictions

```kotlin
// Cannot change TO FØRSTEGANG if not first behandling in sak
// Cannot change FROM MANGLENDE_INNBETALING_TRYGDEAVGIFT
```

---

## Behandlingstema

Enum: `no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema` (melosys-internt-kodeverk).

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

Enum: `no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper` (melosys-internt-kodeverk).

### Positive Outcomes

| Type | Description |
|------|-------------|
| `MEDLEM_I_FOLKETRYGDEN` | Approved membership |
| `FASTSATT_LOVVALGSLAND` | Law selection established |
| `FORELOEPIG_FASTSATT_LOVVALGSLAND` | Law selection provisionally established |
| `DELVIS_GODKJENT_UNNTAK` | Partially approved exception |

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

## Sakstyper and Sakstemaer

### Sakstyper

Enum: `no.nav.melosys.domain.kodeverk.Sakstyper`.

| Type | Description |
|------|-------------|
| `EU_EOS` | EU/EEA regulation cases |
| `FTRL` | Folketrygdloven cases |
| `TRYGDEAVTALE` | Bilateral agreement cases |

### Sakstemaer

Enum: `no.nav.melosys.domain.kodeverk.Sakstemaer`.

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

### Behandlingstyper Restrictions

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
