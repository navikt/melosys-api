package no.nav.melosys.regler.lovvalg.utled_fakta;

import static no.nav.melosys.regler.api.lovvalg.rep.Argument.ANTALL_UTLAND_BRUKER_ARBEIDER_I;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.LENGDE_MND_UTENLANDSOPPHOLD;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.settArgument;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.søknadDokumentet;
import static no.nav.melosys.regler.lovvalg.LovvalgVerdielementer.antallMånederI;
import static no.nav.melosys.regler.motor.voc.FellesVokabular.utfør;
import static no.nav.melosys.regler.motor.voc.Verdielement.antallet;
import static no.nav.melosys.regler.motor.voc.VerdielementSett.alle;

import no.nav.melosys.regler.motor.Regelpakke;

public class UtledFaktaOmUtenlandsopphold implements Regelpakke {
    
    /**
     * FIXME: Funksjonell beskrivelse?? Minstekrav til andel inntekt i hvert land?
     */
    @Regel
    public static void settAntallLandBrukerArbeiderI() {
        utfør(
            settArgument(ANTALL_UTLAND_BRUKER_ARBEIDER_I, antallet(alle(søknadDokumentet().arbeidUtland.arbeidsland).somErUnike()))
        );
    }

    @Regel
    public static void settLengdeUtenlandsopphold() {
        utfør(
            settArgument(LENGDE_MND_UTENLANDSOPPHOLD, antallMånederI(søknadDokumentet().arbeidUtland.arbeidsperiode))
        );
    }

}
