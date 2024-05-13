# 3. Norsk som hovedspråk

Date: 2017-04-19

## Status

Accepted

## Context

Siden domenespråk må være på norsk, så mener vi at all kode også må være på norsk for å unngå språklig forvirring.

## Decision

Vi skal skrive all kode i Melosys-api på norsk, med unntak av ting som Java syntaks og metode prefiks som må være på engelsk.

## Consequences

Denne beslutningen er viktig siden lesbarhet av hele kodebasen og skriving av kode er påvirket.
Mange ord og uttrykk i programmering er etablert på engelsk og det føles unaturlig å lage norske oversettelser.
Eksempler på dette er @RestControllere som postfikses med "Tjeneste", bruk av "er" istedenfor "is" for boolske verdier, osv.

Omfattende bruk av norsk krever også tilgang på norskspråklige utviklere. Dette er ikke et problem for øyeblikket, men man kan se
for seg at dette vil bli en utfordring senere. Innenfor programmering er engelsk "lingua franca" og det er uklart hvilken fordel det
skal være å bruke norsk.

Beslutningen er per 08/2022 omstridt i teamet og vil bli tatt opp til ny vurdering, men er foreløpig gjeldende.
