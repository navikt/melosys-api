package no.nav.melosys.service.unntaksperiode.kontroll;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.begrunnelser.Unntak_periode_begrunnelser;
import no.nav.melosys.service.kontroll.MedlemskapKontroller;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import no.nav.melosys.service.kontroll.PersonKontroller;
import no.nav.melosys.service.kontroll.YtelseKontroller;

/**
 * Kontroller fra no.nav.melosys.service.kontroll som returnerer begrunnelser for unntaksperiode
 */
final class UnntaksperiodeKontroller {

    private UnntaksperiodeKontroller() {
    }

    static Unntak_periode_begrunnelser feilIPeriode(KontrollData kontrollData) {
        return PeriodeKontroller.feilIPeriode(
            kontrollData.sedDokument.getLovvalgsperiode().getFom(), kontrollData.sedDokument.getLovvalgsperiode().getTom()) ?
            Unntak_periode_begrunnelser.FEIL_I_PERIODEN : null;
    }

    static Unntak_periode_begrunnelser periodeErÅpen(KontrollData kontrollData) {
        return PeriodeKontroller.periodeErÅpen(
            kontrollData.sedDokument.getLovvalgsperiode().getFom(), kontrollData.sedDokument.getLovvalgsperiode().getTom()) ?
            Unntak_periode_begrunnelser.INGEN_SLUTTDATO : null;
    }

    static Unntak_periode_begrunnelser periodeOver24Mnd(KontrollData kontrollData) {
        return PeriodeKontroller.periodeOver24Mnd(
            kontrollData.sedDokument.getLovvalgsperiode().getFom(), kontrollData.sedDokument.getLovvalgsperiode().getTom()) ?
            Unntak_periode_begrunnelser.PERIODEN_OVER_24_MD : null;
    }

    static Unntak_periode_begrunnelser periodeEldreEnn3År(KontrollData kontrollData) {
        return PeriodeKontroller.datoEldreEnn3År(kontrollData.sedDokument.getLovvalgsperiode().getFom()) ?
            Unntak_periode_begrunnelser.PERIODE_FOR_GAMMEL : null;
    }

    static Unntak_periode_begrunnelser periodeOver1ÅrFremITid(KontrollData kontrollData) {
        return PeriodeKontroller.datoOver1ÅrFremITid(kontrollData.sedDokument.getLovvalgsperiode().getFom()) ?
            Unntak_periode_begrunnelser.PERIODE_LANGT_FREM_I_TID : null;
    }

    static Unntak_periode_begrunnelser utbetaltYtelserFraOffentligIPeriode(KontrollData kontrollData) {
        LocalDate fom = kontrollData.sedDokument.getLovvalgsperiode().getFom();
        LocalDate tom = kontrollData.sedDokument.getLovvalgsperiode().getTom();
        return YtelseKontroller.utbetaltYtelserFraOffentligIPeriode(kontrollData.inntektDokument, fom, tom) ?
            Unntak_periode_begrunnelser.MOTTAR_YTELSER : null;
    }

    static Unntak_periode_begrunnelser utbetaltBarnetrygdytelser(KontrollData kontrollData) {
        if (kontrollData.utbetalingDokument == null) {
            return null;
        }

        return YtelseKontroller.utbetaltBarnetrygdytelser(kontrollData.utbetalingDokument) ?
            Unntak_periode_begrunnelser.MOTTAR_YTELSER : null;
    }

    static Unntak_periode_begrunnelser lovvalgslandErNorge(KontrollData kontrollData) {
        return MedlemskapKontroller.lovvalgslandErNorge(kontrollData.sedDokument.getLovvalgslandKode()) ?
            Unntak_periode_begrunnelser.LOVVALGSLAND_NORGE : null;
    }

    static Unntak_periode_begrunnelser overlappendeMedlemsperiode(KontrollData kontrollData) {
        LocalDate fom = kontrollData.sedDokument.getLovvalgsperiode().getFom();
        LocalDate tom = kontrollData.sedDokument.getLovvalgsperiode().getTom();

        return MedlemskapKontroller.overlappendeMedlemsperiode(fom, tom, kontrollData.medlemskapDokument) ?
            Unntak_periode_begrunnelser.OVERLAPPENDE_MEDL_PERIODER : null;
    }

    static Unntak_periode_begrunnelser statsborgerskapIkkeMedlemsland(KontrollData kontrollData) {
        return MedlemskapKontroller.statsborgerskapIkkeMedlemsland(kontrollData.sedDokument.getStatsborgerskapKoder()) ?
            Unntak_periode_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND : null;
    }

    static Unntak_periode_begrunnelser personDød(KontrollData kontrollData) {
        return PersonKontroller.personDød(kontrollData.personDokument) ?
            Unntak_periode_begrunnelser.PERSON_DOD : null;
    }

    static Unntak_periode_begrunnelser personBosattINorge(KontrollData kontrollData) {
        return PersonKontroller.personBosattINorge(kontrollData.personDokument) ?
            Unntak_periode_begrunnelser.BOSATT_I_NORGE : null;
    }
}
