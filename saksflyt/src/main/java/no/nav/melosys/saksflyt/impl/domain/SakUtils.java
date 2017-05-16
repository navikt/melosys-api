package no.nav.melosys.saksflyt.impl.domain;

import java.util.Comparator;
import java.util.function.Predicate;

import no.nav.melosys.saksflyt.api.Sak;
import no.nav.melosys.saksflyt.api.Status;

public abstract class SakUtils {
    
    // FIXME (farjam): Tester
    
    /**
     * Kvalifiserer kun saker som har en gitt status
     */
    public static Predicate<Sak> sakMedStatus(Status status) {
        return (s) -> {return s.getStatus() == status;};
    }
    
    /**
     * Sorterer saker etter frist, kortest frist først.
     */
    public static Comparator<Sak> kortestFristFørst() {
        return (s1, s2) -> {
            assert s1 != null && s2 != null : "Sak kan ikke være null";
            if (s1.getFristDato() == null && s2.getFristDato() == null) {
                return 0;
            }
            if (s1.getFristDato() == null) {
                return -1;
            }
            if (s2.getFristDato() == null) {
                return 1;
            }
            return s1.getFristDato().compareTo(s2.getFristDato());
        };
    }

}
