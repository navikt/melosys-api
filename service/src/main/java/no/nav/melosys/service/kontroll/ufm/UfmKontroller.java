package no.nav.melosys.service.kontroll.ufm;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.service.kontroll.*;

final class UfmKontroller {
    private UfmKontroller() {
    }

    static Kontroll_begrunnelser feilIPeriode(UfmKontrollData kontrollData) {
        return PeriodeKontroller.feilIPeriode(
            kontrollData.getSedDokument().getLovvalgsperiode().getFom(), kontrollData.getSedDokument().getLovvalgsperiode().getTom()) ?
            Kontroll_begrunnelser.FEIL_I_PERIODEN : null;
    }

    static Kontroll_begrunnelser periodeErÅpen(UfmKontrollData kontrollData) {
        return PeriodeKontroller.periodeErÅpen(
            kontrollData.getSedDokument().getLovvalgsperiode().getFom(), kontrollData.getSedDokument().getLovvalgsperiode().getTom()) ?
            Kontroll_begrunnelser.INGEN_SLUTTDATO : null;
    }

    static Kontroll_begrunnelser periodeOver24Mnd(UfmKontrollData kontrollData) {
        return PeriodeKontroller.periodeOver24Mnd(
            kontrollData.getSedDokument().getLovvalgsperiode().getFom(), kontrollData.getSedDokument().getLovvalgsperiode().getTom()) ?
            Kontroll_begrunnelser.PERIODEN_OVER_24_MD : null;
    }

    static Kontroll_begrunnelser periodeOver5År(UfmKontrollData kontrollData) {
        return PeriodeKontroller.periodeOver5År(
            kontrollData.getSedDokument().getLovvalgsperiode().getFom(), kontrollData.getSedDokument().getLovvalgsperiode().getTom()) ?
                Kontroll_begrunnelser.PERIODEN_OVER_5_AR : null;
    }

    static Kontroll_begrunnelser periodeStarterFørFørsteJuni2012(UfmKontrollData kontrollData) {
        return PeriodeKontroller.datoErFørFørsteJuni2012(kontrollData.getSedDokument().getLovvalgsperiode().getFom())
            ? Kontroll_begrunnelser.PERIODE_FOR_GAMMEL : null;
    }

    static Kontroll_begrunnelser periodeOver1ÅrFremITid(UfmKontrollData kontrollData) {
        return PeriodeKontroller.datoOver1ÅrFremITid(kontrollData.getSedDokument().getLovvalgsperiode().getFom()) ?
            Kontroll_begrunnelser.PERIODE_LANGT_FREM_I_TID : null;
    }

    static Kontroll_begrunnelser utbetaltYtelserFraOffentligIPeriode(UfmKontrollData kontrollData) {
        LocalDate fom = kontrollData.getSedDokument().getLovvalgsperiode().getFom();
        LocalDate tom = kontrollData.getSedDokument().getLovvalgsperiode().getTom();
        return YtelseKontroller.utbetaltYtelserFraOffentligIPeriode(kontrollData.getInntektDokument(), fom, tom) ?
            Kontroll_begrunnelser.MOTTAR_YTELSER : null;
    }

    static Kontroll_begrunnelser utbetaltBarnetrygdytelser(UfmKontrollData kontrollData) {
        if (kontrollData.getUtbetalingDokument() == null) {
            return null;
        }

        return YtelseKontroller.utbetaltBarnetrygdytelser(kontrollData.getUtbetalingDokument()) ?
            Kontroll_begrunnelser.MOTTAR_YTELSER : null;
    }

    static Kontroll_begrunnelser lovvalgslandErNorge(UfmKontrollData kontrollData) {
        return MedlemskapKontroller.lovvalgslandErNorge(kontrollData.getSedDokument().getLovvalgslandKode()) ?
            Kontroll_begrunnelser.LOVVALGSLAND_NORGE : null;
    }

    static Kontroll_begrunnelser overlappendeMedlemsperiode(UfmKontrollData kontrollData) {
        LocalDate fom = kontrollData.getSedDokument().getLovvalgsperiode().getFom();
        LocalDate tom = kontrollData.getSedDokument().getLovvalgsperiode().getTom();
        return MedlemskapKontroller.overlappendeMedlemsperiodeIkkeAvvistPeriode(fom, tom, kontrollData.getMedlemskapDokument()) ?
            Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER : null;
    }

    static Kontroll_begrunnelser statsborgerskapIkkeMedlemsland(UfmKontrollData kontrollData) {
        return MedlemskapKontroller.avsenderErNordiskEllerAvtaleland(kontrollData.getSedDokument().getAvsenderLandkode())
            || MedlemskapKontroller.erStatsløs(kontrollData.getSedDokument().getStatsborgerskapKoder())
            || MedlemskapKontroller.statsborgerskapErMedlemsland(kontrollData.getSedDokument().getStatsborgerskapKoder()) ?
            null : Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND;
    }

    static Kontroll_begrunnelser personDød(UfmKontrollData kontrollData) {
        return PersonKontroller.personDød(kontrollData.getPersonDokument()) ?
            Kontroll_begrunnelser.PERSON_DOD : null;
    }

    static Kontroll_begrunnelser personBosattINorge(UfmKontrollData kontrollData) {
        return PersonKontroller.personBosattINorge(kontrollData.getPersonDokument()) ?
            Kontroll_begrunnelser.BOSATT_I_NORGE : null;
    }

    static Kontroll_begrunnelser arbeidssted(UfmKontrollData kontrollData) {
        return ArbeidsstedKontroller.arbeidstedSvalbardOgJanMayen(kontrollData.getSedDokument()) ?
            Kontroll_begrunnelser.ARBEIDSSTED_UTENFOR_EOS : null;
    }
}
