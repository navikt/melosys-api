package no.nav.melosys.domain.adresse;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.kodeverk.Landkoder;

public interface Adresse {
    String getLandkode();
    boolean erTom();

    default boolean erNorsk() {
        return Landkoder.NO.getKode().equals(getLandkode());
    }

    static String sammenslå(String... strings) {
        return Arrays.stream(strings)
            .reduce("", (res, s) -> res + " " + Objects.toString(s, "")).trim();
    }

    List<String> toList();
}
