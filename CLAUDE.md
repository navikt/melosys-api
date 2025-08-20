# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Development Commands

### Build and Test
```bash
# Full build with tests
mvn clean verify

# Build without tests
mvn clean package -DskipTests

# Run only unit tests
mvn test

# Run integration tests
mvn verify -DskipUnitTests

# Build with parallel threads for faster execution
mvn clean verify --threads=8
```

### Running the Application
```bash
# Run with local-mock profile (uses docker-compose)
mvn spring-boot:run -Dspring.profiles.active=local-mock

# Run with local-q1 profile (requires environment variables)
mvn spring-boot:run -Dspring.profiles.active=local-q1
```

### Running Single Tests
```bash
# Run a specific test class
mvn test -Dtest=ClassName

# Run a specific test method
mvn test -Dtest=ClassName#methodName

# Run tests matching a pattern
mvn test -Dtest=*ServiceTest
```

### Database Migrations
```bash
# Run Flyway migrations
mvn flyway:migrate

# Clean database (WARNING: drops all objects)
mvn flyway:clean
```

## High-Level Architecture

### Modular Monolith Structure
Melosys-api follows a modular monolith architecture with clear separation of concerns:

- **app**: Application entry point with Spring Boot configuration and Flyway migrations
- **frontend-api**: REST endpoints exposed to melosys-web frontend
- **service**: Business logic layer containing core services
- **repository**: Data access layer with JPA repositories
- **domain**: Domain models and entities (mixed Java/Kotlin)
- **saksflyt**: Saga pattern implementation for orchestrating complex processes
- **integrasjon**: External integrations (SOAP/REST/GraphQL) with NAV services
- **sikkerhet**: Security concerns (ABAC, OIDC, STS)
- **statistikk**: Statistics production for data warehouse

### Key Architectural Decisions

1. **Kotlin Migration**: The codebase is gradually migrating from Java to Kotlin. New code should be written in Kotlin, and existing Java code should be converted when touched.

2. **Saga Pattern**: Complex business processes use the saksflyt module which implements the saga pattern for managing distributed transactions across multiple services.

3. **Oracle Database**: Uses Oracle DB with Flyway for migrations. Test containers are used for integration tests, but ARM-based Macs require `USE-LOCAL-DB=true` environment variable.

4. **Spring Boot 3.3**: Modern Spring Boot with Jakarta EE namespace

### Domain Context
Melosys handles membership applications in the Norwegian National Insurance Scheme (folketrygden) and manages exceptions for foreign nationals working in Norway. It integrates with EU/EEA systems through EESSI.

### Testing Strategy
- Unit tests with JUnit 5 and Kotest
- Integration tests with Spring Boot Test
- Architecture tests with ArchUnit
- Test containers for database testing (Oracle)

### External Dependencies
- PDL (Person Data Service) for person information
- EESSI/EUX for EU/EEA coordination
- Oppgave API for task management
- SAF/Joark for document archiving
- Various NAV internal services

### Security
- Azure AD (OIDC) for authentication
- ABAC for authorization
- STS for legacy SOAP services

### Environment Configuration
- Profiles: local-mock, local-q1, local-q2, nais
- Environment-specific properties in `application-{profile}.properties`
- Kubernetes deployment with NAIS platform

## Important Notes for Development

### When Converting Java to Kotlin
- Ensure JPA compatibility (use `open class`, not `data class`)
- Handle nullable fields explicitly
- Maintain bidirectional JPA relationships
- Preserve existing business logic exactly
- Update tests to use Kotlin idioms

### Database Migration Versioning
- Main migrations: increment by 1 (e.g., V124 → V125)
- Data warehouse migrations: increment by decimal (e.g., V17.1 → V17.2)

### Integration Tests on ARM Macs
Set `USE-LOCAL-DB=true` to use local Oracle database instead of test containers.
