# 3. Norsk som hovedspråk

Date: 2017-04-19

## Status

Accepted

## Context

All kode i Melosys-api er skrevet på norsk. Dette innebærer at med unntak av ting som Java syntaks og metode prefiks som må være på engelsk, vil
alt annet være på norsk. Dette er en avgjørelse som ble tatt flere år tilbake hvor de fleste utviklere mente at siden domenespråk må være på norsk,
så må resten av koden være på norsk også, for å unngå forvirringen av flere språk.

## Decision

Vi skal skrive all kode på norsk så langt det er mulig.

## Consequences

Denne beslutningen var viktig siden lesbarhet av hele kodebasen og skriving av kode er påvirket.
Mange ord og uttrykk i programmering er etablert på engelsk og det føles unaturlig å lage norske oversettelser.
Eksempler på dette er @RestControllere som postfikses med "Tjeneste", bruk av "er" istendefor "is" for boolske verdier, osv.

Omfattende bruk av norsk krever også tilgang på norskspråklige utviklere. Dette er ikke et problem for øyeblikket, men man kan se
for seg at dette vil bli en utfordring senere. Innenfor programmering er engelsk "lingua franca" og det er uklart hvilken fordel det
skal være å bruke norsk.

Beslutningen er per i dag omstridt i teamet og vil bli tatt opp til ny vurdering, men er foreløpig gjeldende.
