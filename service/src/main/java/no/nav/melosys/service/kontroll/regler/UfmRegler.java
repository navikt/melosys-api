package no.nav.melosys.service.kontroll.regler;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import no.nav.melosys.domain.kodeverk.Landkoder;

public class UfmRegler {
    private static final String STATSLØS = "XS";
    private static final Set<Landkoder> NORDISK_ELLER_AVTALELAND = Set.of(
        Landkoder.SE,
        Landkoder.DK,
        Landkoder.FI,
        Landkoder.IS,
        Landkoder.AT,
        Landkoder.NL,
        Landkoder.LU
    );

    public static boolean statsborgerskapErMedlemsland(Collection<String> statsborgerskapLandkoder) {
        return !statsborgerskapLandkoder.isEmpty() && Arrays.stream(Landkoder.values())
            .anyMatch(landkode -> statsborgerskapLandkoder.contains(landkode.getKode()));
    }

    public static boolean avsenderErNordiskEllerAvtaleland(Landkoder avsenderLandkode) {
        return avsenderLandkode != null && NORDISK_ELLER_AVTALELAND.contains(avsenderLandkode);
    }

    public static boolean erStatsløs(Collection<String> statsborgerskapLandkoder) {
        return !statsborgerskapLandkoder.isEmpty() && statsborgerskapLandkoder.contains(STATSLØS);
    }

    public static boolean lovvalgslandErNorge(Landkoder landkode) {
        return landkode.equals(Landkoder.NO);
    }
}
