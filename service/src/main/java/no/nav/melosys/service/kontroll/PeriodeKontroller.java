package no.nav.melosys.service.kontroll;

import java.time.LocalDate;
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
        return tom != null && ChronoUnit.MONTHS.between(fom, tom) >= 24L;
    }

    public static boolean periodeOver5År(LocalDate fom, LocalDate tom) {
        return tom != null && ChronoUnit.YEARS.between(fom, tom) >= 5L;
    }

    public static boolean datoEldreEnn5År(LocalDate fom) {
        return fom.isBefore(LocalDate.now().minusYears(5L));
    }

    public static boolean datoOver1ÅrFremITid(LocalDate fom) {
        return fom.isAfter(LocalDate.now().plusYears(1L));
    }

    public static boolean periodeErLik(LocalDate fom1, LocalDate tom1, LocalDate fom2, LocalDate tom2) {
        return datoErLik(fom1, fom2) && datoErLik(tom1, tom2);
    }

    private static boolean datoErLik(LocalDate date1, LocalDate date2) {

        if (date1 == null || date2 == null) {
            return date1 == null && date2 == null;
        }

        return date1.equals(date2);
    }
}
