# Melosys-api (Medlemskap- og Lovvalgssystem)

Melosys er et saksbehandlingssystem for avdelingen Medlemskap og avgift i NAV som behandler søknader om medlemskap
 i folketrygden, samt registrerer unntak for utenlandske statsborgere som jobber i Norge.

Melosys-api er backenden for selve saksbehandlingsløsningen for prosjektet og inneholder det meste av logikk tilknyttet
 saksbehandlingsløsningen.

## Lokal utvikling

Melosys-api kan kjøres opp som en ren Spring-applikasjon med profil `local-mock` ved hjelp av 
 [melosys-docker-compose](https://github.com/navikt/melosys-docker-compose), som spinner opp alle avhenigheter applikajsonen
 har, som database, kafka, ldap, oauth-server samt eksterne integrasjoner. Trenger også naisdevice for å koble til enkelte
 eksterne tjenester.

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
- **sikkerhet**: Felles logikk knyttet til sikkerhet. Eks ABAC, OIDC, STS, etc.
- **soknad-altinn**: maven-modul som genererer POJO's fra XSD som representerer en søknad fra Altinn
- **statistikk**: Produserer statistikk om utstedte A1 (attester om medlemskap etter EU/EØS-forordning) til dvh (datavarehus).
