package no.nav.melosys.domain.util;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public final class LandkoderUtils {
    private static final BiMap<String, String> map = HashBiMap.create();

    static {
        Arrays.stream(Locale.getISOCountries())
            .forEach(c -> map.put(c, new Locale("", c).getISO3Country()));
    }

    private LandkoderUtils() {
        throw new IllegalArgumentException("Utility");
    }

    public static String tilIso3(String iso2Kode) {
        return map.get(iso2Kode);
    }

    public static List<String> tilIso3(List<String> iso2Koder) {
        return iso2Koder.stream().map(LandkoderUtils::tilIso3).collect(Collectors.toList());
    }

    public static String tilIso2(String iso3Kode) {
        return map.inverse().get(iso3Kode);
    }
}
