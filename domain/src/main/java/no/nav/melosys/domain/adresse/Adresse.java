package no.nav.melosys.domain.adresse;

import java.util.Objects;

import no.nav.melosys.domain.kodeverk.Landkoder;

public interface Adresse {
    String getLandkode();
    boolean erTom();

    default boolean erNorsk() {
        return Landkoder.NO.getKode().equals(getLandkode());
    }

    static String sammenslå(String s1, String s2) {
        return (Objects.toString(s1, "") + " " + Objects.toString(s2, "")).trim();
    }
}
