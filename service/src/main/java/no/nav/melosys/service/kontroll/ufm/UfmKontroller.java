package no.nav.melosys.service.kontroll.ufm;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.begrunnelser.Unntak_periode_begrunnelser;
import no.nav.melosys.service.kontroll.MedlemskapKontroller;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import no.nav.melosys.service.kontroll.PersonKontroller;
import no.nav.melosys.service.kontroll.YtelseKontroller;

final class UfmKontroller {

    private UfmKontroller() {
    }

    static Unntak_periode_begrunnelser feilIPeriode(UfmKontrollData kontrollData) {
        return PeriodeKontroller.feilIPeriode(
            kontrollData.getSedDokument().getLovvalgsperiode().getFom(), kontrollData.getSedDokument().getLovvalgsperiode().getTom()) ?
            Unntak_periode_begrunnelser.FEIL_I_PERIODEN : null;
    }

    static Unntak_periode_begrunnelser periodeErÅpen(UfmKontrollData kontrollData) {
        return PeriodeKontroller.periodeErÅpen(
            kontrollData.getSedDokument().getLovvalgsperiode().getFom(), kontrollData.getSedDokument().getLovvalgsperiode().getTom()) ?
            Unntak_periode_begrunnelser.INGEN_SLUTTDATO : null;
    }

    static Unntak_periode_begrunnelser periodeOver24Mnd(UfmKontrollData kontrollData) {
        return PeriodeKontroller.periodeOver24Mnd(
            kontrollData.getSedDokument().getLovvalgsperiode().getFom(), kontrollData.getSedDokument().getLovvalgsperiode().getTom()) ?
            Unntak_periode_begrunnelser.PERIODEN_OVER_24_MD : null;
    }

    static Unntak_periode_begrunnelser periodeOver5År(UfmKontrollData kontrollData) {
        return PeriodeKontroller.periodeOver5År(
            kontrollData.getSedDokument().getLovvalgsperiode().getFom(), kontrollData.getSedDokument().getLovvalgsperiode().getTom()) ?
                Unntak_periode_begrunnelser.PERIODEN_OVER_5_AR : null;
    }

    static Unntak_periode_begrunnelser periodeEldreEnn3År(UfmKontrollData kontrollData) {
        return PeriodeKontroller.datoEldreEnn3År(kontrollData.getSedDokument().getLovvalgsperiode().getFom()) ?
            Unntak_periode_begrunnelser.PERIODE_FOR_GAMMEL : null;
    }

    static Unntak_periode_begrunnelser periodeOver1ÅrFremITid(UfmKontrollData kontrollData) {
        return PeriodeKontroller.datoOver1ÅrFremITid(kontrollData.getSedDokument().getLovvalgsperiode().getFom()) ?
            Unntak_periode_begrunnelser.PERIODE_LANGT_FREM_I_TID : null;
    }

    static Unntak_periode_begrunnelser utbetaltYtelserFraOffentligIPeriode(UfmKontrollData kontrollData) {
        LocalDate fom = kontrollData.getSedDokument().getLovvalgsperiode().getFom();
        LocalDate tom = kontrollData.getSedDokument().getLovvalgsperiode().getTom();
        return YtelseKontroller.utbetaltYtelserFraOffentligIPeriode(kontrollData.getInntektDokument(), fom, tom) ?
            Unntak_periode_begrunnelser.MOTTAR_YTELSER : null;
    }

    static Unntak_periode_begrunnelser utbetaltBarnetrygdytelser(UfmKontrollData kontrollData) {
        if (kontrollData.getUtbetalingDokument() == null) {
            return null;
        }

        return YtelseKontroller.utbetaltBarnetrygdytelser(kontrollData.getUtbetalingDokument()) ?
            Unntak_periode_begrunnelser.MOTTAR_YTELSER : null;
    }

    static Unntak_periode_begrunnelser lovvalgslandErNorge(UfmKontrollData kontrollData) {
        return MedlemskapKontroller.lovvalgslandErNorge(kontrollData.getSedDokument().getLovvalgslandKode()) ?
            Unntak_periode_begrunnelser.LOVVALGSLAND_NORGE : null;
    }

    static Unntak_periode_begrunnelser overlappendeMedlemsperiode(UfmKontrollData kontrollData) {
        LocalDate fom = kontrollData.getSedDokument().getLovvalgsperiode().getFom();
        LocalDate tom = kontrollData.getSedDokument().getLovvalgsperiode().getTom();

        return MedlemskapKontroller.overlappendeMedlemsperiodeIkkeAvvistPeriode(fom, tom, kontrollData.getMedlemskapDokument()) ?
            Unntak_periode_begrunnelser.OVERLAPPENDE_MEDL_PERIODER : null;
    }

    static Unntak_periode_begrunnelser statsborgerskapIkkeMedlemsland(UfmKontrollData kontrollData) {
        return MedlemskapKontroller.statsborgerskapIkkeMedlemsland(kontrollData.getSedDokument().getStatsborgerskapKoder()) ?
            Unntak_periode_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND : null;
    }

    static Unntak_periode_begrunnelser personDød(UfmKontrollData kontrollData) {
        return PersonKontroller.personDød(kontrollData.getPersonDokument()) ?
            Unntak_periode_begrunnelser.PERSON_DOD : null;
    }

    static Unntak_periode_begrunnelser personBosattINorge(UfmKontrollData kontrollData) {
        return PersonKontroller.personBosattINorge(kontrollData.getPersonDokument()) ?
            Unntak_periode_begrunnelser.BOSATT_I_NORGE : null;
    }
}
