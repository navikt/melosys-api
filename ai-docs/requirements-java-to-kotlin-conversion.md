# Requirements: Java to Kotlin Test Conversion

## Overview

Convert all Java test files in the service module to Kotlin following best practices. The service module contains approximately 129 Java test files
that need to be converted.

## Goals

1. Convert all Java test files to Kotlin
2. Apply Kotlin best practices consistently
3. Maintain test functionality and coverage
4. Use Kotest and MockK patterns
5. Follow the established conversion patterns from BehandlingServiceKtTest

## Conversion Requirements

### Kotlin Best Practices to Apply

1. **Expression Bodies**: Use expression bodies where possible (`fun name() = expression`)
2. **Apply Function**: Use `apply` for object initialization and configuration
3. **Named Parameters**: Use named parameters for Kotlin data classes (but not for Java classes)
4. **Companion Object Position**: Move companion objects to the end of the class
5. **Kotest Assertions**: Replace JUnit assertions with Kotest matchers
6. **MockK Patterns**: Use `@RelaxedMockK`, `every { }`, `verify { }`

### Import Requirements

- Replace Mockito imports with MockK
- Add Kotest assertion imports
- Add domain enum imports (Behandlingstyper.*, Behandlingstema.*, etc.)

### Test Framework Migration

- Keep JUnit `@Test` annotations
- Replace Mockito annotations with MockK equivalents
- Convert `when().thenReturn()` to `every { } returns`
- Convert `verify()` to `verify { }`
- Replace `assertThat` with Kotest matchers

### File Naming Convention

- Convert `.java` files to `.kt` files
- Add `KtTest` suffix to distinguish from original Java files
- Keep original Java files for reference

## Scope

### Directories to Process

- `service/src/test/java/no/nav/melosys/service/` and all subdirectories
- Focus on test files (files ending with `Test.java`)
- Exclude utility/stub files (like `SaksbehandlingDataFactory.java`)

### File Types to Convert

- All `*Test.java` files
- All test utility classes that are part of test suites

### Files to Exclude

- Non-test files (factories, stubs, etc.)
- Already converted Kotlin test files

## Technical Requirements

### Conversion Process

1. Read Java test file
2. Convert to Kotlin syntax
3. Apply MockK patterns
4. Apply Kotest assertions
5. Apply Kotlin best practices
6. Fix compilation errors
7. Run tests to verify functionality
8. Check if we classes used in test is kotlin data classes, and then us named arguments for better readability
9. Apply refactoring for Kotlin idioms

### Error Handling

- Handle compilation errors systematically
- Fix import issues
- Resolve MockK vs Mockito differences
- Handle ClassCastException issues if they arise

### Quality Assurance

- All converted tests must compile successfully
- All converted tests must pass
- Maintain original test coverage
- Preserve test logic and assertions

## Success Criteria

1. All 129 Java test files converted to Kotlin
2. All converted tests pass
3. Code follows Kotlin best practices
4. No regression in test coverage
5. Improved readability and maintainability

## Constraints

- Must maintain backward compatibility with existing test infrastructure
- Must preserve test functionality exactly
- Cannot change test logic or expected outcomes
- Must work with existing Maven build system
