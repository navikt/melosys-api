# Dev-data

Engangs-data for å fylle opp lokale miljøer med realistiske eksempler — ikke ment
for prod-migrasjoner.

## tekstblokker-seed.json

12 eksempel-tekstblokker og brevmaler (mix av korte standardparagrafer og fullstendige
brevmaler med headings, lister, klagerett og hilsen) for testing av admin-siden og
Send brev-popoveren.

### Bruk

```bash
export MELOSYS_TOKEN="<bearer token mot lokal API>"
./scripts/seed-tekstblokker.sh
```

Eller med eksplisitt base-url:
```bash
./scripts/seed-tekstblokker.sh http://localhost:8080/melosys/api
```

### Forutsetninger

- `melosys.tekstblokker` toggle aktivert lokalt (sjekk Unleash-config)
- Innlogget bruker har gyldig bearer-token
- `jq` og `curl` installert

Se ADR-0001 i `doc/architecture/decisions/` for arkitekturbeslutninger rundt
tekstblokk-modulen.
