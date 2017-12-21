package no.nav.melosys.regler.lovvalg.utled_fakta;

import static no.nav.melosys.regler.api.lovvalg.rep.Argument.BRUKER_ER_MEDLEM_AV_FTR_MÅNEDEN_FØR_PERIODESTART;
import static no.nav.melosys.regler.api.lovvalg.rep.Kategori.DELVIS_STOETTET;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.leggTilMelding;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.settArgument;
import static no.nav.melosys.regler.motor.voc.FellesVokabular.JA;
import static no.nav.melosys.regler.motor.voc.FellesVokabular.utfør;

import no.nav.melosys.regler.motor.Regelpakke;

public class UtledFaktaOmPerson implements Regelpakke {

    @Regel
    public static void sjekkOmBrukerenVarMedlemAvFtrMånedenFørPeriodestart() {
        // FIXME (MELOSYS-755): Ikke implementert. Se https://confluence.adeo.no/pages/viewpage.action?pageId=255102083
        utfør(
            settArgument(BRUKER_ER_MEDLEM_AV_FTR_MÅNEDEN_FØR_PERIODESTART, JA),
            leggTilMelding(DELVIS_STOETTET, "Kan ikke fastslå om bruker var medlem av FTR måneden før utenlandsopphold.")
        );
    }
   
    @Regel
    public static void giVarselHvisInntektOpptjentIUtlandet() {
        // FIXME (MELOSYS-755): Ikke implementert. Se https://confluence.adeo.no/pages/viewpage.action?pageId=255102083
    }

    @Regel
    public static void giVarselHvisTvilOmBostedsland() {
        // TODO (farjam 2017-12-21): Denne kan pr. i dag ikke implementeres på bakgrunn av innhentet data
    }

}
