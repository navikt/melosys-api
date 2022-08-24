# 4. Oracle db

Date: 2017-04-19

## Status

Accepted

## Context

NAV har satset på Oracle databaser som er per idag det eneste alternativet.

## Decision

Vi skal bruke Oracle db.

## Consequences

Denne avgjørelsen vil låse oss for lang tid fremover. Oracle bruker noen spesifikke typer og all bruk utover standard sql,
type PL/SQL osv vil gjøre det vanskeligere å endre db på et senere tidspunkt.
