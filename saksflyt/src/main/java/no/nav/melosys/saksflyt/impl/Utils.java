package no.nav.melosys.saksflyt.impl;

import java.util.Comparator;
import java.util.function.Predicate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;

public abstract class Utils {

    // FIXME (farjam): Tester

    /**
     * Kvalifiserer kun behandlinger som har en gitt status
     */
    public static Predicate<Behandling> medStatus(BehandlingStatus status) {
        return (s) -> {
            assert status != null : "Status kan ikke være null";
            return s.getStatus().equals(status);
        };
    }

    /**
     * Sorterer behandlinger etter frist, kortest frist først.
     */
    public static Comparator<Behandling> eldsteFørst() {
        return (s1, s2) -> {
            assert s1 != null && s2 != null : "Behandling kan ikke være null";
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
