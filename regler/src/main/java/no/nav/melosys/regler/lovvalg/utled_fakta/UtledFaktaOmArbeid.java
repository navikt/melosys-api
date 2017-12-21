package no.nav.melosys.regler.lovvalg.utled_fakta;

import static no.nav.melosys.regler.api.lovvalg.rep.Argument.BRUKER_ARBEIDER_I_NORGE;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.BRUKER_ER_ARBEIDSTAKER;
import static no.nav.melosys.regler.api.lovvalg.rep.Kategori.DELVIS_STOETTET;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.leggTilMelding;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.settArgument;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.søknadDokumentet;
import static no.nav.melosys.regler.lovvalg.LovvalgPredikater.harOverlappMedSøknadsperioden;
import static no.nav.melosys.regler.lovvalg.LovvalgProdusenter.arbeidsforholdene;
import static no.nav.melosys.regler.lovvalg.LovvalgProdusenter.permitteringer;
import static no.nav.melosys.regler.motor.voc.Deklarasjon.hvis;
import static no.nav.melosys.regler.motor.voc.FellesVokabular.JA;
import static no.nav.melosys.regler.motor.voc.FellesVokabular.NEI;
import static no.nav.melosys.regler.motor.voc.Verdielement.antallet;
import static no.nav.melosys.regler.motor.voc.Verdielement.verdien;

import no.nav.melosys.regler.motor.Regelpakke;

public class UtledFaktaOmArbeid implements Regelpakke {
    
    /**
     * Gir et varsel dersom bruker har en permittering som overlapper med søknadsperioden.
     */
    @Regel
    public static void giVarselHvisPermitteringISøknadsperioden() {
        hvis(
            antallet(arbeidsforholdene().sine(permitteringer).som(harOverlappMedSøknadsperioden)).erStørreEnnEllerLik(1))
        .så(leggTilMelding(DELVIS_STOETTET, "Bruker har en eller flere permitteringer som overlapper med søknadsperioden. Disse ignoreres av systemet."));
    }
    
    /**
     * Bestemmer om bruker er arbeidstaker eller ikke.
     * Setter variabelen BRUKER_ER_ARBEIDSTAKER
     * FIXME: Ikke avklart. Se https://confluence.adeo.no/pages/viewpage.action?pageId=255102083
     */
    @Regel
    public static void finnUtOmBrukerErArbeidstaker() {
        hvis(
            antallet(arbeidsforholdene().som(harOverlappMedSøknadsperioden)).erStørreEnnEllerLik(1)
        ).så(
            settArgument(BRUKER_ER_ARBEIDSTAKER, JA)
        ).ellers(
            settArgument(BRUKER_ER_ARBEIDSTAKER, NEI)
        );
    }

    /**
     * Fastslår om bruker arbeider i Norge eller ikke
     */
    @Regel
    public static void finnUtOmBrukerArbeiderINorge() {
        // FIXME: Ikke riktig implementert...
        hvis(
            verdien(søknadDokumentet().arbeidNorge).harVerdi()
        ).så(
            settArgument(BRUKER_ARBEIDER_I_NORGE, JA)
        ).ellers(
            settArgument(BRUKER_ARBEIDER_I_NORGE, NEI)
        );
    }
    
}
