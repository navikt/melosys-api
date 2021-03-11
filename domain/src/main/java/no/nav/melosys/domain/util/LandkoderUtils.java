package no.nav.melosys.domain.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public final class LandkoderUtils {
    private static final BiMap<String, String> ISO2_ISO3 = HashBiMap.create();
    private static final BiMap<String, String> ISO2_LANDNAVN = HashBiMap.create();

    static {
        Arrays.stream(Locale.getISOCountries())
            .forEach(c -> ISO2_ISO3.put(c, new Locale("", c).getISO3Country()));
        Arrays.stream(Locale.getISOCountries())
            .forEach(
                c -> ISO2_LANDNAVN.put(c, new Locale("no", c, "nb").getDisplayCountry().toUpperCase()));
    }

    private LandkoderUtils() {
        throw new IllegalStateException("Utility");
    }

    public static String tilIso3(String iso2Kode) {
        return ISO2_ISO3.get(iso2Kode);
    }

    public static Collection<String> tilIso3(Collection<String> iso2Koder) {
        return iso2Koder.stream().map(LandkoderUtils::tilIso3).collect(Collectors.toList());
    }

    public static String tilIso2(String iso3Kode) {
        return ISO2_ISO3.inverse().get(iso3Kode);
    }

    public static String tilIso2FraLandnavn(String landnavn) {
        if (landnavn == null) {
            throw new IllegalArgumentException("Landnavn kreves");
        }
        return ISO2_LANDNAVN.inverse().get(landnavn.trim().toUpperCase());
    }
}
