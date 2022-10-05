package no.nav.melosys.domain;

import java.time.LocalDate;

/**
 * Felles grensesnitt for perioder
 */
public interface ErPeriode {

    LocalDate getFom();

    LocalDate getTom();

    default boolean erGyldig() {
        return inkluderer(LocalDate.now());
    }

    default boolean erTom() {
        return getFom() == null && getTom() == null;
    }

    default boolean inkluderer (LocalDate kandidat) {
        if (getFom() != null && kandidat.isBefore(getFom())) {
            return false;
        }
        return getTom() == null || !kandidat.isAfter(getTom());
    }

}
