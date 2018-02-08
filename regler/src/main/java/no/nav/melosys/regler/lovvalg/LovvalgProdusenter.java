package no.nav.melosys.regler.lovvalg;

import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.arbeidsforholdDokumentene;
import static no.nav.melosys.regler.motor.voc.VerdielementSett.alle;

import java.util.function.Function;

import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.PermisjonOgPermittering;
import no.nav.melosys.regler.motor.voc.VerdielementSett;

/**
 * Verbalisering av produsenter.
 * 
 * Disse metodene støtter verbalisering av typen
 * 
 * forAlle(arbeidsforholdene).sine(permitteringer)...
 * 
 */
public class LovvalgProdusenter {
    
    private LovvalgProdusenter() {}
    
    /** Gir alle permisjonene til et arbeidsforhold */
    public static final Function<Arbeidsforhold, Iterable<PermisjonOgPermittering>> permitteringer = Arbeidsforhold::getPermisjonOgPermittering;
    
    /** Gir alle arbeidsforhold */
    public static final VerdielementSett<Arbeidsforhold, ?> arbeidsforholdene() {
        return alle(arbeidsforholdDokumentene()).sine(ArbeidsforholdDokument::getArbeidsforhold);
    }

}
