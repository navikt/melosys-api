package no.nav.melosys.saksflyt.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.function.Predicate;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;

public abstract class Utils {

    /**
     * Kvalifiserer kun prosessinstanser som har en gitt steg
     */
    public static Predicate<Prosessinstans> medSteg(ProsessSteg steg) {
        return (pi) -> pi.getSteg() == steg;
    }
    
    /**
     * Kvalifiserer prosessinstanser som ikke sover
     */
    public static final Predicate<Prosessinstans> somIkkeSover = pi -> pi.getSoverTil() == null || pi.getSoverTil().isBefore(LocalDateTime.now());
    
    /**
     * Sorterer prosessinstanser etter alder, eldste først
     */
    public static Comparator<Prosessinstans> eldsteFørst() {
        return (pi1, pi2) -> {
            if (pi1.getRegistrertDato() == null && pi2.getRegistrertDato() == null) {
                return 0;
            }
            if (pi1.getRegistrertDato() == null) {
                return -1;
            }
            if (pi2.getRegistrertDato() == null) {
                return 1;
            }
            return pi1.getRegistrertDato().compareTo(pi2.getRegistrertDato());
        };
    }

}
