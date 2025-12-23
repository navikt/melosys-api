# Skills Development Progress

This document tracks the progress of Claude Code skills development for melosys-api.

## Existing Skills (28)

| # | Skill | Status | Description | References |
|---|-------|--------|-------------|------------|
| 1 | `behandling` | Done | Case treatment lifecycle and status transitions | kodeverk.md, debugging.md |
| 2 | `behandlingsresultat` | Done | Case results, periods, and outcomes | periods.md, result-types.md, trygdeavgift.md |
| 3 | `brev` | Done | Letter generation (melosys-dokgen) | letter-types.md, templates.md, debugging.md |
| 4 | `database` | Done | Oracle schema, entities, and tables | core-tables.md, period-tables.md, kodeverk.md, dvh-tables.md |
| 5 | `docker-build-push` | Done | Build and push Docker images to GCR | - |
| 6 | `fagsak` | Done | Case (fagsak) management and lifecycle | actors.md, debugging.md, status.md |
| 7 | `journalforing` | Done | Document archiving (Joark/SAF) | debugging.md, joark-api.md, saf.md |
| 8 | `kodeverk` | Done | Code registry and enum management | combinations.md, debugging.md, enums.md |
| 9 | `medl` | Done | MEDL membership registry integration | debugging.md, enums.md, mapping.md |
| 10 | `oppgave` | Done | Task management (Gosys integration) | debugging.md, mapping.md, task-types.md |
| 11 | `person` | Done | PDL person data integration | pdl-api.md, data-models.md, debugging.md |
| 12 | `arbeidsgiver` | Done | EREG/AAREG employer/employment data | ereg-api.md, aareg-api.md, debugging.md |
| 13 | `rule-generator-skill` | Done | Meta-skill for creating rules/guidelines | README.md |
| 14 | `saksflyt` | Done | Saga pattern and process orchestration | architecture.md, flow-definitions.md, concurrency.md, patterns.md |
| 15 | `sed` | Done | SED documents (EESSI/BUC handling) | buc-types.md, debugging.md, sed-types.md |
| 16 | `vedtak` | Done | Decisions/rulings and iverksetting | debugging.md, race-conditions.md, vedtak-types.md |
| 17 | `trygdeavgift` | Done | Social insurance charge calculation | beregning.md, fakturering.md, aarsavregning.md, debugging.md |
| 18 | `lovvalg` | Done | Law determination / applicable legislation | articles.md, debugging.md |
| 19 | `ftrl` | Done | Folketrygdloven processing flows | debugging.md |
| 20 | `medlemskap` | Done | Membership determination (pliktig/frivillig) | types.md, periods.md, medl-sync.md, debugging.md |
| 21 | `vilkaar` | Done | Requirements/conditions evaluation | structure.md, avklartefakta.md, evaluation.md, debugging.md |
| 22 | `eessi-eux` | Done | EUX/RINA integration for EESSI | buc-types.md, sed-types.md, api.md, debugging.md |
| 23 | `altinn-soknad` | Done | Altinn A1 application form processing | form-structure.md, mapping.md, debugging.md |
| 24 | `inntekt-skatt` | Done | Income lookup and tax events integration | debugging.md |
| 25 | `statistikk` | Done | A1 statistics publishing to Kafka/DVH | debugging.md |
| 26 | `trygdeavtaler` | Done | Bilateral social security agreements | debugging.md |
| 27 | `eos-forordning` | Done | EU/EEA regulation 883/2004 and 987/2009 | debugging.md |
| 28 | `kafka` | Done | Kafka event streaming patterns | debugging.md |

## Planned Skills

### Priority 1: Core Business Domains

| # | Skill | Priority | Status | Description | Key Areas |
|---|-------|----------|--------|-------------|-----------|
| 17 | `trygdeavgift` | HIGH | **Done** | Social insurance charge calculation and collection | Avgift calculation, rates, årsavregning, fakturering, OEBS integration |
| 18 | `lovvalg` | HIGH | **Done** | Law determination / applicable legislation | Article selection (11, 12, 13, 16), LA_BUC processing, A1 attestation |
| 19 | `ftrl` | HIGH | **Done** | Folketrygdloven processing flows | §2-5 to §2-13, yrkesaktiv/ikke-yrkesaktiv/pensjonist flows |
| 20 | `medlemskap` | HIGH | **Done** | Membership determination logic | Pliktig/frivillig membership, periods, trygdedekning |
| 21 | `vilkaar` | HIGH | **Done** | Requirements/conditions evaluation | Inngangsvilkår, vilkårsvurdering, avklartefakta |

### Priority 2: Integrations & External Systems

| # | Skill | Priority | Status | Description | Key Areas |
|---|-------|----------|--------|-------------|-----------|
| 22 | `eessi-eux` | HIGH | **Done** | EUX/RINA integration for EESSI | BUC lifecycle, SED sending/receiving, institusjonskatalog |
| 23 | `altinn-soknad` | MEDIUM | **Done** | Altinn form submissions and processing | soknad-altinn module, SoeknadMapper, form validation |
| 24 | `inntekt-skatt` | MEDIUM | **Done** | Income lookup and tax events | Inntektskomponenten, skattehendelser, årsavregning |
| 25 | `statistikk` | MEDIUM | **Done** | A1 statistics publishing to Kafka | UtstedtA1Service, Kafka producer, DVH |

### Priority 3: Agreements & Regulations

| # | Skill | Priority | Status | Description | Key Areas |
|---|-------|----------|--------|-------------|-----------|
| 26 | `trygdeavtaler` | MEDIUM | **Done** | Bilateral social security agreements | AU, CA, GB, US, and 15 other countries |
| 27 | `eos-forordning` | MEDIUM | **Done** | EU/EEA regulation 883/2004 and 987/2009 | Articles, personal scope, coordination rules |

### Priority 4: Technical Infrastructure

| # | Skill | Priority | Status | Description | Key Areas |
|---|-------|----------|--------|-------------|-----------|
| 28 | `kafka` | MEDIUM | **Done** | Event streaming patterns | Topics, producers, consumers, message schemas |
| 29 | `testing` | MEDIUM | Pending | Testing patterns and strategies | Unit tests, integration tests (Testcontainers), ArchUnit |
| 30 | `security` | LOW | Pending | Authentication and authorization | ABAC, Azure AD/OIDC, TokenX, STS |
| 31 | `flyway-migration` | LOW | Pending | Database migration patterns | Versioning, rollback strategies, data migrations |
| 32 | `frontend-api` | LOW | Pending | REST endpoint patterns | Controller patterns, DTOs, validation |

## Skill Detail Templates

### trygdeavgift (Priority: HIGH)

**Purpose**: Expert knowledge of social insurance charge (trygdeavgift) domain

**Use Cases**:
1. Understanding avgift calculation for different member types
2. Debugging årsavregning (annual reconciliation) processing
3. Understanding fakturering flow to OEBS
4. Investigating avgiftssatser and rules per year
5. Understanding skatteforhold and inntektskilder
6. Debugging trygdeavgiftsperiode generation

**Key Files/Packages**:
- `service/src/main/kotlin/.../avgift/` - Core avgift services
- `service/src/main/kotlin/.../avgift/aarsavregning/` - Annual reconciliation
- `service/src/main/kotlin/.../avgift/fakturering/` - Billing integration
- `domain/src/main/kotlin/.../domain/avgift/` - Domain entities

**Reference Documents Needed**:
- `satser.md` - Avgift rates by year and category
- `beregning.md` - Calculation rules and 25% rule
- `fakturering.md` - OEBS integration and billing flow
- `aarsavregning.md` - Annual reconciliation process
- `debugging.md` - Common issues and SQL queries

---

### lovvalg (Priority: HIGH)

**Purpose**: Expert knowledge of law determination (lovvalg) for EU/EEA

**Use Cases**:
1. Understanding which article applies (11, 12, 13, 16)
2. Debugging LA_BUC processing
3. Understanding A1 attestation generation
4. Investigating lovvalgsperiode and unntaksperiode
5. Understanding automatic vs manual lovvalg determination
6. Debugging lovvalgsbeslutning status transitions

**Key Files/Packages**:
- `service/src/main/kotlin/.../dokument/brev/` - A1 and SED generation
- `domain/src/main/kotlin/.../domain/lovvalg/` - Lovvalg entities
- `saksflyt/src/.../steg/sed/` - SED processing steps

**Reference Documents Needed**:
- `articles.md` - Article 11-16 explanation and conditions
- `la-buc.md` - LA_BUC types and SED flows
- `a1-attest.md` - A1 generation and content
- `debugging.md` - Common lovvalg issues

---

### ftrl (Priority: HIGH)

**Purpose**: Expert knowledge of Folketrygdloven (National Insurance Act) processing

**Use Cases**:
1. Understanding FTRL bestemmelser (§2-5 to §2-13)
2. Debugging yrkesaktiv/ikke-yrkesaktiv/pensjonist flows
3. Understanding trygdedekning options
4. Investigating vilkår for each bestemmelse
5. Understanding medlemskapsperiode generation logic

**Key Files/Packages**:
- `service/src/main/kotlin/.../ftrl/` - FTRL services
- `service/src/main/kotlin/.../ftrl/bestemmelse/` - Bestemmelse definitions
- `service/src/main/kotlin/.../ftrl/medlemskapsperiode/` - Period logic

**Reference Documents Needed**:
- `bestemmelser.md` - All FTRL paragraphs and their conditions
- `trygdedekning.md` - Coverage types and combinations
- `flows.md` - Processing flows per arbeidssituasjon
- `debugging.md` - Common FTRL issues

---

### medlemskap (Priority: HIGH)

**Purpose**: Expert knowledge of membership determination in folketrygden

**Use Cases**:
1. Understanding pliktig vs frivillig membership
2. Debugging medlemskapsperiode generation
3. Understanding trygdedekning combinations
4. Investigating MEDL synchronization
5. Understanding membership status transitions

**Key Files/Packages**:
- `service/src/main/kotlin/.../ftrl/medlemskapsperiode/` - Membership logic
- `domain/src/main/kotlin/.../domain/medlemskap/` - Membership entities
- `integrasjon/.../medl/` - MEDL integration

**Reference Documents Needed**:
- `types.md` - Pliktig/frivillig/unntak types
- `periods.md` - Period management and validation
- `medl-sync.md` - MEDL synchronization
- `debugging.md` - Common membership issues

---

### vilkaar (Priority: HIGH)

**Purpose**: Expert knowledge of vilkårsvurdering (requirements evaluation)

**Use Cases**:
1. Understanding inngangsvilkår structure
2. Debugging vilkårsvurdering results
3. Understanding avklartefakta collection
4. Investigating why a vilkår fails
5. Adding new vilkår to bestemmelser

**Key Files/Packages**:
- `service/src/main/kotlin/.../ftrl/bestemmelse/vilkaar/` - Vilkår definitions
- `service/src/main/kotlin/.../avklartefakta/` - Avklarte fakta services
- `service/src/main/kotlin/.../vilkaar/` - Vilkår service

**Reference Documents Needed**:
- `structure.md` - Vilkår hierarchy and types
- `avklartefakta.md` - How facts are collected
- `evaluation.md` - Vilkår evaluation logic
- `debugging.md` - Debugging vilkår issues

---

### eessi-eux (Priority: HIGH)

**Purpose**: Expert knowledge of EUX/RINA integration for EESSI

**Use Cases**:
1. Understanding BUC lifecycle management
2. Debugging SED sending/receiving
3. Understanding institusjonskatalog lookup
4. Investigating EUX API errors
5. Understanding RINA case synchronization

**Key Files/Packages**:
- `integrasjon/.../eessi/` - EUX integration
- `service/src/main/kotlin/.../eessi/` - EESSI services
- `saksflyt/src/.../steg/sed/` - SED saga steps

**Reference Documents Needed**:
- `eux-api.md` - EUX API endpoints and contracts
- `buc-lifecycle.md` - BUC states and transitions
- `institution-catalog.md` - Institution lookup
- `debugging.md` - Common EUX issues

---

## Progress Tracking

### Phase 1: Core Business (Target: Q1 2025)
- [x] trygdeavgift (Dec 2024)
- [x] lovvalg (Dec 2024)
- [x] ftrl (Dec 2024)
- [x] medlemskap (Dec 2024)
- [x] vilkaar (Dec 2024)

### Phase 2: Integrations (Target: Q2 2025)
- [x] eessi-eux (Dec 2024)
- [x] altinn-soknad (Dec 2024)
- [x] inntekt-skatt (Dec 2024)
- [x] statistikk (Dec 2024)

### Phase 3: Agreements (Target: Q3 2025)
- [x] trygdeavtaler (Dec 2024)
- [x] eos-forordning (Dec 2024)

### Phase 4: Technical (As needed)
- [x] kafka (Dec 2024)
- [ ] testing
- [ ] security
- [ ] flyway-migration
- [ ] frontend-api

## Notes

### Skill Structure Standard

We have a skill-creator skill with guidelines for creating new skills. Use this

### Creation Guidelines

When creating a new skill:
1. Start with thorough codebase exploration
2. Identify key services, entities, and integrations
3. Document common debugging scenarios
4. Include SQL queries for investigation
5. Cross-reference with related skills (e.g., saksflyt, kodeverk)

### Confluence Resources

Use `mcp__melosys-confluence__search_melosys-confluence` to gather domain knowledge:
- Funksjons-fagområder documentation
- Business rules and regulations
- Process flows and decision trees
