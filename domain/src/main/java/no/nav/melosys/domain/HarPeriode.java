package no.nav.melosys.domain;

import java.time.LocalDate;

/**
 * Felles grensesnitt for perioder
 */
public interface HarPeriode {

    public LocalDate getFom();

    public LocalDate getTom();
    
    public default boolean inkluderer (LocalDate kandidat) {
        if (getFom() != null && kandidat.isBefore(getFom())) {
            return false;
        }
        if (getTom() != null && kandidat.isAfter(getTom())) {
            return false;
        }
        return true;
    }

}
