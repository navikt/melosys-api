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
        return tom != null && ChronoUnit.MONTHS.between(fom, tom) >= 23L;
    }

    public static boolean datoEldreEnn5År(LocalDate fom) {
        return fom.isBefore(LocalDate.now().minusYears(5L));
    }

    public static boolean datoOver1ÅrFremITid(LocalDate fom) {
        return fom.isAfter(LocalDate.now().plusYears(1L));
    }
}
