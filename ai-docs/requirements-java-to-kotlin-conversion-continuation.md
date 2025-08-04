# Requirements: Java to Kotlin Test Conversion Continuation

## Project Overview

Continue the systematic conversion of Java test files to Kotlin in the melosys-api service module. The project has already achieved significant
progress with 57 files converted out of 129 total Java test files (44.2% completion).

## Current Status

- **Total Java Test Files**: 129
- **Converted Kotlin Files**: 57 (44.2% complete)
- **Remaining Files**: 72
- **Success Rate**: 100% for all converted tests (all tests passing)

## Objectives

1. **Complete the conversion** of all remaining Java test files to Kotlin
2. **Maintain 100% test success rate** for all converted files
3. **Preserve exact test behavior** while improving code readability
4. **Establish consistent patterns** for future Kotlin test development
5. **Document conversion patterns** for team reference

## Technical Requirements

### Conversion Standards

1. **File Naming**: Use `KtTest` suffix for converted files
2. **Package Structure**: Maintain identical package structure in Kotlin
3. **Test Behavior**: Ensure 100% functional equivalence with original Java tests
4. **Code Quality**: Improve readability using Kotlin's concise syntax

### Required Patterns

#### MockK Integration

- Replace Mockito with MockK for better Kotlin integration
- Use `@MockK` annotations for dependency injection
- Implement `every { service.method(any()) } returns value` patterns
- Handle void methods with `every { service.method(any()) } just Runs`

#### Kotest Assertions

- Replace AssertJ with Kotest assertions
- Use `shouldBe` for equality assertions
- Use `shouldContainExactly` for list comparisons
- Use `shouldContainExactlyInAnyOrder` for order-independent comparisons
- Use `shouldHaveSize` for collection size assertions
- Use `shouldBeEmpty()` for empty collection assertions

#### Property Access

- Convert Java getter/setter methods to direct property access
- Handle immutable collections with `toMutableList()` when needed
- Use null-safe property access with `?.` operator

#### Import Management

- Replace Java-specific imports with Kotlin equivalents
- Use `whenMock` alias for Mockito `when` to avoid keyword conflicts
- Import Kotest matchers for assertions

## Remaining Files Analysis

### Priority Order (by complexity and dependencies)

#### Phase 1: Simple Files (0-100 lines)

1. `service/src/test/java/no/nav/melosys/service/aktoer/KontaktopplysningServiceTest.java`
2. `service/src/test/java/no/nav/melosys/service/aktoer/UtenlandskMyndighetServiceTest.java`
3. `service/src/test/java/no/nav/melosys/service/avklartefakta/AvklartefaktaDtoKonvertererTest.java`
4. `service/src/test/java/no/nav/melosys/service/avklartefakta/AvklarteMedfolgendeFamilieServiceTest.java`
5. `service/src/test/java/no/nav/melosys/service/avklartefakta/AvklarteVirksomheterServiceTest.java`

#### Phase 2: Medium Complexity Files (100-300 lines)

1. `service/src/test/java/no/nav/melosys/service/avklartefakta/AvklartefaktaServiceTest.java`
2. `service/src/test/java/no/nav/melosys/service/behandling/AngiBehandlingsresultatServiceTest.java`
3. `service/src/test/java/no/nav/melosys/service/brev/bestilling/HentBrevmottakereNorskMyndighetServiceTest.java`
4. `service/src/test/java/no/nav/melosys/service/brev/bestilling/HentMuligeBrevmottakereServiceTest.java`
5. `service/src/test/java/no/nav/melosys/service/brev/bestilling/TilBrevAdresseServiceTest.java`

#### Phase 3: Complex Files (300+ lines)

1. `service/src/test/java/no/nav/melosys/service/behandling/jobb/AvsluttArt13BehandlingServiceTest.java`
2. `service/src/test/java/no/nav/melosys/service/dokument/brev/BrevDataServiceTest.java`
3. `service/src/test/java/no/nav/melosys/service/persondata/PersondataServiceTest.java`
4. `service/src/test/java/no/nav/melosys/service/registeropplysninger/RegisteropplysningerServiceTest.java`

## Conversion Process

### Step-by-Step Approach

1. **File Selection**: Choose next file based on priority order
2. **Analysis**: Examine original Java file structure and dependencies
3. **Conversion**: Apply established patterns systematically
4. **Testing**: Run tests immediately to verify conversion
5. **Documentation**: Update progress tracking
6. **Iteration**: Fix any issues and retest

### Quality Assurance

1. **Compilation Check**: Ensure no compilation errors
2. **Test Execution**: Verify all tests pass
3. **Behavior Verification**: Confirm exact same test behavior
4. **Code Review**: Check for Kotlin best practices
5. **Documentation**: Update progress documents

## Technical Challenges

### Known Issues to Address

1. **Complex Domain Objects**: Some files have complex domain object interactions
2. **Protected/Private Access**: Some domain classes have access restrictions
3. **Enum Values**: Missing enum values in domain model
4. **Collection Handling**: Immutable collections vs mutable collections
5. **Mock Setup Complexity**: Complex service dependency mocking

### Solutions

1. **Domain Model Understanding**: Research domain classes before conversion
2. **Access Modifiers**: Use reflection or public interfaces when needed
3. **Enum Handling**: Create missing enums or use string constants
4. **Collection Conversion**: Use `toMutableList()` and reassignment
5. **Mock Patterns**: Develop reusable mock setup utilities

## Success Criteria

### Quantitative Metrics

- [ ] 100% of remaining 72 files converted
- [ ] 100% test success rate for all converted files
- [ ] 0 compilation errors
- [ ] 0 runtime errors
- [ ] Improved code readability scores

### Qualitative Metrics

- [ ] Consistent Kotlin patterns across all converted files
- [ ] Maintained test coverage
- [ ] Improved maintainability
- [ ] Better developer experience

## Risk Mitigation

### High-Risk Files

- Files with complex domain object interactions
- Files with many service dependencies
- Files with custom test utilities

### Mitigation Strategies

- Start with simple files to build momentum
- Create reusable patterns for common scenarios
- Document complex conversions for future reference
- Test each conversion immediately

## Resource Requirements

### Tools Needed

- IntelliJ IDEA or similar IDE with Kotlin support
- Maven build system
- Git for version control
- Test execution framework

### Knowledge Requirements

- Kotlin syntax and best practices
- MockK framework understanding
- Kotest assertion library
- Domain model understanding
- Spring Boot testing patterns

## Timeline

### Phase 1: Simple Files (2-3 weeks)

- Convert 20-25 simple files
- Establish patterns for medium complexity files
- Document common issues and solutions

### Phase 2: Medium Complexity (3-4 weeks)

- Convert 30-35 medium complexity files
- Refine patterns based on experience
- Address common challenges

### Phase 3: Complex Files (2-3 weeks)

- Convert remaining complex files
- Final quality assurance
- Documentation completion

### Total Estimated Time: 7-10 weeks

## Deliverables

1. **Converted Kotlin Files**: All 72 remaining Java files converted to Kotlin
2. **Updated Progress Documentation**: Complete tracking of all conversions
3. **Pattern Documentation**: Comprehensive guide for future conversions
4. **Quality Report**: Summary of conversion success and improvements
5. **Team Knowledge Transfer**: Documentation for team adoption

## Success Metrics

### Primary Metrics

- **Conversion Rate**: 100% of remaining files converted
- **Test Success Rate**: 100% of converted tests passing
- **Code Quality**: Improved readability and maintainability

### Secondary Metrics

- **Performance**: No degradation in test execution time
- **Coverage**: Maintained or improved test coverage
- **Developer Experience**: Improved development workflow

## Future Considerations

### Post-Conversion Tasks

1. **Domain Class Migration**: Consider converting domain classes to Kotlin
2. **Test Framework Standardization**: Establish consistent patterns across team
3. **Build Optimization**: Optimize for parallel test execution
4. **Documentation**: Create comprehensive testing guidelines

### Long-term Benefits

1. **Unified Codebase**: Consistent Kotlin usage across test code
2. **Improved Maintainability**: Better code organization and readability
3. **Enhanced Developer Experience**: Modern Kotlin testing patterns
4. **Knowledge Transfer**: Team expertise in Kotlin testing
