# Forenkling av tabeller for trygdeavgift

Date: 2024-04-26

## Status

Under arbeid

## Context

Nåværende datastruktur for trygdeavgift oppleves som mer komplisert enn nødvendig av flere utviklere, med tabeller som inneholder lite data
og avhengigheter som skaper nesten en syklus. Det ble også oppdaget feil i JPA-mappingen i forbindelse med oppgradering av Spring Boot.

Fastsatt_trygdeavgift kan betraktes som både starten og slutten av syklusen.
Trygdeavgiftsperioder er avhengige av inntektsperioder og skatteforhold, som igjen er avhengige av et trygdeavgiftsgrunnlag. Men
Samtidig er tabellen Trygdeavgiftsgrunnlag avhengig av Fastsatt_trygdeavgift og trygdeavgiftsperioder er avhengige av fastsatt_trygdeavgift.

Avhengighetssykluser mellom tabeller i en database kan føre til uønskede konsekvenser som f.eks. økt kompleksitet, utfordringer med å opprettholde dataintegritet
og dårligere ytelser. Unødvendige avhengighetssykluser bør derfor unngås.

Siden årsavregninger skal gjenbruke en del av datastrukturen for trygdeavgift mener vi at nå er et godt tidspunkt for å endre den.


## Decision

- Vi bestemmer oss for å forenkle datastrukturen for trygdeavgift uten å miste funksjonalitet.
Endringen forventes å føre til en mer oversiktlig og vedlikeholdbar datastruktur.
Se ev. https://confluence.adeo.no/display/TEESSI/Fysiske+database+modellen
