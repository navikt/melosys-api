package no.nav.melosys.regler.lovvalg;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonopplysningDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.regler.api.lovvalg.req.FastsettLovvalgRequest;
import no.nav.melosys.regler.motor.KontekstManager;

/**
 * Kontekst (og -manager) for lovvalgregler.
 * 
 * Tilbyr verbalisering for tilgang til søknaden og responsen.
 * 
 * Konteksten er bundet til tråden den kjører på, slik at regelsett kan kalles i parallell.
 * 
 */
public class LovvalgKontekstManager {

    private static Logger log = LoggerFactory.getLogger(LovvalgKontekstManager.class);

    private static ThreadLocal<FastsettLovvalgRequest> lokalFastsettLovvalgRequest = new ThreadLocal<>();
    private static ThreadLocal<FastsettLovvalgReply> lokalFastsettLovvalgRespons = new ThreadLocal<>();

    /** Initialiserer regelkjøringens kontekst. Må gjøres før man oppretter eller kjører regler. */
    public static void initialiserLokalKontekst(FastsettLovvalgRequest req) {
        if (lokalFastsettLovvalgRequest.get() != null) {
            log.error("Forsøk på å sette kontekst før eksisterende kontekst er slettet.");
            throw new RuntimeException("Forsøk på å sette kontekst før eksisterende kontekst er slettet.");
        }
        KontekstManager.initialiserLokalKontekst();
        lokalFastsettLovvalgRequest.set(req);
        FastsettLovvalgReply rep = new FastsettLovvalgReply();
        rep.feilmeldinger = new ArrayList<>();
        rep.lovvalgsbestemmelser = new ArrayList<>();
        lokalFastsettLovvalgRespons.set(rep);
    }
    
    /** Sletter regelkjøringens kontekst. Må gjøres etter at alle regelsettene er kjørt. */
    public static void slettLokalKontekst() {
        lokalFastsettLovvalgRequest.set(null);
        lokalFastsettLovvalgRespons.set(null);
        KontekstManager.slettLokalKontekst();
    }

    /** Returnerer søknaden fra input. */
    public static SoeknadDokument søknadDokumentet() {
        return lokalFastsettLovvalgRequest.get().søknadDokument;
    }

    /** Returnerer personopplysnng fra input. */
    public static PersonopplysningDokument personopplysningDokumentet() {
        return lokalFastsettLovvalgRequest.get().personopplysningDokument;
    }

    /** Returnerer arbeidsforholdDokumenter fra input. */
    public static List<ArbeidsforholdDokument> arbeidsforholdDokumentene() {
        return lokalFastsettLovvalgRequest.get().arbeidsforholdDokumenter;
    }
    
    /** Returnerer inntektDokumenter fra input. */
    public static List<InntektDokument> inntektDokumentene() {
        return lokalFastsettLovvalgRequest.get().inntektDokumenter;
    }
    
    /** Returnerer medlemskapDokumenter fra input. */
    public static List<MedlemskapDokument> medlemskapDokumentene() {
        return lokalFastsettLovvalgRequest.get().medlemskapDokumenter;
    }
    
    /** Returnerer organisasjonDokumenter fra input. */
    public static List<OrganisasjonDokument> organisasjonDokumentene() {
        return lokalFastsettLovvalgRequest.get().organisasjonDokumenter;
    }
    
    /** Returnerer FastsettLovvalgRespons som regelsettet jobber med. */
    public static FastsettLovvalgReply responsen() {
        return lokalFastsettLovvalgRespons.get();
    }

}
