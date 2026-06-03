# Behandlingsresultat Types

## Behandlingsresultattyper Enum

Complete reference of all result types and when they're used.

| Type | Description | Case Types | Terminal |
|------|-------------|------------|----------|
| `IKKE_FASTSATT` | Not yet determined | All | No |
| `FASTSATT_LOVVALGSLAND` | Law choice country determined | LOVVALG | Yes |
| `MEDLEM_I_FOLKETRYGDEN` | Member of Norwegian NI | MEDLEMSKAP | Yes |
| `UNNTATT_MEDLEMSKAP` | Exempt from membership | MEDLEMSKAP | Yes |
| `AVSLAG_SØKNAD` | Application denied | All | Yes |
| `OPPHØRT` | Membership/law choice terminated | All | Yes |
| `ANMODNING_OM_UNNTAK` | Article 16 exception request sent | LOVVALG | No |
| `REGISTRERT_UNNTAK` | Exception registered/approved | LOVVALG | Yes |
| `HENLEGGELSE` | Case dismissed | All | Yes |
| `FERDIGBEHANDLET` | Processing completed | All | Yes |
| `MEDHOLD` | Appeal upheld | KLAGE | Yes |
| `KLAGEINNSTILLING` | Appeal recommendation | KLAGE | No |
| `AVVIST_KLAGE` | Appeal rejected | KLAGE | Yes |
| `OMGJORT` | Decision reversed (fvl § 35), no klage required | NY_VURDERING | Yes |

> The enum has more values than listed here, e.g. `FORELOEPIG_FASTSATT_LOVVALGSLAND` (backs `erUtpeking()`),
> `REGISTRERT_UNNTAK`, `DELVIS_GODKJENT_UNNTAK`, `UTPEKING_NORGE_AVVIST`, `AVSLAG_MANGLENDE_OPPL`, `ANNULLERT`,
> `AVBRUTT`, `DELVIS_OPPHØRT`, `FASTSATT_TRYGDEAVGIFT`, `HENLEGGELSE_BORTFALT`.

## Result Type by Case Type

### Lovvalg Cases (EU/EEA)
```
IKKE_FASTSATT → FASTSATT_LOVVALGSLAND  (normal flow)
IKKE_FASTSATT → ANMODNING_OM_UNNTAK → REGISTRERT_UNNTAK  (Article 16)
IKKE_FASTSATT → AVSLAG_SØKNAD  (rejection)
IKKE_FASTSATT → HENLEGGELSE  (dismissed)
*             → OPPHØRT  (termination)
```

### Medlemskap Cases (FTRL)
```
IKKE_FASTSATT → MEDLEM_I_FOLKETRYGDEN  (approved)
IKKE_FASTSATT → UNNTATT_MEDLEMSKAP  (exempt)
IKKE_FASTSATT → AVSLAG_SØKNAD  (rejected)
IKKE_FASTSATT → HENLEGGELSE  (dismissed)
*             → OPPHØRT  (termination)
```

### Klage Cases (Appeals, behandlingstype KLAGE)
```
IKKE_FASTSATT → MEDHOLD  (appeal granted)
IKKE_FASTSATT → AVVIST_KLAGE  (appeal rejected)
IKKE_FASTSATT → KLAGEINNSTILLING  (recommendation made)
```

### Ny vurdering Cases (behandlingstype NY_VURDERING)
```
IKKE_FASTSATT → OMGJORT  (decision reversed, fvl § 35 - no klage required)
```
OMGJORT is validated against `NY_VURDERING` in `AngiBehandlingsresultatService`, not against KLAGE.

## Checking Result Type

```kotlin
// Status checks
resultat.erInnvilgelse()           // FASTSATT_LOVVALGSLAND for lovvalg
resultat.erAvslag()                // AVSLAG_SØKNAD
resultat.erOpphørt()               // OPPHØRT
resultat.erAnmodningOmUnntak()     // ANMODNING_OM_UNNTAK
resultat.erRegistrertUnntak()      // REGISTRERT_UNNTAK
resultat.erUtpeking()              // Is designation

// Direct type check
when (resultat.type) {
    Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN -> // ...
    Behandlingsresultattyper.FASTSATT_LOVVALGSLAND -> // ...
    // ...
}
```

## Setting Result Type

### Via Service (with validation)
```kotlin
// Sets type and closes case (takes behandlingID: Long, behandlingsresultattype)
angiBehandlingsresultatService.oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(
    behandlingId,
    Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
)
```

### Direct Update
```kotlin
// Takes (id: Long, behandlingsresultattype)
behandlingsresultatService.oppdaterBehandlingsresultattype(
    id,
    Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
)
```

## Validation Rules

The `AngiBehandlingsresultatService` validates that the result type is appropriate for:

1. **Case type** (MEDLEMSKAP vs LOVVALG)
2. **Case theme** (TRYGDEAVGIFT, etc.)
3. **Treatment theme**
4. **Treatment type** (SØKNAD, KLAGE, NY_VURDERING, etc.)

Example validation:
```kotlin
// Only these types valid for MEDLEMSKAP cases
val GYLDIGE_FOR_MEDLEMSKAP = setOf(
    MEDLEM_I_FOLKETRYGDEN,
    UNNTATT_MEDLEMSKAP,
    AVSLAG_SØKNAD,
    OPPHØRT,
    HENLEGGELSE
)
```

## Processing Method (Behandlingsmaate)

Each result also tracks how it was processed:

| Method | Description |
|--------|-------------|
| `MANUELT` | Manually processed by saksbehandler |
| `AUTOMATISERT` | Fully automated processing |
| `DELVIS_AUTOMATISERT` | Partially automated |

```kotlin
resultat.erAutomatisert()  // Check if automated

// Takes (id: Long, behandlingsmaate: Behandlingsmaate)
behandlingsresultatService.oppdaterBehandlingsMaate(
    id,
    Behandlingsmaate.AUTOMATISERT
)
```

## Debugging

### Find results by type
```sql
SELECT br.id, br.resultat_type, b.status, f.saksnummer
FROM behandlingsresultat br
JOIN behandling b ON b.id = br.id
JOIN fagsak f ON f.saksnummer = b.saksnummer
WHERE br.resultat_type = 'FASTSATT_LOVVALGSLAND';
```

### Find IKKE_FASTSATT results (incomplete)
```sql
SELECT br.id, f.saksnummer, b.status, b.registrert_dato
FROM behandlingsresultat br
JOIN behandling b ON b.id = br.id
JOIN fagsak f ON f.saksnummer = b.saksnummer
WHERE br.resultat_type = 'IKKE_FASTSATT'
AND b.status NOT IN ('AVSLUTTET', 'LUKKET')
ORDER BY b.registrert_dato;
```
