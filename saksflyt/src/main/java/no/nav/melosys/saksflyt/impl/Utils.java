package no.nav.melosys.saksflyt.impl;

import java.util.Comparator;
import java.util.function.Predicate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingSteg;

public abstract class Utils {

    /**
     * Kvalifiserer kun behandlinger som har en gitt status
     */
    public static Predicate<Behandling> medSteg(BehandlingSteg steg) {
        return (s) -> {
            return s.getSteg() == steg;
        };
    }

    /**
     * Sorterer behandlinger etter alder, eldste først
     */
    public static Comparator<Behandling> eldsteFørst() {
        return (s1, s2) -> {
            if (s1.getRegistrertDato() == null && s2.getRegistrertDato() == null) {
                return 0;
            }
            if (s1.getRegistrertDato() == null) {
                return -1;
            }
            if (s2.getRegistrertDato() == null) {
                return 1;
            }
            return s1.getRegistrertDato().compareTo(s2.getRegistrertDato());
        };
    }

}
