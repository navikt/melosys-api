# Melosys-api (Medlemskap- og Lovvalgssystem)

Melosys er et saksbehandlingssystem for avdelingen Medlemskap og avgift i NAV som behandler søknader om medlemskap
 i folketrygden, samt registrerer unntak for utenlandske statsborgere som jobber i Norge.

Melosys-api er backenden for selve saksbehandlingsløsningen for prosjektet og inneholder det meste av logikk tilknyttet
 saksbehandlingsløsningen.

## Lokal utvikling
Melosys-api kan kjøres opp som en ren Spring-applikasjon<br>
Swagger kan også nås på `localhost:8080/swagger-ui/`

### Mot lokalt docker-compose
Sørg for å kjøre opp  [melosys-docker-compose](https://github.com/navikt/melosys-docker-compose). Alle avhengigheter av applikasjonen spinnes da opp.
Som database, kafka, oauth-server samt eksterne integrasjoner. Trenger også [naisdevice](https://doc.nais.io/device/install/index.html)
for å koble til enkelte eksterne tjenester.<br>
Bruk profil `local-mock`

### Lokal utvikling mot dev-cluster
Man må ha melosys-web kjørende lokalt først, og være innlogget med `nais login`.<br>
Bruk profil `local-q1` eller `local-q2` avhengig av hvilket miljø du kjører mot.

Ved oppstart med profil `local-q1`/`local-q2` henter applikasjonen selv
`AZURE_APP_CLIENT_ID`, `AZURE_APP_CLIENT_SECRET`, `MELOSYSDB_PASSWORD` og
`SRV_USERNAME`/`SRV_PASSWORD` fra nais via `scripts/get-azure-secrets.sh`. Du
trenger derfor ingen hemmeligheter i env-filer — det holder å være innlogget med
`nais login`. Variabler som allerede finnes i miljøet blir ikke overstyrt.

Databaseadresse og -brukernavn er hardkodet per profil. `MELOSYSDB_URL` på nais
peker på en intern adresse som ikke er tilgjengelig fra egen maskin, og
brukernavnet er ikke hemmelig og hører derfor hjemme i konfigurasjonen.

Scriptet kan også kjøres manuelt:

```bash
# Én variabel -> verdien printes rått
scripts/get-azure-secrets.sh AZURE_APP_CLIENT_SECRET

# Flere variabler -> NAVN=verdi-linjer
scripts/get-azure-secrets.sh AZURE_APP_CLIENT_ID MELOSYSDB_PASSWORD

# Rett inn i eget shell, uten å lagre til disk
eval "$(scripts/get-azure-secrets.sh --export AZURE_APP_CLIENT_ID MELOSYSDB_PASSWORD)"
```

Vet du ikke hva variabelen heter, list opp alle tilgjengelige nøkler
(henter ingen verdier, og gir derfor ingen audit-oppføring på secrets):

```bash
scripts/get-azure-secrets.sh --list
```

Se `scripts/get-azure-secrets.sh --help` for flagg som `--app`, `--environment`,
`--team` og `--debug`. `--debug` skriver kun navn og nøkler til stderr, aldri verdier.

> Unngå å skrive hemmelighetene til fil. Bruker du likevel `> .env.local`, ligger
> de ukryptert på disk (filen er dekket av `*.local` i `.gitignore`, så den
> committes ikke, men slett den når du er ferdig).

### Lokale endringer i `melosys-skjema-api-types`

`melosys-skjema-api-types` blir til vanlig kun publisert ved push til `main` i [melosys-skjema-api](https://github.com/navikt/melosys-skjema-api). For å teste type-endringer mot melosys-api uten å merge til main først:

```bash
make local-skjema-types          # bygg + oppdater pom.xml
make local-skjema-types-no-pom   # bygg uten å endre pom.xml
```

Targeten kaller `scripts/build-local-skjema-types.sh`, som delegerer selve byggingen til `../melosys-skjema-api/scripts/publish-types-local.sh`. Den lokale versjonen får suffiks `-LOCAL` (og `.dirty` hvis working tree har ucommittede endringer), publiseres til `~/.m2/repository`, og pom.xml peker på den.

Forutsetninger: `melosys-skjema-api` må være klonet som søsterkatalog (`../melosys-skjema-api`) eller pekt på via `SKJEMA_API_DIR=...`. Husk å revertere pom.xml-endringen før commit.

## Feature Toggles

Melosys-api bruker [Unleash](https://www.getunleash.io/) for feature toggles. I lokal utvikling er systemet konfigurert
med `DefaultEnabledUnleash` som gjør ukjente/ukonfigurerte toggles **enabled** som standard. Dette gjør det enkelt å
utvikle nye funksjoner uten å måtte konfigurere hver toggle manuelt i Unleash.

For fullstendig dokumentasjon om feature toggles, inkludert oppsett av Unleash lokalt, løsningsvalg og best practices,
se [FEATURE_TOGGLES.md](config/src/main/kotlin/no/nav/melosys/featuretoggle/FEATURE_TOGGLES.md)

## Arkitektur

Melosys-api har en lagdelt arkitektur og bruker primært spring-boot som rammeverk:

- **app**: Kjører opp spring-applikasjonen, setter miljøvariabler og inneholder flyway-migreringer.
- **config**: Felles konfigurasjon for applikasjonen
- **domain**: Inneholder domeneobjekter, for det meste POJOs
- **feil**: Inneholder interne exception-klasser
- **frontend-api**: Rest-endepunkter brukt av [melosys-web](https://github.com/navikt/melosys-web)
- **integrasjon**: SOAP, REST og GraphQL-integrasjon mot andre NAV-interne tjenester
- **repository**: Database-lag
- **saksflyt**: Komponent som følger [saga-pattern](https://microservices.io/patterns/data/saga.html) for å orkestere
 prosesser som utfører flere transaksjoner.
- **service**: Service-lag
- **sikkerhet**: Felles logikk knyttet til sikkerhet. Eks OIDC, STS, etc.
- **soknad-altinn**: maven-modul som genererer POJO's fra XSD som representerer en søknad fra Altinn
- **statistikk**: Produserer statistikk om utstedte A1 (attester om medlemskap etter EU/EØS-forordning) til dvh (datavarehus).

## Versjonering for databasemigreringer

I db/migration/melosysDB har vi migreringer for databasen til melosys-api.
I db/migration/melosysDB/di_dvh har vi migreringer for Datavarehus, som er ansvarlig for saksbehandlingstatistikk i Melosys.

Vi har besluttet at versjon for en ny migrering i melosysDB skal være siste versjon + 1.
Ny migrering for Datavarehus skal være siste versjon + desimal, slik at man slippe å titte i mappen di_dvh når man oppretter
ny migrering i melosysDB.

## Testing og Coverage

### Kjøre tester
```bash
make test              # Kjør alle tester
make test-integration  # Kjør integrasjonstester
make coverage          # Kjør tester med coverage og vis oppsummering
```

### Coverage-rapporter
Prosjektet bruker JaCoCo for kodedekning. Hver modul genererer sin egen rapport:
```bash
make coverage-summary  # Vis én-linje-per-modul oppsummering
```

Detaljerte HTML-rapporter finnes i `<modul>/target/site/jacoco/index.html` etter kjøring av tester.
Se [docs/COVERAGE.md](docs/COVERAGE.md) for fullstendig dokumentasjon.

### Komponent tester

Noen komponenttester er avhengig av oracle databasen. Den kjøres opp automatisk med testcontainer. Men siden det ikke finnes et oracle image som støtter arm arkitektur må de som bruker m1 mac sette en enviroment variabel: `USE-LOCAL-DB=true`. Da kobler testene seg til en kjørende database på maskinen. Se dokumentajon [her](https://github.com/navikt/melosys-docker-compose) for mer info

### Mock-container for integrasjonstester

Integrasjonstestene bruker en Docker-basert melosys-mock container som kjøres via Testcontainers. Dette gir:
- **Konsistens**: Samme mock brukes i tester som i lokal utvikling
- **Isolasjon**: Hver testkjøring starter med ren tilstand
- **Raskere tester**: Containeren deles mellom alle tester i samme kjøring

#### Miljøvariabler

| Variabel | Standardverdi | Beskrivelse |
|----------|---------------|-------------|
| `USE_TEST_CONTAINER` | `true` | Sett til `false` for å kjøre mot lokal docker-compose i stedet for testcontainer |
| `MOCK_VERIFICATION_STRICT_MODE` | `true` | Sett til `false` for å returnere tomme resultater i stedet for å kaste unntak ved kommunikasjonsfeil |

#### Kjøre mot lokal docker-compose

For raskere iterasjon under utvikling kan du kjøre testene mot en allerede kjørende mock i docker-compose:

#### Verifisering av mock-tilstand

Testene bruker `MockVerificationClient` for å verifisere hva som ble sendt til mocken:

```kotlin
// Verifiser MEDL-perioder
mockVerificationClient.medl().shouldHaveSize(1)

// Verifiser SED-er sendt til RINA
mockVerificationClient.sedForRinaSak("123456").shouldContain("A009")

// Tøm mock-data (gjøres automatisk i @BeforeEach)
mockVerificationClient.clear()
```
