package no.nav.melosys.regler.lovvalg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
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

    private static final Logger log = LoggerFactory.getLogger(LovvalgKontekstManager.class);

    private static final ThreadLocal<FastsettLovvalgRequest> lokalFastsettLovvalgRequest = new ThreadLocal<>();
    private static final ThreadLocal<FastsettLovvalgReply> lokalFastsettLovvalgRespons = new ThreadLocal<>();

    /** Initialiserer regelkjøringens kontekst. Må gjøres før man oppretter eller kjører regler. */
    public static final void initialiserLokalKontekst(FastsettLovvalgRequest req) {
        if (lokalFastsettLovvalgRequest.get() != null) {
            log.error("Forsøk på å sette kontekst før eksisterende kontekst er slettet.");
            throw new RuntimeException("Forsøk på å sette kontekst før eksisterende kontekst er slettet.");
        }
        KontekstManager.initialiserLokalKontekst();
        lokalFastsettLovvalgRequest.set(req);
        FastsettLovvalgReply rep = new FastsettLovvalgReply();
        rep.feilmeldinger = new ArrayList<>();
        rep.lovvalgsbestemmelser = new HashMap<>();
        lokalFastsettLovvalgRespons.set(rep);
    }
    
    /** Sletter regelkjøringens kontekst. Må gjøres etter at alle regelsettene er kjørt. */
    public static final void slettLokalKontekst() {
        lokalFastsettLovvalgRequest.set(null);
        lokalFastsettLovvalgRespons.set(null);
        KontekstManager.slettLokalKontekst();
    }

    /** Returnerer søknaden fra input. */
    public static final SoeknadDokument søknadDokumentet() {
        return lokalFastsettLovvalgRequest.get().søknadDokument;
    }

    /** Returnerer personopplysnng fra input. */
    public static final PersonDokument personopplysningDokumentet() {
        return lokalFastsettLovvalgRequest.get().personopplysningDokument;
    }

    /** Returnerer arbeidsforholdDokumenter fra input. */
    public static final List<ArbeidsforholdDokument> arbeidsforholdDokumentene() {
        return lokalFastsettLovvalgRequest.get().arbeidsforholdDokumenter;
    }
    
    /** Returnerer inntektDokumenter fra input. */
    public static final List<InntektDokument> inntektDokumentene() {
        return lokalFastsettLovvalgRequest.get().inntektDokumenter;
    }
    
    /** Returnerer medlemskapDokumenter fra input. */
    public static final List<MedlemskapDokument> medlemskapDokumentene() {
        return lokalFastsettLovvalgRequest.get().medlemskapDokumenter;
    }
    
    /** Returnerer organisasjonDokumenter fra input. */
    public static final List<OrganisasjonDokument> organisasjonDokumentene() {
        return lokalFastsettLovvalgRequest.get().organisasjonDokumenter;
    }
    
    /** Returnerer FastsettLovvalgRespons som regelsettet jobber med. */
    public static final FastsettLovvalgReply responsen() {
        return lokalFastsettLovvalgRespons.get();
    }

}
