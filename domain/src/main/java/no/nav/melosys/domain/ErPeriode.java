package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.Objects;

import org.threeten.extra.LocalDateRange;

/**
 * Felles grensesnitt for perioder
 */
public interface ErPeriode {

    LocalDate getFom();

    LocalDate getTom();

    default boolean erGyldig() {
        return inkluderer(LocalDate.now());
    }

    default boolean inkluderer(LocalDate kandidat) {
        if (getFom() != null && kandidat.isBefore(getFom())) {
            return false;
        }
        return getTom() == null || !kandidat.isAfter(getTom());
    }

    // En løpende periode (tom = null) er åpen, ikke ugyldig, jf. inkluderer() over.
    // Fom er derimot påkrevd i alle perioder.
    default boolean overlapperMedÅr(int år) {
        var fom = Objects.requireNonNull(getFom(), () -> "fom er påkrevd for " + getClass().getSimpleName());
        var tom = getTom() != null ? getTom() : LocalDate.MAX;
        var localDateRangeForPeriode = LocalDateRange.ofClosed(fom, tom);
        var localDateRangeForÅr = LocalDateRange.ofClosed(LocalDate.of(år, 1, 1), LocalDate.of(år, 12, 31));
        return localDateRangeForPeriode.overlaps(localDateRangeForÅr);
    }
}
