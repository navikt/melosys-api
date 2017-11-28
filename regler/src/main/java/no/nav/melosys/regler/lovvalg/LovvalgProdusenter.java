package no.nav.melosys.regler.lovvalg;

import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.arbeidsforholdDokumentene;
import static no.nav.melosys.regler.motor.dekl.VerdielementSett.alle;

import java.util.function.Function;

import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.PermisjonOgPermittering;
import no.nav.melosys.regler.motor.dekl.VerdielementSett;

/**
 * Klassen inneholder verbalisering av produsenter
 */
public class LovvalgProdusenter {
    
    private LovvalgProdusenter() {}
    
    /** Gir alle permisjonene til et arbeidsforhold */
    public static Function<Arbeidsforhold, Iterable<PermisjonOgPermittering>> permitteringer = Arbeidsforhold::getPermisjonOgPermittering;
    
    /** Gir alle arbeidsforhold */
    public static VerdielementSett<Arbeidsforhold, ?> arbeidsforholdene() {
        return alle(arbeidsforholdDokumentene()).sine(ArbeidsforholdDokument::getArbeidsforhold);
    }

}
