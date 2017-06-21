package no.nav.melosys.regler.lovvalg;

import static no.nav.melosys.regler.lovvalg.LovvalgKontekst.detErMeldtFeil;

import no.nav.melosys.regler.lovvalg.utsendt_arbeidstaker.Art_12_1_UtsendtArbeidstaker;
import no.nav.melosys.regler.lovvalg.verifiser_inndata.VerifiserInndata;

/**
 * Inngansport til å kjøre regler for å fastsette lovvalg
 */
public final class FastsettLovvalg {
    
    // Regelsett som kjøres:
    private Regelsett[] regelsett = new Regelsett[] {
        new Art_12_1_UtsendtArbeidstaker()
    };
    
    /**
     * Fastsetter lovvalg
     */
    public void kjørRegler() {

        // Verifiser input og returner hvis feil
        VerifiserInndata.kjørRegler();
        if (detErMeldtFeil()) {
            return;
        }
        
        // Kjør regelsettene
        for (Regelsett rsett : regelsett) {
            rsett.kjør();
        }
    }
