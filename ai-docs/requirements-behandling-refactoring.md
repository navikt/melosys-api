# Behandling Entity Refactoring Requirements

## Task ID: behandling-refactoring

## Overview

Refactor the `Behandling` JPA entity from Java to Kotlin with proper null safety based on database constraints, and create automated conversion of Java code to use a builder pattern.

## Current State Analysis

### Database Schema (BEHANDLING table)

From `V1.0_03__BEHANDLING.sql` and related migrations:

**NOT NULL fields (non-nullable in Kotlin):**

-   `ID` - NUMBER(19) generated as identity, primary key
-   `SAKSNUMMER` - VARCHAR2(99) not null (foreign key to FAGSAK)
-   `STATUS` - VARCHAR2(99) not null (foreign key to BEHANDLING_STATUS)
-   `BEH_TYPE` - VARCHAR2(99) not null (foreign key to BEHANDLING_TYPE)
-   `BEH_TEMA` - VARCHAR2(99) not null (foreign key to BEHANDLING_TEMA) - Added in V6.2_01
-   `BEHANDLINGSFRIST` - DATE not null - Added in V7.3_05, made NOT NULL in V7.6_05
-   `REGISTRERT_DATO` - TIMESTAMP(6) not null (from RegistreringsInfo)
-   `ENDRET_DATO` - TIMESTAMP(6) not null (from RegistreringsInfo)

**NULLABLE fields (nullable in Kotlin):**

-   `SISTE_OPPLYSNINGER_HENTET_DATO` - TIMESTAMP(6)
-   `DOKUMENTASJON_SVARFRIST_DATO` - TIMESTAMP(6)
-   `REGISTRERT_AV` - VARCHAR2(99) (from RegistreringsInfo)
-   `ENDRET_AV` - VARCHAR2(99) (from RegistreringsInfo)
-   `INITIERENDE_JOURNALPOST_ID` - VARCHAR2(99)
-   `INITIERENDE_DOKUMENT_ID` - VARCHAR2(99)
-   `OPPRINNELIG_BEHANDLING_ID` - NUMBER(19) (foreign key to BEHANDLING)
-   `OPPGAVE_ID` - VARCHAR2(10) - Added in V115

### Current Java Implementation

Located in: `domain/src/main/java/no/nav/melosys/domain/Behandling.java`

**Key characteristics:**

-   Extends `RegistreringsInfo` (provides JPA auditing fields)
-   Uses standard JavaBean setters/getters
-   Contains ~500 lines of business logic methods
-   Has relationships with: Fagsak, Saksopplysning, Behandlingsnotat, Behandlingsaarsak, MottatteOpplysninger
-   Special method: `settBehandlingsårsak()` with relationship management logic

### Current Usage Patterns

Java code typically creates Behandling instances like:

```java
Behandling behandling = new Behandling();
behandling.setFagsak(fagsak);
behandling.setStatus(status);
behandling.setType(type);
behandling.setTema(tema);
behandling.setBehandlingsfrist(frist);
// ... more setters
```

**Files likely to be affected:** ~20+ Java files based on search results

## Requirements

### 1. Kotlin Entity Creation

#### 1.1 Constructor Design

Create a primary constructor with all required fields as parameters:

-   **Required fields** (non-null): `fagsak`, `status`, `type`, `tema`, `behandlingsfrist`
-   **Optional fields** (nullable): All other fields with defaults
-   **ID field**: Default to 0 for new entities
-   **Collections**: Mutable collections for JPA relationships

#### 1.2 Field Mutability

-   **`val` for immutable fields**: `id`, `fagsak`, `type`, `tema` (core business identifiers)
-   **`var` for mutable fields**: `status`, `behandlingsfrist`, nullable fields, collections

#### 1.3 JPA Annotations

-   Preserve all existing JPA annotations exactly
-   Maintain inheritance from `RegistreringsInfo`
-   Keep entity listener: `@EntityListeners(AuditingEntityListener::class)`

#### 1.4 Business Logic Methods

-   Preserve all existing business logic methods (exactly 500 lines)
-   Convert method signatures to Kotlin idioms where appropriate
-   Maintain backward compatibility with Java callers

### 2. Builder Pattern Implementation

#### 2.1 Builder Class Structure

Add companion object with Builder class:

```kotlin
companion object {
    class Builder {
        private var fagsak: Fagsak? = null
        private var status: Behandlingsstatus? = null
        // ... all other fields

        fun medFagsak(fagsak: Fagsak?) = apply { this.fagsak = fagsak }
        // ... all other builder methods

        fun build(): Behandling {
            requireNotNull(fagsak) { "Fagsak er påkrevd for Behandling" }
            // ... validation for all required fields
            return Behandling(/* parameters */)
        }
    }
}
```

#### 2.2 Validation Rules

Builder must validate all required fields:

-   `fagsak` - required
-   `status` - required
-   `type` - required
-   `tema` - required
-   `behandlingsfrist` - required

### 3. Java Code Conversion Script

#### 3.1 Conversion Patterns

Transform from setter pattern to builder pattern:

**From:**

```java
Behandling behandling = new Behandling();
behandling.setFagsak(fagsak);
behandling.setStatus(status);
```

**To:**

```java
Behandling behandling = new Behandling.Builder()
    .medFagsak(fagsak)
    .medStatus(status)
    .build();
```

#### 3.2 Setter Mapping

-   `setFagsak(x)` → `.medFagsak(x)`
-   `setStatus(x)` → `.medStatus(x)`
-   `setType(x)` → `.medType(x)`
-   `setTema(x)` → `.medTema(x)`
-   `setBehandlingsfrist(x)` → `.medBehandlingsfrist(x)`
-   `setSisteOpplysningerHentetDato(x)` → `.medSisteOpplysningerHentetDato(x)`
-   `setDokumentasjonSvarfristDato(x)` → `.medDokumentasjonSvarfristDato(x)`
-   `setInitierendeJournalpostId(x)` → `.medInitierendeJournalpostId(x)`
-   `setInitierendeDokumentId(x)` → `.medInitierendeDokumentId(x)`
-   `setOppgaveId(x)` → `.medOppgaveId(x)`
-   `setOpprinneligBehandling(x)` → `.medOpprinneligBehandling(x)`
-   `setSaksopplysninger(x)` → `.medSaksopplysninger(x)`
-   `setBehandlingsnotater(x)` → `.medBehandlingsnotater(x)`
-   `setBehandlingsårsak(x)` → `.medBehandlingsårsak(x)`
-   `settBehandlingsårsak(x)` → `.medBehandlingsårsak(x)` (special method)
-   `setMottatteOpplysninger(x)` → `.medMottatteOpplysninger(x)`

#### 3.3 RegistreringsInfo Fields Handling

These should be commented out as they're handled by JPA auditing:

-   `setRegistrertDato(x)` → `// behandling.setRegistrertDato(x); // Handled by RegistreringsInfo`
-   `setEndretDato(x)` → `// behandling.setEndretDato(x); // Handled by RegistreringsInfo`
-   `setRegistrertAv(x)` → `// behandling.setRegistrertAv(x); // Handled by RegistreringsInfo`
-   `setEndretAv(x)` → `// behandling.setEndretAv(x); // Handled by RegistreringsInfo`

### 4. Related Entities Analysis

#### 4.1 Dependencies

-   **RegistreringsInfo**: Java superclass providing JPA auditing
-   **Fagsak**: Already converted to Kotlin
-   **Enum classes**: Behandlingsstatus, Behandlingstyper, Behandlingstema (need verification)
-   **Related entities**: Saksopplysning, Behandlingsnotat, Behandlingsaarsak, MottatteOpplysninger

#### 4.2 Relationship Types

-   `@ManyToOne`: fagsak, opprinneligBehandling
-   `@OneToMany`: saksopplysninger, behandlingsnotater
-   `@OneToOne`: behandlingsårsak, mottatteOpplysninger

### 5. Testing Requirements

#### 5.1 Compilation Tests

-   Kotlin entity compiles without errors
-   Java code can still instantiate and use the entity
-   Builder pattern works correctly

#### 5.2 JPA Tests

-   Entity can be persisted and retrieved
-   Relationships work correctly
-   Auditing fields are populated

#### 5.3 Conversion Script Tests

-   Script correctly transforms sample Java files
-   Converted code compiles and functions correctly
-   Edge cases are handled properly

## Constraints

### Technical Constraints

-   Must maintain backward compatibility with existing Java code
-   Cannot break existing JPA functionality
-   All business logic must remain identical
-   Database schema remains unchanged

### Project Structure Constraints

-   Target location: `domain/src/main/kotlin/no/nav/melosys/domain/Behandling.kt`
-   Original Java file should be removed after successful conversion
-   Update build configuration if needed

## Success Criteria

1. **Kotlin Entity**: Compiles without errors and maintains all functionality
2. **Builder Pattern**: Provides type-safe construction with validation
3. **Java Compatibility**: Existing Java code works without modification (initially)
4. **Conversion Script**: Successfully transforms Java usage patterns
5. **Tests Pass**: All existing tests continue to pass
6. **Type Safety**: Proper null safety based on database constraints

## Risks and Mitigation

### High Risk

-   **Breaking existing Java code**: Mitigated by maintaining getter/setter compatibility initially
-   **JPA mapping issues**: Mitigated by preserving all annotations exactly

### Medium Risk

-   **Performance impact**: Monitor entity loading performance
-   **Complex business logic conversion**: Careful method-by-method review required

### Low Risk

-   **Build system changes**: Standard Kotlin integration in existing Spring Boot project
