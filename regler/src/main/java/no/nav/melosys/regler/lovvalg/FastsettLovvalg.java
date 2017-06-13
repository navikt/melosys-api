package no.nav.melosys.regler.lovvalg;

<<<<<<< HEAD
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

=======
import java.util.Collections;

import org.slf4j.Logger;

import no.nav.melosys.regler.api.lovvalg.FastsettLovvalgRequest;
import no.nav.melosys.regler.api.lovvalg.FastsettLovvalgRespons;
import no.nav.melosys.regler.api.lovvalg.Feilmelding;
import no.nav.melosys.regler.api.lovvalg.Kategori;
import no.nav.melosys.regler.util.RegelLoggerFactory;

/**
 * Inngansport til å regler for å fastsette lovvalg
 */
public final class FastsettLovvalg {
    
    private static Logger log = RegelLoggerFactory.getRegelLogger();
    
    private FastsettLovvalg() {}

    /**
     * Fastsetter lovvalg
     */
    public static FastsettLovvalgRespons fastsettLovvalg(FastsettLovvalgRequest req) {
        try {
            log.info("Setter feil IKKE_STOETTET");
            FastsettLovvalgRespons res = new FastsettLovvalgRespons();
            Feilmelding feil = new Feilmelding();
            feil.kategori = Kategori.IKKE_STOETTET;
            feil.feilmelding = "Tjenesten er ikke implementert enda";
            res.feilmeldinger = Collections.singletonList(feil);
            return res;
        } catch(Throwable e) {
            // Forsok å logge feilen...
            try {
                log.error("Uventet Exception", e);
            } catch (Throwable ignored) {
            }
            // Returner teknisk feil...
            FastsettLovvalgRespons res = new FastsettLovvalgRespons();
            Feilmelding feil = new Feilmelding();
            feil.kategori = Kategori.TEKNISK_FEIL;
            feil.feilmelding = "Uventet Exception";
            res.feilmeldinger = Collections.singletonList(feil);
            return res;
        }
    }

    
>>>>>>> cf2df1bd3807d09d0dbf434f50c32f1e049fa5fb
}
