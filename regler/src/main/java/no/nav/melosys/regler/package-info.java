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
 * Konteksten inneholder tilstanden for regelkjøringen, input, output og mellomregninger. Dette muliggjør implementasjon av forretningsregler som static 
 * metoder uten parametre. Konteksten er bundet til tråden som kjører regelflyten.
 * 
 * Regelflyt:
 * Regelflyten inneholder regelpakker i en definert rekkefølge. Inntil vi trenger noe mer, er det kun støtte for veldig enkle regelflyt, der alle regelpakkene
 * alltid kjøres i definert rekkefølge (mao. ingen støtte for løkker og komplekse grafer).
 * 
 * Regelpakke:
 * En regelpakke inneholder en eller flere regler. Reglene må annoteres med @Regel, og da kjøres de automatisk når pakken kjøres. Reglene kan skrives som 
 * vanlig java-kode, men de bør primært skrives deklarativt. Dette gjøres ved å lage en liste med deklarasjoner som man utfører (ved å kalle 
 * Regelpakke.utfør()).
 * 
 * Eksempler på deklarasjoner:
 * hvis(søknaden()).mangler().så(leggTilMelding(VALIDERINGSFEIL, "Forespørselen mangler søknad"))
 * hvis(variabelen(BRUKER_ARBEIDER_PÅ_SKIP)).erSann.så(settVariabel
 * 
 * 
 * 
 * 
 * TODO (farjam 2017-06-12): Hva som må gjøres med modulen før flere regelpakker legges til:
 * 
 * 1) Lag superklasser for respons i api-modulen. Denne skal ha støtte for å legge på feilmeldinger og lovvalgsbestemmelser.
 * 2) Lag superlklasse for request i api-modulen.
 * 3) Lag en typet og instansierbar superklasse for Kontekst. Flytt gjenbrukbar kode fra LovvalgKontekst til superklassen.
 * 4) Tilby en enkel måte å få riktig kontekst.
 * 5) Flytt Regelsett til no.nav..nare og fjern avhengigheter til LovvalgKontekst
 */
package no.nav.melosys.regler;

