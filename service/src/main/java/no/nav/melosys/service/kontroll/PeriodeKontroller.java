package no.nav.melosys.service.kontroll;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public final class PeriodeKontroller {

    private PeriodeKontroller() {
    }

    public static boolean feilIPeriode(LocalDate fom, LocalDate tom) {
        return fom == null || tom != null && fom.isAfter(tom);
    }

    public static boolean periodeErÅpen(LocalDate fom, LocalDate tom) {
        return fom != null && tom == null;
    }

    public static boolean periodeOver24Mnd(LocalDate fom, LocalDate tom) {
        return tom != null && ChronoUnit.MONTHS.between(fom, tom) >= 24;
    }

    public static boolean periodeOver5År(LocalDate fom, LocalDate tom) {
        return tom != null && ChronoUnit.YEARS.between(fom, tom) >= 5;
    }

    public static boolean datoEldreEnn3År(LocalDate dato) {
        return dato.isBefore(LocalDate.now().minusYears(3));
    }

    public static boolean datoEldreEnn2Mnd(Instant instant) {
        return tilLocalDate(instant).isBefore(LocalDate.now().minusMonths(2));
    }

    public static boolean datoOver1ÅrFremITid(LocalDate fom) {
        return fom.isAfter(LocalDate.now().plusYears(1));
    }

    public static boolean periodeErLik(LocalDate fom1, LocalDate tom1, LocalDate fom2, LocalDate tom2) {
        return datoErLik(fom1, fom2) && datoErLik(tom1, tom2);
    }

    private static boolean datoErLik(LocalDate date1, LocalDate date2) {

        if (date1 == null || date2 == null) {
            return date1 == date2;
        }

        return date1.equals(date2);
    }

    public static boolean periodeOverlapper(LocalDate fom1, LocalDate tom1, LocalDate fom2, LocalDate tom2) {

        if (fom1 == null || fom2 == null) {
            throw new IllegalArgumentException("Fom-dato kan ikke være null!");
        } else if (tom1 == null && tom2 == null) {
            throw new IllegalArgumentException("Kan ikke avgjøre om to åpne periode overlapper");
        }

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

    private static boolean åpenPeriodeOverlapper(LocalDate fom, LocalDate tom, LocalDate åpenPeriode) {
        return fom.isAfter(åpenPeriode) && tom.isAfter(åpenPeriode);
    }

    private static LocalDate tilLocalDate(Instant instant) {
        return LocalDate.ofInstant(instant, ZoneId.systemDefault());
    }
}
