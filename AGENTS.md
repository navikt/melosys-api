# AGENT.md

This file provides guidance to AI coding agents working with code in this repository.

## Skills

Domain-specific skills are available in `.agents/skills/`. Each skill contains a `SKILL.md` with expert knowledge about a specific area of the codebase.

## Common Development Commands

### Recommended Test Command

Use this to run tests in a single module with minimal noise and automatic dependency handling:

```bash
# Unit tests (fast, ~5-10s if no changes to dependencies)
scripts/run-tests.sh -pl saksflyt -Dtest=LagreMedlemsperiodeMedlTest

# Integration tests WITHOUT dependency rebuild (fast, ~20-30s)
scripts/run-tests.sh -pl integrasjonstest --integration -Dtest=HibernateProxyVsEmbeddableIT

# Integration tests WITH dependency rebuild (slower, ~60-160s, but safer)
scripts/run-tests.sh -pl integrasjonstest -am --integration -Dtest=HibernateProxyVsEmbeddableIT
```

**Script features:**
- Automatically handles dependency compilation when using `-am`
- For integration tests with `-am`: runs full `verify` lifecycle (compiles dependencies)
- For integration tests without `-am`: uses fast `failsafe:integration-test` (no compilation)
- You can pass `-pl` and `-am` just like with mvn to scope build and test
- **IMPORTANT**: Use `-am` when testing modules after changing dependencies (service, domain, etc.)
- Summarizes output to reduce context usage
- Prints clear errors if tests or compilation fail
- Saves full logs at `/tmp/mvn.log`

**When to use `-am` with integration tests:**
- After changing code in `service`, `domain`, `repository`, or other upstream modules
- After a `mvn clean` or when build state is uncertain
- When getting `NoClassDefFoundError` or compilation errors
- NOT needed when only test code changed (faster without `-am`)

### Running the Application

```bash
# Run with local-mock profile
mvn spring-boot:run -pl app -Dspring-boot.run.profiles=local-mock

# Or use Makefile
make run
```

See `make help` for all available Makefile commands.

## Module Dependencies for Java to Kotlin Conversion

When converting Java to Kotlin in a module, test these dependent modules:

- `feil` → `domain`, `service`, `saksflyt`, `frontend-api`, `integrasjon`, `sikkerhet`
- `config` → `service`, `saksflyt`, `saksflyt-api`, `sikkerhet`, `app`
- `sikkerhet` → `integrasjon`, `frontend-api`, `config`, `app`
- `domain` → `repository`, `service`, `saksflyt`, `frontend-api`, `integrasjon`, `saksflyt-api`, `app`
- `repository` → `service`, `frontend-api`
- `integrasjon` → `service`
- `saksflyt-api` → `service`, `saksflyt`
- `service` → `saksflyt`, `frontend-api`, `statistikk`
- `saksflyt` → `app`
- `frontend-api` → `app`
- `statistikk` → `app`

## Git Commit Rules

**IMPORTANT: Always ask before amending or pushing commits!**

- Never amend commits without asking first
- Never force push without asking first
- Never push commits without asking first
- Create new commits rather than amending existing ones
- Stage only relevant files, not `git add -A`
- Commit titles max 72 characters
- Commit messages in Norwegian

## High-Level Architecture

### Modular Monolith Structure

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
3. **Oracle Database**: Uses Oracle DB with Flyway for migrations.
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

### Database Migration Versioning

- Main migrations: increment by 1 (e.g., V124 → V125)
- Data warehouse migrations: increment by decimal (e.g., V17.1 → V17.2)
