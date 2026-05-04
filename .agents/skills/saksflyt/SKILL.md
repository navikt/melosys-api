---
name: saksflyt
description: |
  Expert knowledge of the saksflyt saga pattern implementation in melosys-api.
  Use when: (1) Debugging or modifying process flows (Prosessinstans, ProsessType, ProsessSteg),
  (2) Understanding async step execution and locking mechanisms (låsReferanse),
  (3) Investigating race conditions, OptimisticLockingException, or entity overwrites,
  (4) Moving event listeners to saga steps to avoid sync/async conflicts,
  (5) Adding new process types or steps,
  (6) Deciding whether to use @DynamicUpdate on entities.
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
2. **Queue Check**: `ProsessinstansOpprettetListener` checks if saga should wait (låsReferanse) - **BEFORE_COMMIT**
3. **Execute**: `ProsessinstansBehandler.behandleProsessinstans()` runs steps in sequence - **AFTER_COMMIT**
4. **Complete/Fail**: Status set to FERDIG or FEILET, `ProsessinstansFerdigEvent` published
5. **Release**: `ProsessinstansFerdigListener` starts next waiting saga with same lock group

## Critical: The AFTER_COMMIT Misconception

> **Misconception**: "Since the saga starts AFTER_COMMIT, it sees committed data."
>
> **Reality**: AFTER_COMMIT guarantees DB commit, NOT fresh Java objects.

The saga receives the **same Java object** from the HTTP transaction. The `behandling` inside
is a **detached entity** with potentially stale data. See **[stale-reference-problem.md](references/stale-reference-problem.md)**.

## Common Pitfalls

The sync/async split at transaction boundaries causes race conditions:

| Issue | Symptom | Solution |
|-------|---------|----------|
| Stale object reference | Silent corruption | Reload Prosessinstans in AFTER_COMMIT |
| Event listener + saga conflict | `OptimisticLockingException` | Move listener to saga step |
| Entity loaded across TX | Silent data corruption | Add `@DynamicUpdate` to entity |
| @Lob in equals/hashCode | Unstable Set membership | Use id-based equals |

**See [Anti-Patterns](references/anti-patterns.md)** and **[stale-reference-problem.md](references/stale-reference-problem.md)** for details.

## Detailed Documentation

- **[Architecture](references/architecture.md)**: Deep dive into component interactions
- **[Flow Definitions](references/flow-definitions.md)**: All process types and their steps
- **[Concurrency Control](references/concurrency.md)**: Locking mechanism, låsReferanse, and entity-level race conditions
- **[Anti-Patterns](references/anti-patterns.md)**: Sync/async race conditions and how to avoid them
- **[Stale Reference Problem](references/stale-reference-problem.md)**: AFTER_COMMIT misconception and investigation guide
- **[Common Patterns](references/patterns.md)**: Debugging, adding steps, error handling

### Project-Level Documentation

Investigation reports in `docs/`:
- `docs/prosessinstans-reload-fix.md` - Proposed fix for stale reference problem
- `docs/race-condition-analysis.md` - Full sync/async split analysis
- `docs/saksopplysningkilde-equals-hashcode-fix.md` - @Lob equals/hashCode issue
