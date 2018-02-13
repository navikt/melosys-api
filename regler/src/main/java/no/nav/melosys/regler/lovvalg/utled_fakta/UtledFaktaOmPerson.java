package no.nav.melosys.regler.lovvalg.utled_fakta;

import no.nav.melosys.regler.motor.Regelpakke;
import no.nav.melosys.regler.motor.voc.Predikat;

import static no.nav.melosys.regler.api.lovvalg.rep.Argument.BRUKER_ER_MEDLEM_AV_FTRL_MÅNEDEN_FØR_PERIODESTART;
import static no.nav.melosys.regler.api.lovvalg.rep.Kategori.DELVIS_STOETTET;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.leggTilMelding;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.settArgument;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.*;
import static no.nav.melosys.regler.lovvalg.LovvalgPredikater.*;
import static no.nav.melosys.regler.lovvalg.LovvalgProdusenter.arbeidsforhold;
import static no.nav.melosys.regler.lovvalg.LovvalgProdusenter.inntekter;
import static no.nav.melosys.regler.lovvalg.LovvalgProdusenter.medlemsperioder;
import static no.nav.melosys.regler.motor.voc.Deklarasjon.hvis;
import static no.nav.melosys.regler.motor.voc.FellesVokabular.JA;
import static no.nav.melosys.regler.motor.voc.Predikat.minstEttAvFølgendeErSant;
import static no.nav.melosys.regler.motor.voc.Verdielement.verdien;
import static no.nav.melosys.regler.motor.voc.VerdielementSett.alle;

public class UtledFaktaOmPerson implements Regelpakke {

    @Regel
    public static void sjekkOmBrukerenVarMedlemAvFtrMånedenFørPeriodestart() {
        // FIXME (MELOSYS-755): Ikke implementert. Se https://confluence.adeo.no/pages/viewpage.action?pageId=255102083
        hvis(
                minstEttAvFølgendeErSant(
                        brukerenHarIkkeAdresseIUtlandet,
                        brukerenVarIJobbINorgeMånedenFørPeriodestart,
                        brukerenVarMedlemAvFtrlMånedenFørPeriodestartIFølgeMEDL
                )
        ).så(
                settArgument(BRUKER_ER_MEDLEM_AV_FTRL_MÅNEDEN_FØR_PERIODESTART, JA)
        ).ellers(
                leggTilMelding(DELVIS_STOETTET, "Kan ikke fastslå om bruker var medlem av ftrl måneden før utenlandsopphold.")
        );
    }
   
    @Regel
    public static void giVarselHvisInntektOpptjentIUtlandet() {
        hvis(
            brukerenHarInntektOpptjentIUtlandet
        ).så(
            // FIXME: Sjekk om meldingen duger
            leggTilMelding(DELVIS_STOETTET, "Bruker har inntekt opptjent i utlandet.")
        );
    }

    @Regel
    public static void giVarselHvisTvilOmBostedsland() {
        // TODO (farjam 2017-12-21): Denne kan pr. i dag ikke implementeres på bakgrunn av innhentet data
    }

    private static final Predikat brukerenHarInntektOpptjentIUtlandet
        = alle(inntektDokumentene()).sine(inntekter).inneholderMinstEn(inntektOpptjentIUtlandet);

    private static final Predikat brukerenVarIJobbINorgeMånedenFørPeriodestart
        = alle(arbeidsforholdDokumentene()).sine(arbeidsforhold).inneholderMinstEn(ansattINorgeFørPeriodestart);

    private static final Predikat brukerenVarMedlemAvFtrlMånedenFørPeriodestartIFølgeMEDL
        = alle(medlemskapDokumentene()).sine(medlemsperioder).inneholderMinstEn(medlemFørPeriodestart);

    private static final Predikat brukerenHarIkkeAdresseIUtlandet
        // FIXME: Postadresse og midlertidig postadresse implementeres etter merge med develop
        = verdien(personopplysningDokumentet().bostedsadresse).oppfyller(bostedsadresseErINorge);
}
