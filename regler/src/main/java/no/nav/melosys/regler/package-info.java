/**
 * Litt om teknisk design av regelmodulen.
 * 
 * Det er i implementasnjonen lagt stor vekt på to kvaliteter: Forvaltbarhet av forretningsregler og lesbarhet av forretningsregler (også av "blårussen"). Det 
 * betyr at det er akseptert innførsel av kompleksitet i form av reflection og annen magi i kode som ikke er kandidat for hyppige endringer for å oppnå de 
 * ønskede kvalitetene.
 * 
 * Forretningsregler i Melosys skal implementeres med disse komponentene:
 * 
 * Regeltjeneste:
 * En regeltjeneste er en tjeneste som kalles for å anvende et sett med forretningsregler (eksempel: no.nav.melosys.regler.old.service.lovvalg.LovvalgTjenesteImpl).
 * En regeltjeneste har én kontekst (som reglene kjører på på) og én regelflyt (som organiserer regelkjøring)
 * 
 * Kontekst:
 * Konteksten inneholder tilstanden for regelkjøringen, dvsa. input, output og mellomregninger. Dette muliggjør implementasjon av forretningsregler som static 
 * metoder uten parametre. Konteksten er bundet til tråden som kjører regelflyten.
 * 
 * Regelflyt:
 * Regelflyten inneholder regelpakker i en definert rekkefølge. Inntil vi trenger noe mer, er det kun støtte for veldig enkle regelflyt, der alle regelpakkene
 * alltid kjøres i definert rekkefølge (mao. ingen støtte for løkker og komplekse grafer).
 * 
 * Regelpakke:
 * En regelpakke inneholder en eller flere regler. Reglene må annoteres med @Regel, og da kjøres de automatisk når pakken kjøres. Reglene kan skrives som 
 * vanlig java-kode, men de skal primært skrives verbalisert og deklarativt.  
 * 
 * Eksempler på deklarasjoner:
 * hvis(søknaden()).mangler().så(leggTilMelding(VALIDERINGSFEIL, "Forespørselen mangler søknad"))
 * 
 * Logging:
 * Verbaliseringen inkluderer også magi for logging. Det betyr at vi i de fleste tilfeller får logging gratis ved å bruke de verbaliserte metodene. Loggingen 
 * inneholder funksjonalitet for å logge med riktig regelnavn (hentes fra stack trace).
 */
package no.nav.melosys.regler;

/*
 * FIXME (innspill fra francois)
 * no.nav.melosys.regler -> no.nav.melosys.lovvalg
 * no.nav.melosys.regler.api -> no.nav.melosys.lovvalg.api
 *
 * Og så blir regler en pakke under lovvalg eller kanskje en egen pakke.
 */
