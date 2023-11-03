# 5. Modulær monolit

Date: 2023-11-02

## Status

Accepted

## Context

Melosys har siden det ble startet i 2018 ikke hatt en klar retning for hvordan arkitekturen i melosys-api skal se ut.
På grunn av dette er dagens arkitektur utviklet seg til package-by-layer spagetti, med innslag av noen funksjonelle moduler.
Særlig service laget er berørt, med opptil 11 lag med kall nedover i service stacken. Det har tidligere vært lite fokus på komponenttester
og refaktorering - noe vi nå betaler prisen for. Vi kommer ikke til å lage mikrotjenester da vi ikke har behov for skalerbarhet eller skille ut
funksjonalitet til andre team.

## Decision

Det bestemmes at koden skal refaktoreres med fokus på å modularisere monoliten. Det vil si at etter prinsippet om
"high cohesion - low coupling" så skal funksjonalitet som hører sammen ligge i samme modul/pakkestruktur og
det skal defineres tydelige interface som koden skal kalles fra. Vi skal håndheve arkitektur gjennom ArchUnit tester
og det skal opprettes komponenttester ifm refaktoreringen.

## Consequences

Dette vil kreve omfattende refaktorering som vil ta tid. Nyutvikling skal fortsette og dette vil kunne føre til merge konflikter hvis refaktoreringen
treffer bredt og branching lever lenge. Vi må derfor gjøre mindre og målrettede endringer for å unngå dette.

Ved å gjøre disse endringene vil vi sørge for at Melosys også i fremtiden vil være endringsdyktig og utvikling kan fortsette med samme hastighet.
