package no.nav.melosys.domain;

import java.time.LocalDate;

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

    default boolean overlapperMedÅr(int år) {
        var localDateRangeForPeriode = LocalDateRange.ofClosed(getFom(), getTom());
        var localDateRangeForÅr = LocalDateRange.ofClosed(LocalDate.of(år, 1, 1), LocalDate.of(år, 12, 31));
        return localDateRangeForPeriode.overlaps(localDateRangeForÅr);
    }
}
