package no.nav.melosys.regler.lovvalg;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.melosys.regler.api.lovvalg.Alvorlighetsgrad;
import no.nav.melosys.regler.api.lovvalg.FastsettLovvalgRequest;
import no.nav.melosys.regler.api.lovvalg.FastsettLovvalgRespons;
import no.nav.melosys.regler.api.lovvalg.Feilmelding;
import no.nav.melosys.regler.api.lovvalg.Kategori;
import no.nav.melosys.regler.api.lovvalg.Lovvalgsbestemmelse;

/**
 * Kontekst for en regelkjøring. 
 * 
 * Tilbyr forskjellige metoder, bl.a. tilgang til søknaden og responsen.
 * 
 * Konteksten er bundet til tråden den kjører på, slik at regelsett kan kalles i parallell.
 * 
 */
public class LovvalgKontekst {
    
    private static Logger log = LoggerFactory.getLogger(FastsettLovvalg.class); // Logger til FastsettLovvalg sin kanal

    private static ThreadLocal<FastsettLovvalgRequest> lokalFastsettLovvalgRequest = new ThreadLocal<>();
    private static ThreadLocal<FastsettLovvalgRespons> lokalFastsettLovvalgRespons = new ThreadLocal<>();

    /** Initialiserer regelkjøringens kontekst. Må gjøres før man oppretter eller kjører regler. */
    public static void initialiserLokalKontekst(FastsettLovvalgRequest req) {
        if (lokalFastsettLovvalgRequest.get() != null) {
            log.error("Forsøk på å sette kontekst før eksisterende kontekst er slettet.");
            throw new RuntimeException("Forsøk på å sette kontekst før eksisterende kontekst er slettet.");
        }
        lokalFastsettLovvalgRequest.set(req);
        FastsettLovvalgRespons res = new FastsettLovvalgRespons();
        res.feilmeldinger = new ArrayList<>();
        res.lovvalgsbestemmelser = new ArrayList<>();
        lokalFastsettLovvalgRespons.set(res);
    }
    
    /** Sletter regelkjøringens kontekst. Må gjøres etter at alle regelsettene er kjørt. */
    public static void slettLokalKontekst() {
        lokalFastsettLovvalgRequest.set(null);
        lokalFastsettLovvalgRespons.set(null);
    }

    /** Returnerer FastsettLovvalgRequest som regelsettet jobber med. */
    public static FastsettLovvalgRequest søknad() {
        return lokalFastsettLovvalgRequest.get();
    }

    /** Returnerer FastsettLovvalgRespons som regelsettet jobber med. */
    public static FastsettLovvalgRespons respons() {
        return lokalFastsettLovvalgRespons.get();
    }

    public static void logg(String format, Object... arguments) {
        String regelnavn = "Ukjent regel";
        for (StackTraceElement se : Thread.currentThread().getStackTrace()) {
            if (se.getClassName().startsWith("no.nav.melosys.regler.lovvalg.") && !se.getClassName().equals(LovvalgKontekst.class.getName())) {
                String regelKlasse = se.getClassName();
                String regelMetode = se.getMethodName();
                regelnavn = regelKlasse.replaceAll("no.nav.melosys.regler.lovvalg", "") + regelMetode;
                break;
            }
        }
        logg(regelnavn, format, arguments);
    }

    /** Skriver en melding til logg. */
    public static void logg(String regelnavn, String format, Object... arguments) {
        log.info(regelnavn + ": " + format, arguments);
    }

    /** Legger til en feil eller varsel på responsen som skal returneres. */
    private static void leggTilMelding(Kategori kat, String melding) {
        Feilmelding feil = new Feilmelding();
        feil.kategori = kat;
        feil.feilmelding = melding;
        respons().feilmeldinger.add(feil);
    }
    
    /** Legger til en feil eller varsel på responsen som skal returneres, og skriver til logg. */
    public static void leggTilMeldingOgLogg(Kategori kat, String melding) {
        leggTilMelding(kat, melding);
        logg("Setter " + (kat.getAlvorlighetsgrad() == Alvorlighetsgrad.FEIL ? "feil" : "varsel") + ": " + melding);
    }

    /** Returnerer true hvis vi har fått en feilmelding. */
    public static boolean detErMeldtFeil() {
        for (Feilmelding feil : respons().feilmeldinger) {
            if (feil.kategori.getAlvorlighetsgrad() == Alvorlighetsgrad.FEIL) {
                return true;
            }
        }
        return false;
    }
    
    /** Legger til em lovvalgsbestemmelse (med tilhørende resultat av evaluering). */
    public static void leggTilLovvalgsbestemmelse(Lovvalgsbestemmelse lovvalgsbestemmelse) {
        respons().lovvalgsbestemmelser.add(lovvalgsbestemmelse);
    }

}
