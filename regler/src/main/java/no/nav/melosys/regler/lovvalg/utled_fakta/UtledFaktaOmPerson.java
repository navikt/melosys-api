package no.nav.melosys.regler.lovvalg.utled_fakta;

import java.util.function.Function;
import java.util.function.Predicate;

import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.regler.motor.Regelpakke;
import no.nav.melosys.regler.motor.voc.Predikat;

import static no.nav.melosys.regler.api.lovvalg.rep.Argument.BRUKER_ER_MEDLEM_AV_FTRL_MÅNEDEN_FØR_PERIODESTART;
import static no.nav.melosys.regler.api.lovvalg.rep.Kategori.DELVIS_STOETTET;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.leggTilMelding;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.settArgument;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.*;
import static no.nav.melosys.regler.motor.voc.Deklarasjon.hvis;
import static no.nav.melosys.regler.motor.voc.FellesVokabular.JA;
import static no.nav.melosys.regler.motor.voc.Predikat.minstEttAvFølgendeErSant;
import static no.nav.melosys.regler.motor.voc.VerdielementSett.alle;

public class UtledFaktaOmPerson implements Regelpakke {

    @Regel
    public static void sjekkOmBrukerenVarMedlemAvFtrMånedenFørPeriodestart() {
        // FIXME (MELOSYS-755): Ikke implementert. Se https://confluence.adeo.no/pages/viewpage.action?pageId=255102083
        hvis(
                minstEttAvFølgendeErSant(
                        // FIXME: brukerenHarIkkeBostedsadresseIUtlandetSiste12Måneder
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
        // FIXME (MELOSYS-755): Ikke implementert. Se https://confluence.adeo.no/pages/viewpage.action?pageId=255102083
    }

    @Regel
    public static void giVarselHvisTvilOmBostedsland() {
        // TODO (farjam 2017-12-21): Denne kan pr. i dag ikke implementeres på bakgrunn av innhentet data
    }

    private static Function<ArbeidsforholdDokument, Iterable<Arbeidsforhold>> arbeidsforhold = ArbeidsforholdDokument::getArbeidsforhold;

    private static Function<MedlemskapDokument, Iterable<Medlemsperiode>> medlemsperioder = MedlemskapDokument::getMedlemsperiode;

    private static Predicate<Arbeidsforhold> gyldigFørPeriodestart = (Arbeidsforhold arbeidsforhold)
        // FIXME: Bedre verbalisering
        -> søknadsperioden().starterPåEllerEtter(arbeidsforhold.getPeriode().getFom()).test();

    private static Predicate<Medlemsperiode> medlemskapFørPeriodestart = (Medlemsperiode medlemsperiode)
        // FIXME: Bedre verbalisering
        -> søknadsperioden().starterPåEllerEtter(medlemsperiode.getPeriode().getFom()).test();

    private static Predikat brukerenVarIJobbINorgeMånedenFørPeriodestart
        = alle(arbeidsforholdDokumentene()).sine(arbeidsforhold).inneholderMinstEn(gyldigFørPeriodestart);

    private static final Predikat brukerenVarMedlemAvFtrlMånedenFørPeriodestartIFølgeMEDL
        // FIXME: Sjekk om medlemskapet gjelder ftrl
        = alle(medlemskapDokumentene()).sine(medlemsperioder).inneholderMinstEn(medlemskapFørPeriodestart);

}
