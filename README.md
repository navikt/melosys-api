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

### Lokal utvikling mot q1 cluster
Man må ha melosys-web kjørende lokalt først<br>
Bruk profil `local-q1`<br>
Men trenger å sette følgende env variabler
* AZURE_APP_CLIENT_ID
* AZURE_APP_CLIENT_SECRET
* melosysDB.password
* systemuser.password

Man henter ut disse verdiene fra melosys-api-q1 poden på dev-fss ved å kjøre:<br>
`env | grep AZURE_APP_CLIENT_ID` og `env | grep AZURE_APP_CLIENT_SECRET`<br>
`melosysDB.password` og `systemuser.password` finner man i [vault](https://vault.adeo.no/ui/vault/secrets/kv%2Fpreprod%2Ffss/show/melosys-q1/teammelosys)

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
