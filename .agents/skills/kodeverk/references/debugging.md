# Kodeverk Debugging Guide

## Common Issues

### Issue: Invalid Combination Error

**Symptom**:
```
FunksjonellException: "Ugyldig kombinasjon av sakstype, sakstema, behandlingstype og behandlingstema"
```

**Investigation**:

1. **Check valid combinations**:
```kotlin
val gyldige = lovligeKombinasjonerService.hentMuligeBehandlingstemaer(
    sakstype, sakstema, hovedpart, behandlingstyper, sisteBehTema
)
log.info("Valid temaer for $sakstype/$sakstema: $gyldige")
```

2. **Check if it's a second treatment restriction**:
```kotlin
val ANNENGANGS_TEMAER = setOf(
    REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
    REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
    BESLUTNING_LOVVALG_NORGE,
    BESLUTNING_LOVVALG_ANNET_LAND,
    ANMODNING_OM_UNNTAK_HOVEDREGEL
)
// If previous theme in this set, only NY_VURDERING/KLAGE/HENVENDELSE allowed
```

3. **Check case status**:
```sql
SELECT status FROM fagsak WHERE saksnummer = 'MEL-12345';
-- If closed, only HENVENDELSE allowed
```

### Issue: Treatment Type Not Available

**Symptom**: KLAGE or ÅRSAVREGNING not in dropdown

**Check feature toggles**:
```kotlin
// KLAGE
unleashService.isEnabled("BEHANDLINGSTYPE_KLAGE")

// ÅRSAVREGNING
unleashService.isEnabled("MELOSYS_ÅRSAVREGNING")
unleashService.isEnabled("MELOSYS_ÅRSAVREGNING_UTEN_FLYT")  // FTRL
unleashService.isEnabled("MELOSYS_ÅRSAVREGNING_EØS_PENSJONIST")  // EU_EOS
```

### Issue: Missing Enum Value

**Symptom**: Enum value exists but not selectable

**Check**:

1. **Is it in external library?**
   - `melosys-internt-kodeverk` must have the enum

2. **Is combination configured?**
   - Check `LovligeBehandlingsKombinasjoner.java`
   - Check `LovligeSakskombinasjoner.java`

3. **Is it filtered by context?**
   - Actor role (BRUKER vs VIRKSOMHET)
   - Previous treatment theme
   - Case status

### Issue: Wrong Behandlingstema from SED

**Symptom**: SED creates treatment with unexpected theme

**Check mapping**:
```kotlin
// SedTypeTilBehandlingstemaMapper
val tema = when (sedType) {
    A001 -> ANMODNING_OM_UNNTAK_HOVEDREGEL
    A003 -> if (erNorge) BESLUTNING_LOVVALG_NORGE else BESLUTNING_LOVVALG_ANNET_LAND
    A009 -> REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
    A010 -> REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
    else -> null
}
```

## SQL Queries

### Check Case/Treatment Combination
```sql
SELECT f.saksnummer, f.type as sakstype, f.tema as sakstema,
       b.type as behtype, b.tema as behtema, b.status
FROM fagsak f
JOIN behandling b ON b.fagsak_saksnummer = f.saksnummer
WHERE f.saksnummer = 'MEL-12345';
```

### Find Cases with Specific Combination
```sql
SELECT f.saksnummer, b.id
FROM fagsak f
JOIN behandling b ON b.fagsak_saksnummer = f.saksnummer
WHERE f.type = 'EU_EOS'
AND f.tema = 'MEDLEMSKAP_LOVVALG'
AND b.tema = 'ANMODNING_OM_UNNTAK_HOVEDREGEL';
```

### Check Previous Behandlingstema
```sql
SELECT b.tema, b.type, b.status, b.registrert_dato
FROM behandling b
WHERE b.fagsak_saksnummer = 'MEL-12345'
ORDER BY b.registrert_dato DESC;
```

## Log Patterns

### Validation Errors
```bash
grep "LovligeKombinasjonerSaksbehandlingService" application.log
grep "Ugyldig kombinasjon" application.log
```

### Feature Toggle Checks
```bash
grep "BEHANDLINGSTYPE_KLAGE\|MELOSYS_ÅRSAVREGNING" application.log
```

## Key Code Locations

| Component | Location |
|-----------|----------|
| Validation Service | `service/.../lovligekombinasjoner/LovligeKombinasjonerSaksbehandlingService.kt` |
| Case Combinations | `service/.../lovligekombinasjoner/LovligeSakskombinasjoner.java` |
| Treatment Combinations | `service/.../lovligekombinasjoner/LovligeBehandlingsKombinasjoner.java` |
| Status Rules | `service/.../lovligekombinasjoner/LovligeBehandlingstatus.kt` |
| SED Mapping | `service/.../dokument/sed/SedTypeTilBehandlingstemaMapper.java` |

## Adding New Combinations

### Step 1: Add enum (if new)
Update `melosys-internt-kodeverk` library with new enum value.

### Step 2: Add to combination table
```java
// LovligeBehandlingsKombinasjoner.java
public static final SakstemaBehandlingsKombinasjon EU_EOS_LOVVALG =
    new SakstemaBehandlingsKombinasjon(
        MEDLEMSKAP_LOVVALG,
        Set.of(
            new BehandlingstemaBehandlingstyperKombinasjon(
                Set.of(YRKESAKTIV, PENSJONIST, NEW_TEMA),  // Add here
                Set.of(FØRSTEGANG, NY_VURDERING)
            )
        )
    );
```

### Step 3: Update case combinations (if needed)
```java
// LovligeSakskombinasjoner.java
muligeSaksKombinasjonerBruker.put(EU_EOS, Set.of(
    EU_EOS_LOVVALG,
    EU_EOS_NEW_COMBINATION  // Add here
));
```

### Step 4: Test
- Unit test in `LovligeKombinasjonerSaksbehandlingServiceTest`
- Integration test for full flow

## External Kodeverk Debugging

### Decode External Code
```kotlin
val beskrivelse = kodeverkService.dekod(FellesKodeverk.LANDKODER, "NOR")
// Returns: "Norge"
```

### Check Valid Codes
```kotlin
val koder = kodeverkService.hentGyldigeKoderForKodeverk(FellesKodeverk.LANDKODER)
// Returns list of valid country codes with validity periods
```

### Refresh Cache
```kotlin
kodeverkService.lastKodeverk()  // Reloads from external service
```
