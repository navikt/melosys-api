# IkkeOmfattetFamilie Java to Kotlin Conversion

## Input Metadata
- **File name:** IkkeOmfattetFamilie.java
- **File type:** Java class (domain model)
- **Primary module:** domain
- **Dependent modules:** service (13 usages), frontend-api (2 usages)
- **Notes:** Non-covered family member representation

## Conversion Summary
Successfully converted `IkkeOmfattetFamilie` from Java to idiomatic Kotlin with the following transformations:

### Key Changes
1. **Primary constructor**: Converted Java constructor to Kotlin primary constructor with nullable parameters
2. **Properties**: All parameters made nullable (`String?`) based on usage analysis showing `null` values being passed
3. **Mutable properties**: `sammensattNavn` and `ident` retained as mutable `var` properties
4. **Expression body**: Used expression body syntax for `toString()` method
5. **Getter/setter elimination**: Removed explicit getters/setters in favor of Kotlin property syntax

## Nullability Rationale
Carefully analyzed usage patterns to determine appropriate nullability:
- **`uuid: String`** - Made non-null because all usage patterns pass valid strings (UUIDs, identifiers)
- **`begrunnelse: String?`** - Made nullable because multiple test cases pass `null`: `IkkeOmfattetFamilie(UUID_BARN, null, FRITEKST_BARN)`
- **`begrunnelseFritekst: String?`** - Made nullable because multiple test cases pass `null`: `IkkeOmfattetFamilie(barn2.uuid, null, null)`
- **`sammensattNavn` and `ident`** - Kept as nullable mutable properties set after construction

## Safe Accessor Decisions
No safe accessors were needed because:
- All usages in dependent modules are straightforward constructor calls and property access
- No usage patterns require non-null assertions (`!!`)
- The nullable design matches the actual usage patterns in the codebase

## Field Compatibility Reasoning
- **Public properties**: Maintained public visibility for all properties to ensure compatibility
- **Mutable properties**: Preserved `var` for `sammensattNavn` and `ident` as they are modified after construction
- **No property annotations**: No special Jackson or JPA annotations needed for this simple data class

## Before/After Code

### Before (Java)
```java
public class IkkeOmfattetFamilie {
    private final String uuid;
    private final String begrunnelse;
    private final String begrunnelseFritekst;
    private String sammensattNavn;
    private String ident;

    public IkkeOmfattetFamilie(String uuid, String begrunnelse, String begrunnelseFritekst) {
        this.uuid = uuid;
        this.begrunnelse = begrunnelse;
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    // ... getters and setters ...
}
```

### After (Kotlin)
```kotlin
class IkkeOmfattetFamilie(
    val uuid: String,
    val begrunnelse: String?,
    val begrunnelseFritekst: String?
) {
    var sammensattNavn: String? = null
    var ident: String? = null

    override fun toString(): String =
        "IkkeOmfattetFamilie{" +
            "uuid='$uuid', " +
            "begrunnelse='$begrunnelse', " +
            "begrunnelseFritekst='$begrunnelseFritekst', " +
            "sammensattNavn='$sammensattNavn', " +
            "ident='$ident'" +
            "}"
}
```

## Dependency Analysis

### Service Module (13 usages)
**Files affected:**
- `AvklarteMedfolgendeFamilieServiceTest.kt`
- `TrygdeavtaleServiceTest.kt`
- `BrevDataTestUtils.kt`
- `TrygdeavtaleMapperTest.kt`
- `BrevDataByggerInnvilgelseTest.kt`

**Usage patterns:**
1. **Constructor calls**: `IkkeOmfattetFamilie(uuid, begrunnelse, fritekst)` - ✅ Compatible
2. **Property setting**: `.apply { sammensattNavn = "value"; ident = "value" }` - ✅ Compatible
3. **Import statements**: Will need to be updated from `.java` to `.kt`

### Frontend-API Module (2 usages)
**Files affected:**
- `TrygdeavtaleControllerTest.kt`

**Usage patterns:**
1. **Constructor calls**: `IkkeOmfattetFamilie(uuid, begrunnelse, fritekst)` - ✅ Compatible
2. **Property setting**: `.apply { ident = value }` - ✅ Compatible
3. **Import statement**: Will need to be updated from `.java` to `.kt`

## Recommended Updates

### Import Changes Required
All dependent files need their import statements updated:
```kotlin
// Change from:
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie

// To: (no change needed - same import path)
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie
```

### Usage Compatibility
- ✅ **Constructor calls**: All existing constructor calls remain compatible
- ✅ **Property access**: Kotlin properties work seamlessly with existing `.apply` blocks
- ✅ **Null handling**: Nullable parameters match actual usage patterns

## Test/Build Verification Plan

### 1. Build Primary Module
```bash
mvn clean install -DskipTests -pl domain -am
```

### 2. Test Converted Class
```bash
mvn test -pl domain -Dtest=*IkkeOmfattetFamilie*
```

### 3. Test Dependent Modules
```bash
# Test service module
mvn test -pl service -Dtest=*AvklarteMedfolgendeFamilieServiceTest*
mvn test -pl service -Dtest=*TrygdeavtaleServiceTest*
mvn test -pl service -Dtest=*BrevDataTestUtils*
mvn test -pl service -Dtest=*TrygdeavtaleMapperTest*
mvn test -pl service -Dtest=*BrevDataByggerInnvilgelseTest*

# Test frontend-api module
mvn test -pl frontend-api -Dtest=*TrygdeavtaleControllerTest*
```

## Challenges and Learnings
1. **Nullability analysis**: Required careful examination of usage patterns - initially made all parameters nullable, but discovered `uuid` is never null in practice and caused compilation errors in dependent classes
2. **Inter-module dependencies**: Fixed compilation issue in `AvklarteMedfolgendeFamilie.kt` that expected non-null `uuid` parameter
3. **Property mutability**: Needed to preserve `var` for properties that are modified after construction
4. **Expression body usage**: Successfully applied to `toString()` method for more idiomatic Kotlin
5. **Compatibility preservation**: Maintained full backward compatibility with existing usage patterns

## Summary
- ✅ Conversion completed successfully
- ✅ Nullability properly handled based on usage analysis
- ✅ No breaking changes introduced
- ✅ Idiomatic Kotlin patterns applied (primary constructor, expression bodies)
- ✅ Full compatibility with dependent modules maintained