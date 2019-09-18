package no.nav.melosys.domain.util;

import java.util.Objects;

public final class AdresseUtils {
    private AdresseUtils() { throw new IllegalStateException("Utility"); }

    public static String sammenslå(String s1, String s2) {
        return (Objects.toString(s1, "") + " " + Objects.toString(s2, "")).trim();
    }
}
