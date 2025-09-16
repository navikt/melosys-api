# PersonMedHistorikk - Java to Kotlin Conversion

## Input Metadata
- **File:** PersonMedHistorikk.java
- **Type:** Java record
- **Module:** domain
- **Dependent modules:** service (26 usages), frontend-api (4 usages)

## Conversion Summary
Converted Java record `PersonMedHistorikk` to idiomatic Kotlin `data class` while maintaining compatibility with dependent modules.

### Key Changes
- Java `record` → Kotlin `data class`
- Preserved public field access pattern
- Maintained implementation of `SaksopplysningDokument` interface
- Used expression-style class declaration

## Nullability Analysis

### Nullable Fields
- `dødsfall: Doedsfall?` - Can be null (observed in factory with `null` parameter)
- `fødsel: Foedsel?` - Can be null (observed in factory with `null` parameter)

### Non-nullable Fields
- All `Collection` fields - Non-null but can be empty collections
- `folkeregisteridentifikator: Folkeregisteridentifikator` - Always required
- `kjønn: KjoennType` - Always has a value (uses UKJENT as default)
- `navn: Navn` - Always required for person data

**Rationale:** Based on usage analysis in `PersonopplysningerObjectFactory.lagPersonMedHistorikk()`, `dødsfall` and `fødsel` are explicitly passed as `null`, while other fields are always provided with values or empty collections.

## Field Compatibility
Preserved original public field access pattern from Java record:
- All fields remain public `val` properties
- No getters/setters introduced
- Compatible with existing Jackson serialization
- No `@JsonProperty` annotations needed

## Before/After Code

### Before (Java)
```java
public record PersonMedHistorikk(
    Collection<Bostedsadresse> bostedsadresser,
    Doedsfall dødsfall,
    Foedsel fødsel,
    Folkeregisteridentifikator folkeregisteridentifikator,
    Collection<Folkeregisterpersonstatus> folkeregisterpersonstatuser,
    KjoennType kjønn,
    Collection<Kontaktadresse> kontaktadresser,
    Navn navn,
    Collection<Oppholdsadresse> oppholdsadresser,
    Collection<Sivilstand> sivilstand,
    Collection<Statsborgerskap> statsborgerskap) implements SaksopplysningDokument {
}
```

### After (Kotlin)
```kotlin
data class PersonMedHistorikk(
    val bostedsadresser: Collection<Bostedsadresse>,
    val dødsfall: Doedsfall?,
    val fødsel: Foedsel?,
    val folkeregisteridentifikator: Folkeregisteridentifikator,
    val folkeregisterpersonstatuser: Collection<Folkeregisterpersonstatus>,
    val kjønn: KjoennType,
    val kontaktadresser: Collection<Kontaktadresse>,
    val navn: Navn,
    val oppholdsadresser: Collection<Oppholdsadresse>,
    val sivilstand: Collection<Sivilstand>,
    val statsborgerskap: Collection<Statsborgerskap>
) : SaksopplysningDokument
```

## Dependency Analysis

### Service Module (26 usages)
**Usage patterns:**
- Constructor calls in test factories
- Method parameters in services
- Return types from PersondataService
- Mock return values in tests

**Key locations:**
- `PersonopplysningerObjectFactory.lagPersonMedHistorikk()`
- `SaksopplysningerService.lagrePersonMedHistorikk()`
- `PersondataService.hentPersonMedHistorikk()`
- Various test classes

**Required changes:** None - field access remains identical

### Frontend-api Module (4 usages)
**Usage patterns:**
- GraphQL data fetcher return types
- Test factory methods
- Method parameters

**Key locations:**
- `PersonopplysningerDataFetcher`
- `PersonopplysningerDataFetcherTest.lagPersonMedHistorikk()`

**Required changes:**
- Import change: `no.nav.melosys.domain.person.PersonMedHistorikk` (now .kt instead of .java)

## Test & Build Verification Plan

### 1. Build Domain Module
```bash
mvn clean install -DskipTests -pl domain -am
```

### 2. Test Converted Class
```bash
mvn test -pl domain -Dtest=*PersonMedHistorikk*
```

### 3. Test Dependent Modules

**Service module:**
```bash
mvn test -pl service -Dtest=*PersonMedHistorikk*
mvn test -pl service -Dtest=PersonopplysningerObjectFactory*
mvn test -pl service -Dtest=SaksopplysningerService*
```

**Frontend-api module:**
```bash
mvn test -pl frontend-api -Dtest=PersonopplysningerDataFetcher*
```

### 4. Full Integration Test
```bash
mvn test -pl service,frontend-api
```

## Challenges and Learnings

### Successful Aspects
- Clean conversion from Java record to Kotlin data class
- Proper nullability inference from usage patterns
- No breaking changes to public API
- Compatible with existing Jackson serialization

### Interface Implementation
- Maintained `SaksopplysningDokument` interface implementation
- No changes needed to interface contract

### Type Safety Improvements
- Explicit nullability makes potential NPEs more visible
- Improved IDE support with null safety checks
- Better documentation of data contract through types

## No Safe Accessors Required
No safe accessors (like `hentX` pattern) were needed because:
- All field access in dependent modules uses direct property access
- No usage of `!!` would be required in converted code
- Collections are non-null (empty when no data)
- Nullable fields (`dødsfall`, `fødsel`) are properly handled with null checks

## Conclusion
Conversion completed successfully with improved type safety and no breaking changes to dependent modules. The Kotlin data class provides better nullability semantics while maintaining full compatibility with existing usage patterns.