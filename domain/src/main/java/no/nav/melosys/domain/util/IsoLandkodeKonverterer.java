package no.nav.melosys.domain.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import no.nav.melosys.domain.kodeverk.Landkoder;

public final class IsoLandkodeKonverterer {
    private static final BiMap<String, String> ISO2_ISO3 = HashBiMap.create();
    private static final BiMap<String, String> ISO2_TIL_EU_EOS_LANDNAVN = HashBiMap.create();

    static {
        Arrays.stream(Locale.getISOCountries())
            .forEach(c -> {
                ISO2_ISO3.put(c, new Locale("", c).getISO3Country());
            });
        ISO2_ISO3.put("XU", "XUK");
        ISO2_ISO3.put("XK", "XXK");
        Arrays.stream(Landkoder.values())
            .forEach(c -> {
                ISO2_TIL_EU_EOS_LANDNAVN.put(c.getKode(), c.getBeskrivelse().toUpperCase());
            });
        ISO2_TIL_EU_EOS_LANDNAVN.put("XUk", "XU");
    }

    private IsoLandkodeKonverterer() {
        throw new IllegalStateException("Utility");
    }

    public static String tilIso3(String iso2Kode) {
        return ISO2_ISO3.get(iso2Kode);
    }

    public static Collection<String> tilIso3(Collection<String> iso2Koder) {
        return iso2Koder.stream().map(IsoLandkodeKonverterer::tilIso3).collect(Collectors.toList());
    }

    public static String tilIso2(String iso3Kode) {
        return ISO2_ISO3.inverse().get(iso3Kode);
    }

    public static String tilIso2FraEuEosLandnavn(String landnavn) {
        if (landnavn == null) {
            throw new IllegalArgumentException("Landnavn kreves");
        }
        return ISO2_TIL_EU_EOS_LANDNAVN.inverse().get(landnavn.trim().toUpperCase());
    }
}
