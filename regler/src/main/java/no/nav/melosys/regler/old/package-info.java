
/**
 * Rotpakke til regelmodulen til Melosys
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
