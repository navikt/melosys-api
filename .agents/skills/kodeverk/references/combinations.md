# Legal Combinations Reference

## Combination Hierarchy

```
Aktoersrolle (BRUKER / VIRKSOMHET)
└── Sakstype
    └── Sakstema
        └── Behandlingstema
            └── Behandlingstype
```

## EU_EOS Combinations

### BRUKER + MEDLEMSKAP_LOVVALG

**Behandlingstemaer:**
- YRKESAKTIV
- IKKE_YRKESAKTIV
- PENSJONIST
- UTSENDT_ARBEIDSTAKER
- UTSENDT_SELVSTENDIG
- ARBEID_FLERE_LAND
- ARBEID_TJENESTEPERSON_ELLER_FLY
- ANMODNING_OM_UNNTAK_HOVEDREGEL
- REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
- REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
- BESLUTNING_LOVVALG_NORGE
- BESLUTNING_LOVVALG_ANNET_LAND

**Behandlingstyper:**
- FØRSTEGANG
- NY_VURDERING
- KLAGE (toggle)
- HENVENDELSE

### VIRKSOMHET + MEDLEMSKAP_LOVVALG

**Behandlingstemaer:**
- VIRKSOMHET

**Behandlingstyper:**
- FØRSTEGANG
- NY_VURDERING
- HENVENDELSE

## FTRL Combinations

### BRUKER + MEDLEMSKAP_LOVVALG

**Behandlingstemaer:**
- YRKESAKTIV
- IKKE_YRKESAKTIV
- PENSJONIST
- UNNTAK_MEDLEMSKAP
- TRYGDETID

**Behandlingstyper:**
- FØRSTEGANG
- NY_VURDERING
- KLAGE (toggle)
- HENVENDELSE
- ÅRSAVREGNING (only YRKESAKTIV, toggle)

### BRUKER + TRYGDEAVGIFT

**Behandlingstemaer:**
- YRKESAKTIV

**Behandlingstyper:**
- FØRSTEGANG
- NY_VURDERING
- HENVENDELSE
- MANGLENDE_INNBETALING_TRYGDEAVGIFT

### VIRKSOMHET + TRYGDEAVGIFT

**Behandlingstemaer:**
- VIRKSOMHET

**Behandlingstyper:**
- FØRSTEGANG
- NY_VURDERING
- HENVENDELSE

## TRYGDEAVTALE Combinations

### BRUKER + MEDLEMSKAP_LOVVALG

**Behandlingstemaer:**
- YRKESAKTIV
- PENSJONIST
- UTSENDT_ARBEIDSTAKER
- UTSENDT_SELVSTENDIG
- REGISTRERING_UNNTAK

**Behandlingstyper:**
- FØRSTEGANG
- NY_VURDERING
- KLAGE (toggle)
- HENVENDELSE

## Special Combination Rules

### Second Treatment Restriction

After these themes:
- REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
- REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
- BESLUTNING_LOVVALG_NORGE
- BESLUTNING_LOVVALG_ANNET_LAND
- ANMODNING_OM_UNNTAK_HOVEDREGEL

**Only allowed:**
- NY_VURDERING
- KLAGE
- HENVENDELSE

### Closed Case Restriction

For cases with status:
- HENLAGT
- AVSLUTTET
- OPPHØRT
- ANNULLERT
- HENLAGT_BORTFALT

**Only allowed:**
- HENVENDELSE

### ÅRSAVREGNING Restrictions

| Sakstype | Sakstema | Behandlingstema | Toggle |
|----------|----------|-----------------|--------|
| FTRL | MEDLEMSKAP_LOVVALG | YRKESAKTIV | MELOSYS_ÅRSAVREGNING_UTEN_FLYT |
| EU_EOS | MEDLEMSKAP_LOVVALG | PENSJONIST | MELOSYS_ÅRSAVREGNING_EØS_PENSJONIST |

## SED-Derived Combinations

When treatment created from SED:

| SED Type | Behandlingstema |
|----------|-----------------|
| A001 | ANMODNING_OM_UNNTAK_HOVEDREGEL |
| A003 (Norway) | BESLUTNING_LOVVALG_NORGE |
| A003 (Other) | BESLUTNING_LOVVALG_ANNET_LAND |
| A009 | REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING |
| A010 | REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE |

## Validation Methods

```kotlin
// Check if combination is valid
service.validerOpprettelseOgEndring(
    sakstype = Sakstyper.EU_EOS,
    sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
    behandlingstype = Behandlingstyper.FØRSTEGANG,
    behandlingstema = Behandlingstema.YRKESAKTIV
)

// Get valid options
val temaer = service.hentMuligeBehandlingstemaer(
    sakstype, sakstema, hovedpart, behandlingstyper, sisteBehTema
)
```

## Configuration Files

| File | Purpose |
|------|---------|
| `LovligeSakskombinasjoner.java` | Sakstype + Sakstema by role |
| `LovligeBehandlingsKombinasjoner.java` | Full combination tables |
| `LovligeBehandlingstatus.kt` | Valid statuses |

## Confluence Reference

Full documentation:
https://confluence.adeo.no/display/TEESSI/Lovlige+kombinasjoner+av+sakstype%2C+sakstema%2C+behandlingstype+og+behandlingstema
