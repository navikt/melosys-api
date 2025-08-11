# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Melosys is a Spring Boot-based saksbehandlingssystem (case management system) for NAV's Medlemskap og avgift department. It handles membership in the
Norwegian National Insurance scheme (folketrygden) and processes applications for exceptions for foreign nationals working in Norway.

## Build and Development Commands

### Building the Application

```bash
mvn clean install                    # Full build with tests
mvn clean compile                    # Compile only
mvn clean install -DskipTests       # Build without tests
```

### Running Tests

```bash
mvn test                            # Unit tests only
mvn verify                          # Integration tests (requires Oracle DB)
mvn test -Dtest=ClassName          # Run specific test class
```

### Running the Application

```bash
# Local development with Docker dependencies
mvn spring-boot:run -Dspring-boot.run.profiles=local-mock

# Against Q1 environment (requires env vars)
mvn spring-boot:run -Dspring-boot.run.profiles=local-q1
```

### Database Migrations

```bash
mvn flyway:migrate                  # Apply database migrations
mvn flyway:info                     # Show migration status
```

### Environment Variables Required for Q1

- `AZURE_APP_CLIENT_ID`
- `AZURE_APP_CLIENT_SECRET`
- `melosysDB.password`
- `systemuser.password`

## Architecture Overview

Melosys follows a **modular monolith** architecture with strict layered separation:

### Module Structure

- **app**: Spring Boot application entry point and Flyway migrations
- **config**: Shared application configuration
- **domain**: JPA entities and domain objects (rich domain model)
- **feil**: Internal exception classes
- **frontend-api**: REST endpoints and GraphQL API for melosys-web frontend
- **integrasjon**: External service integrations (anti-corruption layer pattern)
- **repository**: Spring Data JPA repositories (data access layer)
- **saksflyt**: Custom workflow engine following saga pattern
- **saksflyt-api**: Workflow API definitions
- **service**: Business logic services organized by domain capability
- **sikkerhet**: Security components (ABAC, OIDC, STS)
- **soknad-altinn**: Generated POJOs from Altinn XSD schemas
- **statistikk**: A1 attestation statistics for data warehouse

### Layered Architecture Rules (enforced by ArchUnit)

- Controllers → Services only
- Services → Integrations + Repositories
- No circular dependencies between layers

### Key Patterns

- **Rich Domain Model**: Business logic in JPA entities
- **Test Data Builders**: Fluent DSL for test data creation
- **Consumer Pattern**: External service integration with resilience
- **Process Steps**: Workflow units with async processing

## Technology Stack

- **Java 17** + **Kotlin 1.8.22** (mixed codebase)
- **Spring Boot 3.3.11** with Spring Data JPA
- **Oracle Database** with Flyway migrations
- **Kafka** for messaging
- **GraphQL** + REST APIs
- **NAV token validation** for security
- **Testcontainers** for integration testing

## Testing Approach

### Test Frameworks

- **Kotest** for Kotlin tests (preferred for new tests)
- **JUnit 5** for Java tests
- **MockK** for Kotlin mocking
- **Mockito** for Java mocking

### Test Data Creation

Use the test builders with fluent DSL:

```kotlin
val fagsak = Fagsak.forTest {
    saksnummer = "12345"
    behandling {
        status = Behandlingsstatus.UNDER_BEHANDLING
    }
}
```

### Integration Tests

- Require Oracle database (use testcontainers or local DB)
- Set `USE-LOCAL-DB=true` for M1 Macs to use local database
- Located in `integrasjonstest/` module

## Database Versioning

### Migration Files

- Main migrations: `app/src/main/resources/db/migration/melosysDB/V*.sql`
- Data warehouse: `app/src/main/resources/db/migration/melosysDB/di_dvh/V*.sql`

### Versioning Rules

- New melosysDB migration: last version + 1 (e.g., V150, V151)
- New DVH migration: last version + decimal (e.g., V150.1, V150.2)

## Code Organization Patterns

### Service Layer Structure

Services are organized by business domain:

```
service/
├── aktoer/          # Actor/person management
├── avgift/          # Tax calculations
├── behandling/      # Case processing
├── brev/           # Document generation
├── ftrl/           # Social security regulations
├── oppgave/        # Task management
└── sak/            # Case management
```

### Domain Entities

Rich domain model with business logic in entities:

```kotlin
@Entity
class Fagsak {
    fun hentAktivBehandling(): Behandling { /* business logic */
    }
    fun erAvsluttet(): Boolean { /* domain rules */
    }
}
```

### External Integrations

Consumer classes encapsulate external calls:

```kotlin
@Component
class PersonConsumer(
    @Value("\${integrasjon.pdl.url}") private val pdlUrl: String
) {
    @Retryable
    fun hentPerson(ident: String): PersonDto { /* resilient call */
    }
}
```

## Code Philosophy

- Don't overcomplicate things
- When changing code, delete at least as many lines as you add

## Refactoring Rules

### Prosessinstans Construction

- Replace manual `Prosessinstans().apply { ... }` with DSL syntax `prosessinstansForTest { ... }`
- Use assignment syntax: `status = ProsessStatus.FERDIG` instead of function calls
- For nested Behandling, use the nested DSL: `behandling { ... }` instead of `behandling = Behandling.forTest { ... }`

Examples:

```kotlin
// BAD: Manual construction
val prosessinstans = Prosessinstans().apply {
    id = UUID.randomUUID()
    status = ProsessStatus.UNDER_BEHANDLING
}

// GOOD: DSL syntax
val prosessinstans = prosessinstansForTest {
    id = UUID.randomUUID()
    status = ProsessStatus.UNDER_BEHANDLING
}

// BAD: Manual with nested Behandling
val prosessinstans = Prosessinstans().apply {
    behandling = Behandling.forTest { id = 1L }
}

// GOOD: Nested DSL
val prosessinstans = prosessinstansForTest {
    behandling { id = 1L }
}

## Common Development Patterns

1. **New Features**: Start with domain model, then services, then API endpoints
2. **External Integration**: Create consumer in `integrasjon/` module with DTOs
3. **Workflow Steps**: Implement `StegBehandler` for process automation
4. **Test Data**: Use existing builders or extend them for new domain objects
5. **Database Changes**: Always create migration file with proper versioning

## Local Development Setup

1. Clone and run [melosys-docker-compose](https://github.com/navikt/melosys-docker-compose)
2. Ensure [naisdevice](https://doc.nais.io/device/install/index.html) is running
3. Use profile `local-mock` for full local development
4. Access Swagger at `localhost:8080/swagger-ui/`
