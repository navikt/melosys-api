# 1. Hendelser i Melosys

Date: 2024-05-07

## Status

Accepted

## Context

Melosys API har behov for å kunne sende hendelser til andre systemer. I først om gang er dette vedtak til skatt-hendelser.
Vi ser for oss at dette på sikt kan brukes til å sende statistikk til team som er interessert i dette istendefor at det bruke triggere i databasen.
Hendelsene skal være utvidbare slik at andre systemer kan abonnere på det de er interessert i. Lignende funksjonalitet finnes i andre systemer i NAV f.eks implementert gjennom Rapids and Rivers
## Decision

Vi bruker Kafka kø for å sende hendelser til andre systemer.
Siden vi allerede bruker kafka kø i melosys er dette relativt enkelt å sette opp.

## Consequences
Dette vil før til et mer decouplet system og enklere kommunikasjon med eksterne team.
