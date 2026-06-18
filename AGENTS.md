# AGENTS.md

Domain-specific skills are in `.agents/skills/` (each has a `SKILL.md`).

## Commands

```bash
# Unit tests
scripts/run-tests.sh -pl <module> -Dtest=TestClassName

# Integration tests (fast, no dependency rebuild)
scripts/run-tests.sh -pl integrasjonstest --integration -Dtest=TestClassIT

# Integration tests (with dependency rebuild — use after changing upstream modules)
scripts/run-tests.sh -pl integrasjonstest -am --integration -Dtest=TestClassIT

# Run application
make run
```

Use `-am` after changing code in upstream modules (service, domain, repository, etc.).
Full logs saved at `/tmp/mvn.log`.

## Git Rules

- **Never** amend, force push, or push without asking first
- Stage only relevant files (not `git add -A`)
- Commit titles max 72 chars, messages in Norwegian

## Key Conventions

- Write new code in **Kotlin**; convert Java when touched
- Spring Boot, Jakarta EE, Oracle DB with Flyway
- Architecture details: see `.agents/skills/architecture/SKILL.md`
- Module dependency graph for Kotlin conversion: see `.agents/skills/architecture/SKILL.md`
