# 2. Bruke Kotlin istedenfor Java

Date: 2022-03-31

## Status

Akseptert

## Context

Kotlin er et mye brukt programmeringsspråk hos NAV som kan leve sammen med Java. Kotlin er et mer moderne programmeringsspråk enn Java og innholder en del
funksjoner som kan gjøre hverdagen [lettere](https://www.imaginarycloud.com/blog/kotlin-vs-java) for utviklere. Det kompilerer til
bytecode, kjører på JVM og er kompatibelt med bibliotek/rammeverk som er skrevet i Java. Vi har allerede skrevet nye moduler (Trygdeavtale)
i Kotlin og det er et generelt ønske fra utviklere om å gå i denne retningen.

Det vil ikke bli satt av tid til å konvertere hele kodebasen til Kotlin eller egne dedikerte oppgaver for å konvertere
modul for modul. En evt endring av dette kaliberet vil måtte skje organisk gjennom funksjonelle oppgaver hvor berørte klasser
konverteres løpende. Vi har allerede gjort en vellykket PoC for integrasjonslaget, samt brukt Kotlin i integrasjonstestene våre.

Det er forståelse i team ledelsen for at vi trenger tekniske oppgraderinger av kodebasen vår for å unngå for mye teknisk gjeld. Men vi må sørge for at
dette gjøres samtidig som vi leverer de funksjonelle historiene som er forventet av oss.

## Decision

Vi kommer til å løpende konvertere Java kode til Kotlin i forbindelse med andre oppgaver som rører denne koden.

Vi kommer til å skrive nye klasser/moduler i Kotlin.

## Consequences

Ved hjelp av Kotlin kan vi få mindre syntaktisk sukker, noe som vil gi mer lesbar kode og mindre feil. Det er mange utviklere som ønsker å
bruke lombok, men dette er omstridt i NAV. Med Kotlin vil ikke dette lenger være et poeng.

Det vil alltid være fare for at konvertert kode ikke er helt lik eksisterende. Dette vil vi mitigere ved å skrive tester før vi konverterer
kode. I tillegg har vi fått implementert integrasjonstester som vil redusere risikoen for feil.

En annen konsekvens vil være at utviklere må lære seg Kotlin. Vi har allerede behov for kompetanse på dette ifm andre moduler.
