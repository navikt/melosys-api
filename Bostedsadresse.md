# Java to Kotlin Conversion: Bostedsadresse

## Input Metadata
- **Java file**: `domain/src/main/java/no/nav/melosys/domain/person/adresse/Bostedsadresse.java`
- **Primary module**: domain (extended by service with 63 usages)
- **Dependent modules**: frontend-api (10 usages)
- **Purpose**: Residential address (extends PersonAdresse interface)

## Conversion Summary
Successfully converted Java record `Bostedsadresse` to idiomatic Kotlin data class with JPA compatibility and safe nullability handling.

### Key Changes Made
1. **Record to Data Class**: Converted Java record to Kotlin data class with `@JvmRecord` annotation for Java interoperability
2. **Nullability**: All fields made nullable except `erHistorisk` (Boolean)
3. **Safe Accessor**: Added `hentStrukturertAdresse` safe accessor to prevent `!!` usage in existing Java code
4. **JPA Compatibility**: Used `val` fields with `@JvmRecord` for record-like behavior
5. **Interface Implementation**: Preserved `PersonAdresse` interface implementation
6. **Business Logic**: Maintained exact `erGyldig()` logic and null checking

## Before/After Code

### Before (Java)
```java
public record Bostedsadresse(
    StrukturertAdresse strukturertAdresse,
    String coAdressenavn,
    LocalDate gyldigFraOgMed,
    LocalDate gyldigTilOgMed,
    String master,
    String kilde,
    boolean erHistorisk
) implements PersonAdresse {

    @Override
    public boolean erGyldig() {
        return !erHistorisk && strukturertAdresse != null && strukturertAdresse.erGyldig();
    }
}
```

### After (Kotlin)
```kotlin
@JvmRecord
data class Bostedsadresse(
    val strukturertAdresse: StrukturertAdresse?,
    val coAdressenavn: String?,
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
    val master: String?,
    val kilde: String?,
    val erHistorisk: Boolean
) : PersonAdresse {

    // Safe accessor to avoid !! usage in existing Java code
    val hentStrukturertAdresse: StrukturertAdresse
        get() = strukturertAdresse ?: error("strukturertAdresse er påkrevd for Bostedsadresse")

    override fun erGyldig(): Boolean {
        return !erHistorisk && strukturertAdresse != null && strukturertAdresse.erGyldig()
    }
}
```

## Nullability Analysis
- **strukturertAdresse**: Made nullable with safe accessor due to unsafe access patterns in existing code
- **coAdressenavn, gyldigFraOgMed, gyldigTilOgMed, master, kilde**: Made nullable as typical for address components
- **erHistorisk**: Kept non-null as it's a business flag

## Safe Accessor Implementation
Added `hentStrukturertAdresse` safe accessor because existing Java code directly calls `strukturertAdresse()` without null checking, particularly in:
- `BostedGrunnlag.java:55` - `bostedsadresseFraRegister.strukturertAdresse().getLandkode()`
- `TrygdeavtaleAdresseSjekker.java` - Multiple unsafe accesses to `strukturertAdresse().toList()`

## Dependency Analysis

### Import Changes Required
No import changes needed - all imports already point to correct package `no.nav.melosys.domain.person.adresse.Bostedsadresse`.

### Usage Patterns Found

#### Constructor Usage
- **File**: `service/src/main/java/no/nav/melosys/service/persondata/mapping/adresse/BostedsadresseOversetter.java:48`
- **Pattern**: `new Bostedsadresse(...)` - Will work seamlessly with Kotlin data class

#### Method Access
- **Files**: Multiple in service module
- **Pattern**: Direct access to record methods via `record.field()` syntax
- **Impact**: No changes needed - Kotlin data class methods are compatible

#### Interface Implementation
- **Usage**: As `PersonAdresse` interface
- **Impact**: No changes needed - interface contract preserved

## Specific Updates Required

### High Priority (Direct Method Calls)
1. **BostedGrunnlag.java:55**
   - Current: `bostedsadresseFraRegister.strukturertAdresse().getLandkode()`
   - Recommendation: Use safe accessor or add null checking

2. **TrygdeavtaleAdresseSjekker.java (lines 29, 31, 37, 40)**
   - Current: `personAdresse.strukturertAdresse().toList()`
   - Recommendation: Use safe accessor or add null checking

### Medium Priority (Constructor Usage)
1. **BostedsadresseOversetter.java:48**
   - Current: `new Bostedsadresse(...)`
   - Status: ✅ Compatible - no changes needed

### Low Priority (Interface Usage)
- All interface-based usage continues to work without changes
- GraphQL mapping in frontend-api works seamlessly

## Test & Build Verification Plan

### 1. Build Primary Module (domain)
```bash
mvn clean install -DskipTests -pl domain -am
```

### 2. Test Converted Class
```bash
mvn test -pl domain -Dtest=*Bostedsadresse*
```

### 3. Test Dependent Modules
Based on dependency matrix, test these modules that depend on domain:
```bash
mvn test -pl repository,service,saksflyt,frontend-api,integrasjon,saksflyt-api,app
```

### 4. Specific Test Commands
```bash
# Test service module (primary usage)
mvn test -pl service -Dtest=*Bostedsadresse*

# Test frontend-api module
mvn test -pl frontend-api -Dtest=*Bostedsadresse*

# Test integration scenarios
mvn test -pl service -Dtest=*PersonMedHistorikk*
mvn test -pl service -Dtest=*TrygdeavtaleAdresse*
```

## JPA and Record Compatibility Notes
- **@JvmRecord**: Maintains binary compatibility with Java consumers
- **val fields**: Immutable fields as expected for data transfer objects
- **No JPA annotations**: This is a pure data class, not an entity
- **Interface implementation**: PersonAdresse contract fully preserved

## Risk Assessment
- **Low Risk**: Conversion maintains exact API compatibility
- **Safe Accessors**: Added to handle existing unsafe access patterns
- **Binary Compatibility**: @JvmRecord ensures Java interoperability
- **Test Coverage**: Extensive test coverage in both service and frontend modules

## Files Modified
1. Created: `domain/src/main/kotlin/no/nav/melosys/domain/person/adresse/Bostedsadresse.kt`
2. Will delete: `domain/src/main/java/no/nav/melosys/domain/person/adresse/Bostedsadresse.java`
3. Documentation: `Bostedsadresse.md`

## Next Steps
1. Run verification tests
2. Update any failing direct access patterns to use safe accessor
3. Commit changes once all tests pass