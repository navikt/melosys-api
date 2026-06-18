# Person Data Debugging Guide

## Common Issues

### Issue: Person Not Found

**Symptom**:
```
IkkeFunnetException: "Fant ikke ident i PDL."
```

**Causes**:
- Identity doesn't exist in PDL
- Using wrong identity type (fnr vs aktørID)
- Person is a foreigner without Norwegian registration

**Investigation**:
```kotlin
// Check what identities exist
try {
    val identer = pdlClient.hentIdenter(ident)
    log.info("Found identities: ${identer.identer().map { "${it.gruppe()}: ${it.ident()}" }}")
} catch (e: IkkeFunnetException) {
    log.warn("No identities found for: $ident")
}
```

**Resolution**:
- Verify the identity is correct
- Try alternate identity (fnr ↔ aktørID)
- For foreigners without registration, handle gracefully

### Issue: Empty Address Data

**Symptom**: Person fetched successfully but addresses are empty

**Causes**:
- Person has address protection (kode 6/7)
- No registered address in PDL
- All addresses are historical

**Investigation**:
```kotlin
val person = pdlClient.hentPerson(ident)
log.info("Adressebeskyttelse: ${person.adressebeskyttelse().map { it.gradering() }}")
log.info("Bostedsadresse count: ${person.bostedsadresse().size}")
log.info("Kontaktadresse count: ${person.kontaktadresse().size}")

// Check if protected
if (person.adressebeskyttelse().any { it.erStrengtFortrolig() }) {
    log.warn("Person has strict address protection - addresses may be hidden")
}
```

### Issue: Missing Family Members

**Symptom**: `hentFamiliemedlemmer()` returns empty or incomplete set

**Causes**:
- Person is 18+ (parents not fetched by default)
- Sivilstand doesn't qualify for spouse lookup (must be GIFT, SEPARERT, SEPARERT_PARTNER, or REGISTRERT_PARTNER)
- `relatertPersonsIdent` is null for some relations
- Related person not found in PDL

**Investigation**:
```kotlin
val person = pdlClient.hentFamilierelasjoner(ident)

// Check age
val fødselsdato = person.foedselsdato().firstOrNull()?.foedselsdato()
val age = ChronoUnit.YEARS.between(fødselsdato, LocalDate.now())
log.info("Person age: $age (parents only fetched if < 18)")

// Check sivilstand
person.sivilstand().forEach { sivilstand ->
    log.info("Sivilstand: type=${sivilstand.type()}, relatert=${sivilstand.relatertVedSivilstand()}, gyldig=${sivilstand.erGyldigForEktefelleEllerPartner()}")
}

// Check parent-child relations
person.forelderBarnRelasjon().forEach { relasjon ->
    log.info("Relasjon: rolle=${relasjon.relatertPersonsRolle()}, ident=${relasjon.relatertPersonsIdent()}")
}
```

### Issue: Stale Cached Identity

**Symptom**: Cached aktørID or fnr is outdated after identity change

**Causes**:
- Person got new identity (adoption, etc.)
- Cache not invalidated

**Investigation**:
```kotlin
// Cache keys
log.info("Checking caches: aktoerID, folkeregisterIdent")

// Force fresh lookup by calling PDL directly
val freshIdenter = pdlClient.hentIdenter(ident)
log.info("Fresh from PDL: ${freshIdenter.identer()}")
```

**Resolution**:
- Clear relevant caches
- Use `@CacheEvict` when identity changes are detected

### Issue: Historical Data Not Included

**Symptom**: Only current data returned, missing history

**Cause**: Using `hentPerson()` instead of `hentPersonMedHistorikk()`

**Fix**:
```kotlin
// For current data only
val person = pdlClient.hentPerson(ident)

// For data with history
val personMedHistorikk = pdlClient.hentPersonMedHistorikk(ident)
```

### Issue: Address Priority Wrong

**Symptom**: Wrong address selected as postal address

**Cause**: Priority logic in `Personopplysninger.hentGjeldendePostadresse()`

**Investigation**:
```kotlin
val persondata = persondataService.hentPerson(ident)

// Check each address type
log.info("Kontaktadresser: ${persondata.kontaktadresser.map {
    "master=${it.master}, gyldig=${it.erGyldig()}"
}}")
log.info("Oppholdsadresser: ${persondata.oppholdsadresser.map {
    "master=${it.master}"
}}")
log.info("Bostedsadresse: ${persondata.bostedsadresse}")
log.info("Selected postal address: ${persondata.hentGjeldendePostadresse()}")
```

**Priority order**:
1. Kontaktadresse (PDL master, then FREG)
2. Oppholdsadresse (PDL master, then FREG)
3. Bostedsadresse

## Log Patterns

### PDLClient Operations

```bash
grep "PDLClient\|pdlClient" application.log
```

### Identity Lookups

```bash
grep "hentIdenter\|hentAktørIdForIdent\|hentFolkeregisterident" application.log
```

### Person Data Fetching

```bash
grep "hentPerson\|hentPersonMedHistorikk\|PersondataService" application.log
```

### Family Relations

```bash
grep "hentFamilierelasjoner\|FamiliemedlemService\|EktefelleEllerPartner" application.log
```

## SQL Queries

### Find Person References in Behandling

```sql
-- Find behandlinger for a person (via fagsak).
-- Behandling references Fagsak by saksnummer (the @JoinColumn is "saksnummer",
-- not a fagsak_id). The person/aktør is not a column on fagsak; it lives in the
-- aktoer table (rolle BRUKER) joined on saksnummer.
SELECT b.id, b.status, f.saksnummer
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
JOIN aktoer a ON a.saksnummer = f.saksnummer AND a.rolle = 'BRUKER'
WHERE a.person_ident = :ident;
```

### Check Cached Person Data (Saksopplysninger)

```sql
-- PDL personopplysninger stored in saksopplysning.
-- Columns: opplysning_type (enum stored as string), registrert_dato, dokument (JSON payload).
-- PDL types: PDL_PERSOPL (personopplysning) and PDL_PERS_SAKS (til saksbehandler).
SELECT so.id, so.opplysning_type, so.registrert_dato, so.dokument
FROM saksopplysning so
WHERE so.behandling_id = :behandlingId
AND so.opplysning_type IN ('PDL_PERSOPL', 'PDL_PERS_SAKS');
```

## Key Code Locations

| Component | Location |
|-----------|----------|
| PDL Client | `integrasjon/.../pdl/PDLClient.java` |
| PDL Client Impl | `integrasjon/.../pdl/PDLClientImpl.java` |
| PersondataService | `service/.../persondata/PersondataService.java` |
| FamiliemedlemService | `service/.../persondata/familie/FamiliemedlemService.java` |
| Address Priority | `domain/.../person/Personopplysninger.kt:80-84` |
| Mappers | `service/.../persondata/mapping/` |

## Mock Setup

In local development (melosys-docker-compose):
- PDL is mocked in `melosys-mock` service
- Mock responses are in `mock/src/main/resources/pdl/`
- Default test persons are pre-configured

### Adding Test Person to Mock

1. Add person data to mock resources
2. Configure identity mapping in mock service
3. Restart mock container

## Environment Differences

| Aspect | Local | Dev (Q1/Q2) | Prod |
|--------|-------|-------------|------|
| PDL | Mocked | PDL-Q | PDL-P |
| Auth | mock-oauth2 | Azure AD | Azure AD |
| Data | Test data | Test data | Real data |

## Debugging Tips

1. **Always check address protection first** when addresses are missing
2. **Use hentIdenter()** to verify identity exists before other operations
3. **Check sivilstand type and relatertVedSivilstand** when spouse is missing
4. **Verify person age** when parents are missing (must be < 18)
5. **Check metadata.historisk** flag when data seems outdated
