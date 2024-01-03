package no.nav.melosys.service.kontroll.feature.ufm.kontroll;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.SedGrunnlag;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.service.kontroll.feature.ufm.data.UfmKontrollData;
import no.nav.melosys.service.kontroll.regler.*;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static no.nav.melosys.service.kontroll.regler.OverlappendeMedlemskapsperioderRegler.*;
import static org.apache.cxf.common.util.StringUtils.isEmpty;

class UfmKontroll {

    private static final Logger log = LoggerFactory.getLogger(UfmKontroll.class);

    private UfmKontroll() {
    }

    static Kontroll_begrunnelser feilIPeriode(UfmKontrollData kontrollData) {
        return PeriodeRegler.feilIPeriode(
            kontrollData.sedDokument().getLovvalgsperiode().getFom(), kontrollData.sedDokument().getLovvalgsperiode().getTom()) ?
            Kontroll_begrunnelser.FEIL_I_PERIODEN : null;
    }

    static Kontroll_begrunnelser periodeErÅpen(UfmKontrollData kontrollData) {
        return PeriodeRegler.periodeErÅpen(
            kontrollData.sedDokument().getLovvalgsperiode().getFom(), kontrollData.sedDokument().getLovvalgsperiode().getTom()) ?
            Kontroll_begrunnelser.INGEN_SLUTTDATO : null;
    }

    static Kontroll_begrunnelser periodeOver24MånederOgEnDag(UfmKontrollData kontrollData) {
        LocalDate fom = kontrollData.sedDokument().getLovvalgsperiode().getFom();
        LocalDate tom = kontrollData.sedDokument().getLovvalgsperiode().getTom();
        return PeriodeRegler.periodeOver2ÅrOgEnDag(fom, tom) ? Kontroll_begrunnelser.PERIODEN_OVER_24_MD : null;
    }

    static Kontroll_begrunnelser periodeOver5År(UfmKontrollData kontrollData) {
        return PeriodeRegler.periodeOver5År(
            kontrollData.sedDokument().getLovvalgsperiode().getFom(), kontrollData.sedDokument().getLovvalgsperiode().getTom()) ?
            Kontroll_begrunnelser.PERIODEN_OVER_5_AR : null;
    }

    static Kontroll_begrunnelser periodeStarterFørFørsteJuni2012(UfmKontrollData kontrollData) {
        return PeriodeRegler.datoErFørFørsteJuni2012(kontrollData.sedDokument().getLovvalgsperiode().getFom())
            ? Kontroll_begrunnelser.PERIODE_FOR_GAMMEL : null;
    }

    static Kontroll_begrunnelser periodeOver1ÅrFremITid(UfmKontrollData kontrollData) {
        return PeriodeRegler.datoOver1ÅrFremITid(kontrollData.sedDokument().getLovvalgsperiode().getFom()) ?
            Kontroll_begrunnelser.PERIODE_LANGT_FREM_I_TID : null;
    }

    static Kontroll_begrunnelser utbetaltYtelserFraOffentligIPeriode(UfmKontrollData kontrollData) {
        LocalDate fom = kontrollData.sedDokument().getLovvalgsperiode().getFom();
        LocalDate tom = kontrollData.sedDokument().getLovvalgsperiode().getTom();
        return YtelseRegler.utbetaltYtelserFraOffentligIPeriode(kontrollData.inntektDokument(), fom, tom) ?
            Kontroll_begrunnelser.MOTTAR_YTELSER : null;
    }

    static Kontroll_begrunnelser lovvalgslandErNorge(UfmKontrollData kontrollData) {
        return UfmRegler.lovvalgslandErNorge(kontrollData.sedDokument().getLovvalgslandKode()) ?
            Kontroll_begrunnelser.LOVVALGSLAND_NORGE : null;
    }

    static Kontroll_begrunnelser overlappendeMedlemsperiode(UfmKontrollData kontrollData) {
        return harOverlappendeMedlemsperiodeFraSed(
            kontrollData.medlemskapDokument(), kontrollData.sedDokument().getLovvalgsperiode()) ?
            Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER : null;
    }

    static Kontroll_begrunnelser overlappendeMedlemsperiodeForA003(UfmKontrollData kontrollData) {
        var sedDokument = kontrollData.sedDokument();
        var lovvalgsperiode = sedDokument.getLovvalgsperiode();

        var medlemskapDokument = kontrollData.medlemskapDokument();

        if (harOverlappendeMedlemsperiodeMerEnn1DagFraSed(medlemskapDokument, lovvalgsperiode)) {
            if (sedDokument.erMedlemskapsperiode()) {
                log.info("Mottatt overlappende medlemsperiode med medlemskap for A003");
                return Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER;
            } else if (sedDokument.erUnntaksperiode()) {
                log.info("Mottatt overlappende unntaksperiode uten medlemskap for A003");
                if (sedDokument.getErEndring()) {
                    log.info("Mottatt overlappende unntaksperiode for A003 med en endring");
                    return Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER;
                }
                var optionalMottatteOpplysningerData = kontrollData.mottatteOpplysningerData();
                if (harMottatteOpplysningerMedYtterligereInformasjon(optionalMottatteOpplysningerData)) {
                    log.info("Mottatt overlappende unntaksperiode for A003 med ytterligere informasjon");
                    return Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER;
                }
                if (harOverlappendePerioderMedUlikSedLovvalgslandOgMedlLovvalgsland(sedDokument, medlemskapDokument)) {
                    log.info("Mottatt overlappende unntaksperiode for A003 med ulike lovvalgsland i SED og MEDL");
                    return Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER;
                }
            }
        }
        return null;
    }

    static Kontroll_begrunnelser overlappendeMedlemsperiodeMerEnn1Dag(UfmKontrollData kontrollData) {
        return harOverlappendeMedlemsperiodeMerEnn1DagFraSed(
            kontrollData.medlemskapDokument(), kontrollData.sedDokument().getLovvalgsperiode()) ?
            Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER : null;
    }

    static Kontroll_begrunnelser statsborgerskapIkkeMedlemsland(UfmKontrollData kontrollData) {
        return UfmRegler.avsenderErNordiskEllerAvtaleland(kontrollData.sedDokument().getAvsenderLandkode())
            || UfmRegler.erStatsløs(kontrollData.sedDokument().getStatsborgerskapKoder())
            || UfmRegler.statsborgerskapErMedlemsland(kontrollData.sedDokument().getStatsborgerskapKoder()) ?
            null : Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND;
    }

    static Kontroll_begrunnelser personDød(UfmKontrollData kontrollData) {
        return PersonRegler.erPersonDød(kontrollData.persondata()) ?
            Kontroll_begrunnelser.PERSON_DOD : null;
    }

    static Kontroll_begrunnelser personBosattINorge(UfmKontrollData kontrollData) {
        return PersonRegler.personBosattINorge(kontrollData.persondata()) ?
            Kontroll_begrunnelser.BOSATT_I_NORGE : null;
    }

    static Kontroll_begrunnelser personBosattINorgeIPerioden(UfmKontrollData kontrollData) {
        log.info("personBosattINorgeIPerioden: Skal sjekke nå");
        LocalDate sedFra = kontrollData.sedDokument().getLovvalgsperiode().getFom();
        LocalDate sedTil = kontrollData.sedDokument().getLovvalgsperiode().getTom();

        var historiskeBostedAdresser = kontrollData.persondataMedHistorikk().isPresent() ? kontrollData.persondataMedHistorikk().get().bostedsadresser() : Collections.EMPTY_LIST;
        var historiskeOppholdsAdresser = kontrollData.persondataMedHistorikk().isPresent() ? kontrollData.persondataMedHistorikk().get().oppholdsadresser() : Collections.EMPTY_LIST;


        var bostedAdressePeriode = kontrollData.personhistorikkDokumenter()
            .stream()
            .flatMap(a -> a.bostedsadressePeriodeListe.stream())
            .collect(Collectors.toList());

        Optional<Bostedsadresse> personBostedsadresse = kontrollData.persondata().finnBostedsadresse();

        personBostedsadresse.ifPresent(bostedsadresse -> log.info("personBosattINorgeIPerioden: Sjekker at personen har bodd i norge under perioden. sedFra: {}, sedTil: {}, boFra: {}, boTil: {}", sedFra, sedTil, bostedsadresse.gyldigFraOgMed(), bostedsadresse.gyldigTilOgMed()));


        log.info("personBosattINorgeIPerioden: historiskeBostedAdresser {}: {}", historiskeBostedAdresser.size(), historiskeBostedAdresser);
        log.info("personBosattINorgeIPerioden: historiskeOppholdsAdresser {}: {}",historiskeOppholdsAdresser.size(), historiskeOppholdsAdresser);
        log.info("personBosattINorgeIPerioden: bostedAdressePeriode {}: {}",bostedAdressePeriode.size(), bostedAdressePeriode);

        return PersonRegler.personBosattINorgeIPeriode(bostedAdressePeriode, personBostedsadresse, historiskeBostedAdresser, historiskeOppholdsAdresser, sedFra, sedTil) ?
            Kontroll_begrunnelser.BOSATT_I_NORGE_I_PERIODEN : null;
    }

    static Kontroll_begrunnelser arbeidssted(UfmKontrollData kontrollData) {
        return ArbeidsstedRegler.erArbeidsstedFraSvalbardOgJanMayen(kontrollData.sedDokument()) ?
            Kontroll_begrunnelser.ARBEIDSSTED_UTENFOR_EOS : null;
    }

    static Kontroll_begrunnelser unntakForA003(UfmKontrollData kontrollData) {
        return !UfmRegler.lovvalgslandErNorge(kontrollData.sedDokument().getLovvalgslandKode())
            && (harTransitiveRegler(kontrollData.mottatteOpplysningerData()) || harOvergangsregler(kontrollData.sedDokument()))
            ? Kontroll_begrunnelser.OVERGANGSREGEL_VALGT : null;
    }

    private static boolean harTransitiveRegler(Optional<MottatteOpplysningerData> optionalMottatteOpplysningerData) {
        return optionalMottatteOpplysningerData.isPresent()
            && !((SedGrunnlag) optionalMottatteOpplysningerData.get()).overgangsregelbestemmelser.isEmpty();
    }

    private static boolean harOvergangsregler(SedDokument sedDokument) {
        return sedDokument.getLovvalgBestemmelse() != null
            && (sedDokument.getLovvalgBestemmelse().equals(Tilleggsbestemmelser_883_2004.FO_883_2004_ART87_8)
                || sedDokument.getLovvalgBestemmelse().equals(Tilleggsbestemmelser_883_2004.FO_883_2004_ART87A));
    }

    private static boolean harMottatteOpplysningerMedYtterligereInformasjon(Optional<MottatteOpplysningerData> optionalMottatteOpplysningerData) {
        return optionalMottatteOpplysningerData.isPresent() && !isEmpty(((SedGrunnlag) optionalMottatteOpplysningerData.get()).ytterligereInformasjon);
    }
}
