package no.nav.melosys.service.kontroll.overlapp;

import java.time.LocalDate;

import static java.util.Objects.isNull;

public class PeriodeOverlappMedEnDag {
    public static boolean periodeOverlapperMerEnn1Dag(LocalDate fom1, LocalDate tom1, LocalDate fom2, LocalDate tom2) {

        validerFraOgMedDato(fom1, fom2);
        validerTilOgMedDato(tom1, tom2);

        if (datoErLik(fom1, fom2) || datoErLik(tom1, tom2) || datoErLik(fom1, tom2) || datoErLik(tom1, fom2)) {
            return true;
        }

        if (tom1 == null) {
            return åpenPeriodeOverlapper(fom2, tom2, fom1);
        } else if (tom2 == null) {
            return åpenPeriodeOverlapper(fom1, tom1, fom2);
        }

        return fom1.isBefore(fom2) && tom1.isAfter(fom2) || fom1.isAfter(fom2) && fom1.isBefore(tom2);
    }

    private static void validerTilOgMedDato(LocalDate tom1, LocalDate tom2) {
        if (tom1 == null && tom2 == null) {
            throw new IllegalArgumentException("Kan ikke avgjøre om to åpne periode overlapper");
        }
    }

    private static void validerFraOgMedDato(LocalDate fom1, LocalDate fom2) {
        if (isNull(fom1) || isNull(fom2)) {
            throw new IllegalArgumentException("Fom-dato kan ikke være null!");
        }
    }
}
