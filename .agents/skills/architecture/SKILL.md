---
name: architecture
description: |
  Modular monolith architecture overview for melosys-api. Use when needing to understand module structure, dependencies between modules, where to place new code, or when changes in one module might affect others. Triggers: "architecture", "module dependencies", "where should I put", "which modules", "dependency graph".
---

# Melosys-API Architecture

Modular monolith with 12 main modules. Understanding the dependency graph is essential for:
- Knowing where to place new code
- Understanding what tests to run after changes
- Avoiding circular dependencies

## Module Dependency Graph

```
                              ┌─────────────┐
                              │     app     │ ← Entry point (Spring Boot)
                              └──────┬──────┘
           ┌──────────────┬─────────┼─────────┬──────────────┐
           │              │         │         │              │
           ▼              ▼         ▼         ▼              ▼
    ┌──────────┐   ┌──────────┐ ┌──────┐ ┌─────────┐  ┌──────────┐
    │frontend- │   │ saksflyt │ │config│ │sikkerhet│  │statistikk│
    │   api    │   └────┬─────┘ └───┬──┘ └────┬────┘  └────┬─────┘
    └────┬─────┘        │           │         │            │
         │              │           │         │            │
         └──────┬───────┴───────────┴─────────┘            │
                │                                          │
                ▼                                          │
         ┌──────────┐                                      │
         │ service  │◄─────────────────────────────────────┘
         └────┬─────┘
              │
    ┌─────────┼─────────┬────────────┐
    │         │         │            │
    ▼         ▼         ▼            ▼
┌────────┐┌────────┐┌──────────┐┌───────────┐
│reposit-││integra-││saksflyt- ││   domain  │
│  ory   ││  sjon  ││   api    │└─────┬─────┘
└───┬────┘└───┬────┘└────┬─────┘      │
    │         │          │            │
    └─────────┴──────────┴────────────┘
                         │
                         ▼
                    ┌─────────┐
                    │  feil   │ ← Base exception module
                    └─────────┘
```

## Module Descriptions

| Module | Purpose | Key Contents |
|--------|---------|--------------|
| **app** | Spring Boot entry point | Main class, Flyway migrations, profiles |
| **frontend-api** | REST endpoints for melosys-web | Controllers, DTOs, GraphQL |
| **saksflyt** | Saga orchestration | Saga implementations, step handlers |
| **saksflyt-api** | Saga contracts/events | Event classes, saga interfaces |
| **service** | Business logic | Core services, validation, calculations |
| **repository** | Data access | JPA repositories, queries |
| **integrasjon** | External systems | PDL, EESSI, Joark, Kafka consumers |
| **domain** | JPA entities | Entities, embeddables, converters |
| **sikkerhet** | Security | OIDC, ABAC, token handling |
| **config** | Cross-cutting config | MDC, Unleash, ShedLock, metrics |
| **statistikk** | DVH reporting | Event listeners, statistics producers |
| **feil** | Exception hierarchy | TekniskException, FunksjonellException |

## Dependency Rules

**Allowed dependencies** (downward only):
```
app → frontend-api, saksflyt, config, sikkerhet, statistikk, domain
frontend-api → service, repository, domain, sikkerhet, feil
saksflyt → service, saksflyt-api, domain, config, feil
service → repository, integrasjon, saksflyt-api, domain, config, feil
statistikk → service, domain
repository → domain
integrasjon → domain, sikkerhet, config, feil, soknad-altinn
saksflyt-api → domain, config
domain → feil
sikkerhet → feil
config → sikkerhet
feil → (none)
```

**Forbidden**: Circular dependencies, upward dependencies

## Impact Analysis

When modifying code, test dependent modules:

| If you change... | Test these modules |
|------------------|-------------------|
| `feil` | domain, service, saksflyt, frontend-api, integrasjon, sikkerhet |
| `domain` | repository, service, saksflyt, frontend-api, integrasjon, saksflyt-api |
| `repository` | service, frontend-api |
| `integrasjon` | service |
| `saksflyt-api` | service, saksflyt |
| `service` | saksflyt, frontend-api, statistikk |
| `sikkerhet` | integrasjon, frontend-api, config |
| `config` | service, saksflyt, saksflyt-api, integrasjon |

## Placement Guidelines

| Type of code | Module |
|--------------|--------|
| REST endpoint | frontend-api |
| Business logic | service |
| JPA entity | domain |
| Repository interface | repository |
| External API client | integrasjon |
| Saga/workflow | saksflyt (impl), saksflyt-api (contract) |
| Security filter | sikkerhet |
| Kafka consumer | integrasjon |
| Kafka producer | service or integrasjon |
| Statistics listener | statistikk |
| Flyway migration | app |
| Cross-cutting config | config |
| Custom exception | feil |

## Data Flow Examples

### REST Request Flow
```
frontend-api → service → repository → domain
                     ↘→ integrasjon (external calls)
```

### Saga Flow
```
frontend-api → service → saksflyt-api (event)
                              ↓
                         saksflyt (saga execution)
                              ↓
                         service (steps)
```

### Kafka Event Flow
```
integrasjon (consumer) → service → domain/repository
service → integrasjon (producer)
```

## Test Module

| Module | Type |
|--------|------|
| `integrasjonstest` | Integration tests with Spring context |
| `arkitektur` | ArchUnit architecture tests |

Run integration tests with: `mvn verify -pl integrasjonstest`
