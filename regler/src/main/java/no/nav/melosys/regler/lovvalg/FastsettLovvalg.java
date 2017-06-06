package no.nav.melosys.regler.lovvalg;

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

    
}
