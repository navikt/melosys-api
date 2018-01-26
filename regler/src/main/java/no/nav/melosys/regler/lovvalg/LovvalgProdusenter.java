package no.nav.melosys.regler.lovvalg;

import java.util.function.Function;

import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.PermisjonOgPermittering;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.regler.motor.voc.VerdielementSett;

import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.arbeidsforholdDokumentene;
import static no.nav.melosys.regler.motor.voc.VerdielementSett.alle;

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

    /** Gir alle arbeidsforholdene til et arbeidsforholddokument */
    public static final Function<ArbeidsforholdDokument, Iterable<Arbeidsforhold>> arbeidsforhold = ArbeidsforholdDokument::getArbeidsforhold;

    /** Gir alle medlemsperiodene til et medlemskapdokument */
    public static final Function<MedlemskapDokument, Iterable<Medlemsperiode>> medlemsperioder = MedlemskapDokument::getMedlemsperiode;

}
