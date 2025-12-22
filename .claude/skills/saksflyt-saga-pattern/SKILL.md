---
name: saksflyt-saga-pattern
description: |
  Expert knowledge of the saksflyt saga pattern implementation in melosys-api.
  Use when: (1) Debugging or modifying process flows (Prosessinstans, ProsessType, ProsessSteg),
  (2) Understanding async step execution and locking mechanisms (låsReferanse),
  (3) Investigating race conditions or concurrency issues in saksflyt,
  (4) Adding new process types or steps, (5) Understanding how MEDL updates, SED handling,
  or other multi-step operations are orchestrated.
---

# Saksflyt Saga Pattern

The `saksflyt` and `saksflyt-api` modules implement a saga pattern for orchestrating multi-step
business processes asynchronously in melosys-api.

## Quick Reference

### Module Structure
```
saksflyt-api/    # Interfaces, domain classes, and service facade
├── ProsessinstansService.java   # Entry point for creating sagas
├── domain/
│   ├── Prosessinstans.kt        # Saga state entity
│   ├── ProsessType.java         # ~45 process type definitions
│   ├── ProsessSteg.java         # ~70 step definitions
│   ├── ProsessStatus.java       # KLAR, UNDER_BEHANDLING, PÅ_VENT, FERDIG, FEILET
│   ├── ProsessDataKey.java      # Data keys stored in saga
│   └── LåsReferanse*.kt         # Concurrency control

saksflyt/        # Execution engine and step implementations
├── ProsessinstansBehandler.java     # Main saga orchestrator
├── ProsessinstansOpprettetListener  # Event listener, starts sagas
├── ProsessinstansFerdigListener.kt  # Releases waiting sagas
├── ProsessinstansBehandlerDelegate.kt  # Locking logic
├── prosessflyt/
│   ├── ProsessFlyt.kt               # Flow definition (steps list)
│   └── ProsessflytDefinisjon.kt     # All flow mappings
└── steg/                            # Step implementations
    ├── StegBehandler.java           # Step interface
    ├── medl/                        # MEDL integration steps
    ├── sed/                         # SED/EESSI steps
    ├── brev/                        # Letter sending steps
    └── ...
```

### Creating a New Saga
```java
// In any service class, inject ProsessinstansService
prosessinstansService.opprettProsessinstansIverksettVedtakEos(behandling);
```

### Step Implementation Pattern
```java
@Component
public class MyStep implements StegBehandler {
    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.MY_STEP;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)  // Each step has own transaction
    public void utfør(Prosessinstans prosessinstans) {
        // Access data: prosessinstans.getData(ProsessDataKey.SOME_KEY)
        // Access behandling: prosessinstans.getBehandling()
        // Do work...
    }
}
```

## Key Concepts

| Concept | Description |
|---------|-------------|
| **Prosessinstans** | Saga state - tracks type, status, current step, data |
| **ProsessType** | Defines which saga to run (e.g., `IVERKSETT_VEDTAK_EOS`) |
| **ProsessFlyt** | Ordered list of steps for a process type |
| **ProsessSteg** | Individual step in a flow |
| **StegBehandler** | Step implementation (one per ProsessSteg) |
| **låsReferanse** | Concurrency lock key (e.g., RINA case ID) |

## Execution Flow

1. **Create**: `ProsessinstansService` creates `Prosessinstans`, publishes `ProsessinstansOpprettetEvent`
2. **Queue Check**: `ProsessinstansOpprettetListener` checks if saga should wait (låsReferanse)
3. **Execute**: `ProsessinstansBehandler.behandleProsessinstans()` runs steps in sequence
4. **Complete/Fail**: Status set to FERDIG or FEILET, `ProsessinstansFerdigEvent` published
5. **Release**: `ProsessinstansFerdigListener` starts next waiting saga with same lock group

## Detailed Documentation

- **[Architecture](references/architecture.md)**: Deep dive into component interactions
- **[Flow Definitions](references/flow-definitions.md)**: All process types and their steps
- **[Concurrency Control](references/concurrency.md)**: Locking mechanism and race condition handling
- **[Common Patterns](references/patterns.md)**: Debugging, adding steps, error handling
