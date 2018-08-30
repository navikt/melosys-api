package no.nav.melosys.domain.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

/**
 * Metoder for å trekke ut opplysninger fra et {@code SoeknadDokument}.
 */
public final class SoeknadUtils {

    private SoeknadUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Henter arbeidsland og oppholdsland samlet i en liste av landkoder.
     */
    public static List<String> hentLand(SoeknadDokument soeknad) {
        List<String> landkoder = new ArrayList<>();
        if (soeknad.arbeidUtland != null) {
            landkoder.add(soeknad.arbeidUtland.adresse.landKode);
        }
        if (soeknad.oppholdUtland != null) {
            soeknad.oppholdUtland.oppholdslandKoder.stream().filter(Objects::nonNull).forEach(landkoder::add);
        }
        return landkoder;
    }

    public static Periode hentPeriode(SoeknadDokument soeknadDokument) {
        Optional<Periode> oppholdsPeriode = Optional.ofNullable(soeknadDokument.oppholdUtland.oppholdsPeriode);
        if (oppholdsPeriode.isPresent()) {
            return oppholdsPeriode.get();
        }
        throw new RuntimeException("Det finnes ikke noen arbeidsperiode eller oppholdsPeriode");
    }
}
