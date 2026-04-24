---
name: commit-melding
description: |
  Skriv commit-meldinger på norsk.
  Use when: creating git commits, writing commit messages, staging and committing changes.
  Triggers: "commit", "committe", "lag commit", "commit message", "commit melding", "commit-melding", "skriv commit", "git commit".
---

# Commit-melding

## Git-regler for agenter

- **Aldri amend** en commit uten å spørre brukeren først.
- **Aldri force-push** uten å spørre brukeren først.
- **Aldri push** uten å spørre brukeren først.
- Foretrekk **nye commits** fremfor å amende eksisterende.
- **Stage kun relevante filer** — aldri `git add -A`.

## Språk og stil

- **Norsk.** Både tittel og brødtekst.
- **Imperativ form** i tittelen: «Innfør», «Fjern», «Fiks», «Legg til», «Flytt», «Endre», «Bump».
- **Maks 72 tegn** i tittelen.
- Unngå å beskrive det som er opplagt fra diffen. Brødteksten skal forklare **hvorfor**, ikke gjenta **hva** som ble endret.
- Vær kortfattet. Dropp fyllord og unødvendige detaljer.

## Format

```
<tittel>

<brødtekst — valgfri, kun når endringen trenger kontekst>
```

### Tittel

- Beskriver intensjonen — **hvorfor**, ikke implementasjonsdetaljer.
- Spesielt viktig når brødteksten mangler: tittelen er ofte det eneste man leser (f.eks. i `git log --oneline`).
- Bare imperativ beskrivelse — ingen saksnummer eller prefiks.

### Brødtekst

- Forklar **hvorfor** endringen er gjort, ikke **hva** som ble endret (det viser diffen).
- Relevant kontekst: hva som feilet, hvilken kontrakt/konvensjon som følges, hvorfor en tilnærming ble valgt.
- Unngå å liste opp filer eller moduler som er endret — det ser man i diffen.
- Blank linje mellom tittel og brødtekst.
- Linjelengde maks ~72 tegn i brødteksten.

## Eksempler

### Enkel endring — tittelen forklarer hvorfor

```
Fjern ubrukt import for å unngå falsk avhengighet til saksflyt
```

### Endring som trenger kontekst i brødtekst

```
Innfør Avgiftsdel-enum

Streng-verdier gir ingen kompileringsfeil ved skrivefeil og
ingen oversikt over gyldige verdier. Enum gir typesikkerhet
og dokumenterer de lovlige verdiene.

Følger samme mønster som Avgiftsberegningsregel: enum i
domain, parsing fra ekstern streng i service-laget.
```

### Refaktorering

```
Trekk ut duplisert faktura-mapping til FakturabeskrivelseMapper
```

### Bump-endring

```
Bump spring-boot fra 3.3.0 til 3.3.1
```

## Anti-mønster

- ❌ «Erstatter String-feltet avgiftsdel med typesikker enum på tvers av modulene domain, service og frontend-api» — lister opp hva og hvor, begge deler er opplagt fra diffen.
- ❌ «Oppdaterer X, Y og Z» — gjentar diffen.
- ❌ Engelske commit-meldinger.
- ❌ Emojis.
- ❌ Saksnummer eller Jira-nøkler i tittelen (f.eks. `7588 ...` eller `MELOSYS-7588 ...`) — saksnummer hører til brancher, ikke commits.
