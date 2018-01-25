package no.nav.melosys.regler.lovvalg.vurder_artikler;

import no.nav.melosys.regler.motor.Regelpakke;

import static no.nav.melosys.regler.api.lovvalg.rep.Argument.*;
import static no.nav.melosys.regler.api.lovvalg.rep.Artikkel.ART_12_1;
import static no.nav.melosys.regler.api.lovvalg.rep.Kategori.DELVIS_STOETTET;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.leggTilMelding;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.opprettLovvalgbestemmelse;
import static no.nav.melosys.regler.lovvalg.LovvalgKrav.*;
import static no.nav.melosys.regler.motor.voc.Deklarasjon.hvis;
import static no.nav.melosys.regler.motor.voc.Verdielement.argumentet;

public class VurderArtikkel12_1 implements Regelpakke {
    
    /**
     * Vurder om kravene til 12.1 tilfredsstilles
     * 
     * Kravene til 12.1. er:
     * 1) Den fysiske arbeidsplassen i utlandet er på et sted som omfattes av EU/EØS men som ikke er i Norge
     * 2) Bruker har norsk arbeidsgiver
     * 3) Bruker har kun 1 arbeidsgiver i perioden
     * 4) Arbeidsforholdet skal vare hele utsendelsesperioden
     * 5) Oppholdet varer ikke lenger enn 2 år
     * 6) Brukeren skal ikke erstatte en annen utsendt arbeidstaker
     * 7) Bruker er medlem av FTR måneden før periodestart
     */
    @Regel
    public static void vurderArtikke12_1() {
        hvis(
            argumentet(SKAL_VURDERE_ART_12_1)
        ).så(
            opprettLovvalgbestemmelse(
                ART_12_1,
                betingelse(ARBEIDSPLASSEN_I_UTLANDET_DEKKES_AV_EF_883_2004, erSann), // Krav 1
                betingelse(BRUKER_HAR_NORSK_ARBEIDSGIVER, erSann), // Krav 2
                betingelse(ANTALL_ARBEIDSGIVERE_I_SØKNADSPERIODEN, erLik(1)), // Krav 3
                betingelse(HOVEDARBEIDSFORHOLDET_VARER_I_HELE_SØKNADSPERIODEN, erSann), // Krav 4
                betingelse(LENGDE_MND_UTENLANDSOPPHOLD, erMindreEnnEllerLik(24)), // Krav 5
                // Krav 6 gir heller en varsel, siden 12.1 kanskje skal innvilges likevel.
                betingelse(BRUKER_ER_MEDLEM_AV_FTRL_MÅNEDEN_FØR_PERIODESTART, erSann) // Krav 7
            )
        );
    }
    
    @Regel
    public static void giVarselHvisErstatterAnnen() {
        hvis(
            argumentet(BRUKEREN_SKAL_ERSTATTE_EN_ANNEN_ARBEIDSTAKER)
        ).så(
            leggTilMelding(DELVIS_STOETTET, "Brukeren erstatter en annen utsendt arbeidstaker. 12.1 skal ikke innvilges dersom den totale utsendelsesperioden (for begge arbeidstakere) overstiger 24 måneder.")
        );
    }

}
