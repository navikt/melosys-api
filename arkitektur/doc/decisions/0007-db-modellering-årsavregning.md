# Modellering av årsavregninger i databasen

Date: 2024-04-29

## Status

Under arbeid

## Context

Teamet trenger å kunne gjøre årsavregning av trygdeavgift i saker der NAV fakturerer trygdeavgift.
I denne forbindelsen må vi representere og lagre en årsavregning i databasen.

En årsavregning består av året og to beløp: totalbeløpet for det som har vært fakturert tidligere og totalbeløpet som faktisk gjelder for året.

Modellen må støtte tre brukstilfeller hvor årsavregningen beregnes:
- det finnes et tidligere trygeavgiftsgrunnlag med tidligere fakturering i Melosys
- det finnes et tidligere trygeavgiftsgrunnlag uten tidligere fakturering i Melosys
- det finnes ingen trygeavgiftsgrunnlag (både uten og med tidligere fakturering), fordi det har vært behandlet utenfor Melosys (Avgiftsystemet)

### Vurdering: kopiering av det tidligere trygeavgiftsgrunnlaget
Det har vært en diskusjon om å kopiere det tidligere trygdeavgiftsgrunnlaget i forbindelse med årsavregning, kontra å peke på en tidligere behandling.

Det er mulig å ikke kopiere data fordi en årsavregning utføres kun på bakgrunn av uforanderlige data fra et vedtak i en tidligere
behandling.
Det tidligere trygdeavgiftsgrunnlaget kan gjelde for flere år, mens den delen av grunnlaget som er relevant for en årsavregning kun er innenfor et år.

#### Å kopiere den relevante delen av grunnlaget
Fordeler:
- Forenkler visning og rapportering: tidligere grunnlag utledes kun en gang
- Kan gi bedre ytelse hvis utledning blir krevendre, noe vi ikke forventer i dag
- Gjør grunnlaget for årsavregningen direkte lesbar og eksplisitt i databasen

Ulemper:
- Dupliserer som faktisk data som finnes i en tidligere behandling
- Modellen blir litt mer kompleks
- Krever mer lagringsplass

#### Å peke på en tidligere behandling
Fordeler:
- Krever mindre lagringsplass, ingen duplisering
- Dette gir en enklere datamodell.
- Vi trenger uansett å peke på den opprinnelige kilden for årsavregning,

Ulemper:
- Vi må finne frem grunnlaget for årsavregningen hver gang ifm. visning.
- Kilden, behandlingen, må ikke være redigerbar. Kan vi garantere det?
- Hvis måten man finner frem på et tidligere trygdeavgiftsgrunnlag skulle endre seg på et tidspunkt (og ikke gjelder tilbake i tid)
så må man beholde flere versjoner av logikken for å finne frem til grunnlaget i koden. Vi tror ikke dette kommer til å skje.


Vi vurderer at å peke på en tidligere behandling har mindre ulemper.

### Håndtering av årsavregninger fra saker utenfor Melosys

Når det ikke finnes et tidligere trygeavgiftsgrunnlag i Melosys må sakbehandlere kunne oppgi hva som ble fakturert som et totalbeløp.
Man må også kunne oppgi dekning for medlemskapsperioden.

Vi har tenkt å lage egne behandlinger for å importere behandlinger fra Avgiftsystemet. En slik behandling ville inneholde medlemskapsperiode eller
lovvalgsperioden fra Avgiftsystemet og forenkle litt databasestrukturen for årsavregning siden alle årsavregninger ville alltid peke på en tidligere
behandlingsresultat i Melosys.

Ulempen med å lage egne behandlinger vurderes imidlertid å være for stor. Teknisk må ta hensyn til en ny behandlingstype, visning av slike behandlinger og disse
behandlingene må ignoreres for saksbehandlingsstatistikk. Funksjonelt er det også uvant å lage en behandling som resulterer i perioder uten vedtak fra Melosys.

## Decision

Tabellen for en årsavregning inneholder år, tidligere fakturert beløp, nytt total beløp for året og beløpet som skal faktureres eller krediteres.
Tabellen refererer til en tidligere behandlingsresultat når det finnes et tidligere trygeavgiftsgrunnlag.

Når man avregner på basert en tidligere behandling i Melosys er det kun årsavregning med nye trygdeavgiftsperioder man oppretter ifm. årsavregning, med tilhørende nye inntektsperioder og skatteforhold.

Når en årsavregning utføres på bakgrunn av en behandling i Avgiftssystemet lager vi en ny medlemskapsperiode eller lovvalgsperiode som svarer til vedtaket i avgiftssystemet, knyttet til årsavregningen.
Dermed får vi noen rader i tabellene for medlemskapsperioder/lovvalgsperioder som ikke svarer til vedtak og dermed ikke trenger innvilgelsesresultat eller medlPeriodeID.
Dette må avklares ytterligere.

Se gjerne https://confluence.adeo.no/display/TEESSI/Fysiske+database+modellen
