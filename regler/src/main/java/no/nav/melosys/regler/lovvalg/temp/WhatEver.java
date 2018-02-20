package no.nav.melosys.regler.lovvalg.temp;

import no.nav.melosys.regler.motor.Regelpakke;

import static no.nav.melosys.regler.api.lovvalg.rep.Argument.*;
import static no.nav.melosys.regler.api.lovvalg.rep.Artikkel.ART_12_1;
import static no.nav.melosys.regler.api.lovvalg.rep.Kategori.DELVIS_STOETTET;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.*;
import static no.nav.melosys.regler.lovvalg.LovvalgKrav.*;
import static no.nav.melosys.regler.motor.voc.FellesVokabular.JA;
import static no.nav.melosys.regler.motor.voc.FellesVokabular.utfør;

// FIXME: Slett denne pakken
public class WhatEver implements Regelpakke {

    @Regel
    public static void svar_12_1_samma_hva() {
        utfør(
            leggTilMelding(DELVIS_STOETTET, "Bryr meg ikke... 12.1 samma hva."),
            
            settArgument(ARBEIDSPLASSEN_I_UTLANDET_DEKKES_AV_EF_883_2004, JA),
            settArgument(BRUKER_HAR_NORSK_ARBEIDSGIVER, JA),
            settArgument(ANTALL_ARBEIDSGIVERE_I_SØKNADSPERIODEN, 1),
            settArgument(HOVEDARBEIDSFORHOLDET_VARER_I_HELE_SØKNADSPERIODEN, JA),
            settArgument(LENGDE_MND_UTENLANDSOPPHOLD, 12),
            settArgument(BRUKER_ER_MEDLEM_AV_FTRL_MÅNEDEN_FØR_PERIODESTART, JA)
        );
        
        utfør(
            opprettLovvalgbestemmelse(
                ART_12_1,
                betingelse(ARBEIDSPLASSEN_I_UTLANDET_DEKKES_AV_EF_883_2004, erSann), // Krav 1
                betingelse(BRUKER_HAR_NORSK_ARBEIDSGIVER, erSann), // Krav 2
                betingelse(ANTALL_ARBEIDSGIVERE_I_SØKNADSPERIODEN, erLik(1)), // Krav 3
                betingelse(HOVEDARBEIDSFORHOLDET_VARER_I_HELE_SØKNADSPERIODEN, erSann), // Krav 4
                betingelse(LENGDE_MND_UTENLANDSOPPHOLD, erMindreEnnEllerLik(24)), // Krav 5
                betingelse(BRUKER_ER_MEDLEM_AV_FTRL_MÅNEDEN_FØR_PERIODESTART, erSann) // Krav 7
            ),
            
            avbrytRegelkjøring
        );

    }
}
