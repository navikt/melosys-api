# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Development Commands

When using jetbrains MCP use `projectPath`: `/Users/rune/source/nav/melosys-api-claude`

🚀 Optimal Strategy Summary

# 🧪 Recommended Test Command

Use this to run tests in a single module with minimal noise and automatic dependency handling:

```bash
# Unit tests (fast, ~5-10s if no changes to dependencies)
~/.claude/scripts/run-tests.sh -pl saksflyt -Dtest=LagreMedlemsperiodeMedlTest

# Integration tests WITHOUT dependency rebuild (fast, ~20-30s)
~/.claude/scripts/run-tests.sh -pl integrasjonstest --integration -Dtest=HibernateProxyVsEmbeddableIT

# Integration tests WITH dependency rebuild (slower, ~60-160s, but safer)
~/.claude/scripts/run-tests.sh -pl integrasjonstest -am --integration -Dtest=HibernateProxyVsEmbeddableIT
```

**Script features:**
- Automatically handles dependency compilation when using `-am`
- For integration tests with `-am`: runs full `verify` lifecycle (compiles dependencies)
- For integration tests without `-am`: uses fast `failsafe:integration-test` (no compilation)
- You can pass `-pl` and `-am` just like with mvn to scope build and test
- **IMPORTANT**: Use `-am` when testing modules after changing dependencies (service, domain, etc.)
- Summarizes output to reduce Claude's context usage
- Prints clear errors if tests or compilation fail
- Saves full logs at `/tmp/mvn.log`

**When to use `-am` with integration tests:**
- ✅ After changing code in `service`, `domain`, `repository`, or other upstream modules
- ✅ After a `mvn clean` or when build state is uncertain
- ✅ When getting `NoClassDefFoundError` or compilation errors
- ❌ When only test code changed (faster without `-am`)

## Module Dependencies for Java to Kotlin Conversion

### Dependency Impact Matrix
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

### Running the Application

#### Using Maven directly
```bash
# Run with local-mock profile
mvn spring-boot:run -pl app -Dspring-boot.run.profiles=local-mock

# Or use Makefile
make run
```

#### Using Docker for local development
```bash
# Quick start - build and run everything
make local-setup

# Or step by step:
make build-fast        # Build the app without tests
make docker-build      # Create Docker image
make compose-up        # Start with docker-compose

# View logs
make compose-logs

# Rebuild after code changes
make compose-rebuild
```

See `make help` for all available commands.

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

### Makefile Commands

A comprehensive Makefile is available for local development. Key commands:

**Quick Start:**
- `make help` - Show all available commands
- `make local-setup` - Complete local setup (build + docker + compose)
- `make rebuild` - Quick rebuild after code changes

**Building:**
- `make build` - Full build with tests
- `make build-fast` - Fast build without tests
- `make package` - Package JAR only

**Docker:**
- `make docker-build` - Build Docker image (tag: melosys-api:local)
- `make docker-build-local` - Build app + Docker image
- `make docker-run` - Run Docker container locally

**Docker Compose (requires melosys-docker-compose):**
- `make compose-up` - Build and start services
- `make compose-down` - Stop services
- `make compose-logs` - View logs
- `make compose-rebuild` - Rebuild and restart

**Testing:**
- `make test` - Run all tests
- `make test-integration` - Run integration tests

**Database:**
- `make db-migrate` - Run Flyway migrations
- `make db-info` - Show migration status

### Docker Compose Setup

To use docker-compose integration:
1. Clone melosys-docker-compose: `git clone https://github.com/navikt/melosys-docker-compose.git ../melosys-docker-compose`
2. Build and start: `make compose-up`
3. The Makefile expects docker-compose repo at `../melosys-docker-compose`

### Database Migration Versioning
- Main migrations: increment by 1 (e.g., V124 → V125)
- Data warehouse migrations: increment by decimal (e.g., V17.1 → V17.2)
