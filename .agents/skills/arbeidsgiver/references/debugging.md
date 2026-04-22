# Arbeidsgiver Debugging Guide

## Common Issues

### Issue: Organization Not Found

**Symptom**:
```
IkkeFunnetException when calling eregFasade.hentOrganisasjon(orgnummer)
```

**Causes**:
- Invalid organization number format
- Organization doesn't exist in EREG
- Organization has been deleted (check opphoersdato)
- Using fnr (11 digits) instead of orgnr (9 digits)

**Investigation**:
```kotlin
// Validate orgnr
if (orgnr.length == 11) {
    log.error("Looks like fnr, not orgnr: $orgnr")
    return  // fnr will be rejected
}
if (orgnr.length != 9) {
    log.error("Invalid orgnr length: ${orgnr.length}")
}

// Try optional lookup
val optOrg = eregFasade.finnOrganisasjon(orgnr)
if (optOrg.isEmpty) {
    log.warn("Organization not found in EREG: $orgnr")
}
```

**Resolution**:
- Verify the organization number is correct
- Check if organization exists in Brønnøysundregistrene
- For deleted organizations, historical data may still be available

### Issue: Missing Organization Address

**Symptom**: Letter cannot be sent, address fields empty

**Causes**:
- Organization has no registered address in EREG
- Address is in invalid period (bruksperiode/gyldighetsperiode)

**Investigation**:
```kotlin
val org = organisasjonOppslagService.hentOrganisasjon(orgnr)
log.info("Orgnummer: ${org.orgnummer}")
log.info("Navn: ${org.navn}")
log.info("Har postadresse: ${org.harRegistrertPostadresse()}")
log.info("Har forretningsadresse: ${org.harRegistrertForretningsadresse()}")
log.info("Tilgjengelig adresse: ${org.hentTilgjengeligAdresse()}")

// Check raw details
org.organisasjonDetaljer.postadresse.forEach { addr ->
    log.info("Postadresse: ${addr.adresselinje1}, ${addr.postnummer} ${addr.poststed}")
}
```

**Resolution**:
- Use forretningsadresse if postadresse is missing
- Consider using a manual address override

### Issue: Empty Employment History

**Symptom**: `arbeidsforholdService.finnArbeidsforholdPrArbeidstaker()` returns empty list

**Causes**:
- Person has no registered employment
- Date range doesn't overlap any employment periods
- Wrong Regelverk (A_ORDNINGEN only has data from 2015+)
- Employment is under a different fnr (identity change)

**Investigation**:
```kotlin
// Try with expanded parameters
val query = ArbeidsforholdQuery(
    regelverk = Regelverk.ALLE,  // Include pre-2015 data
    arbeidsforholdType = ArbeidsforholdType.ALLE,
    ansettelsesperiodeFom = fom.minusYears(5),  // Expand date range
    ansettelsesperiodeTom = null  // No end date filter
)

val response = arbeidsforholdConsumer.finnArbeidsforholdPrArbeidstaker(fnr, query)
log.info("Found ${response.arbeidsforhold.size} employment relationships")

response.arbeidsforhold.forEach { arb ->
    log.info("Employment: ${arb.arbeidsgiver.organisasjonsnummer}, " +
             "period: ${arb.ansettelsesperiode.periode.fom} - ${arb.ansettelsesperiode.periode.tom}")
}
```

**Resolution**:
- Expand date range to capture historical employment
- Use `Regelverk.ALLE` to include pre-2015 data
- Verify person's identity (may have changed fnr)

### Issue: Employer is Person, Not Organization

**Symptom**: `arbeidsgiverID` is null, but `arbeidsgivertype` is `PERSON`

**Cause**: Employer is a private individual (enkeltpersonforetak)

**Investigation**:
```kotlin
arbeidsforhold.forEach { arb ->
    log.info("Arbeidsgiver type: ${arb.arbeidsgivertype}")
    if (arb.arbeidsgivertype == Aktoertype.PERSON) {
        log.info("Employer is a person, not organization")
        // Can't look up in EREG
    } else {
        log.info("Employer orgnr: ${arb.arbeidsgiverID}")
    }
}
```

**Resolution**:
- Handle `PERSON` type employers separately
- Don't attempt EREG lookup for person employers

### Issue: Missing Occupation Code Translation

**Symptom**: Yrke code shown instead of readable text

**Cause**: Code not found in kodeverk

**Investigation**:
```kotlin
val yrkeKode = arbeidsavtale.yrke
val yrkeTerm = kodeverkService.getTermFraKodeverk(FellesKodeverk.YRKER, yrkeKode)
log.info("Yrke: $yrkeKode -> $yrkeTerm")
```

**Resolution**:
- Check if kodeverk is up to date
- Handle unknown codes gracefully

### Issue: Stale Register Data

**Symptom**: Register data doesn't reflect recent changes

**Cause**: Cached saksopplysninger on behandling

**Investigation**:
```sql
-- Check when register data was last fetched
SELECT b.id, b.siste_opplysninger_hentet_dato
FROM behandling b
WHERE b.id = :behandlingId;

-- Check stored saksopplysninger
SELECT so.type, so.registrert_dato, so.versjon
FROM saksopplysning so
WHERE so.behandling_id = :behandlingId
AND so.type IN ('ARBFORH', 'ORG');
```

**Resolution**:
- Refresh register data via `RegisteropplysningerService.hentOgLagreOpplysninger()`
- Delete and re-fetch if needed: `slettRegisterOpplysninger(behandlingID)`

## Log Patterns

### EREG Operations

```bash
grep "EregRestService\|OrganisasjonRestConsumer\|eregFasade" application.log
```

### AAREG Operations

```bash
grep "ArbeidsforholdService\|ArbeidsforholdConsumer\|AAREG" application.log
```

### Register Operations

```bash
grep "RegisteropplysningerService\|hentOgLagreOpplysninger" application.log
```

## SQL Queries

### Find Organization Saksopplysninger

```sql
-- Find organization data for a behandling
SELECT so.id, so.registrert_dato, so.versjon,
       so.kildesystem, so.mottatt_dokument
FROM saksopplysning so
WHERE so.behandling_id = :behandlingId
AND so.type = 'ORG';
```

### Find Employment Saksopplysninger

```sql
-- Find employment data for a behandling
SELECT so.id, so.registrert_dato, so.versjon,
       so.kildesystem, so.mottatt_dokument
FROM saksopplysning so
WHERE so.behandling_id = :behandlingId
AND so.type = 'ARBFORH';
```

### Find All Register Data Timestamps

```sql
SELECT b.id as behandling_id,
       b.siste_opplysninger_hentet_dato,
       so.type,
       so.registrert_dato
FROM behandling b
LEFT JOIN saksopplysning so ON so.behandling_id = b.id
WHERE b.id = :behandlingId
ORDER BY so.type;
```

## Key Code Locations

| Component | Location |
|-----------|----------|
| EREG Facade | `integrasjon/.../ereg/EregFasade.kt` |
| EREG Service | `integrasjon/.../ereg/EregRestService.kt` |
| EREG Consumer | `integrasjon/.../ereg/organisasjon/OrganisasjonRestConsumer.kt` |
| EREG Converter | `integrasjon/.../ereg/EregDtoTilSaksopplysningKonverter.kt` |
| AAREG Service | `service/.../aareg/ArbeidsforholdService.kt` |
| AAREG Consumer | `integrasjon/.../aareg/arbeidsforhold/ArbeidsforholdConsumer.kt` |
| AAREG Converter | `service/.../aareg/ArbeidsforholdKonverter.kt` |
| Register Service | `service/.../registeropplysninger/RegisteropplysningerService.java` |
| Org Lookup | `service/.../registeropplysninger/OrganisasjonOppslagService.java` |

## Mock Setup

In local development (melosys-docker-compose):
- EREG and AAREG are mocked in `melosys-mock` service
- Mock responses are configured in mock service

### Test Data

Common test organization numbers:
- `889640782` - Test organization

## Environment Differences

| Aspect | Local | Dev (Q1/Q2) | Prod |
|--------|-------|-------------|------|
| EREG | Mocked | EREG-Q | EREG-P |
| AAREG | Mocked | AAREG-Q | AAREG-P |
| Auth | mock-oauth2 | Azure AD | Azure AD |
| Data | Test data | Test data | Real data |

## Debugging Tips

1. **Always validate orgnr length** - 9 digits for organizations, 11 is fnr
2. **Check Aktoertype** before EREG lookup - PERSON employers have no org data
3. **Expand date ranges** when employment is missing
4. **Use Regelverk.ALLE** for historical data (pre-2015)
5. **Check siste_opplysninger_hentet_dato** for data freshness
6. **Handle missing addresses gracefully** - not all organizations have them
