package no.nav.melosys.domain.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.melosys.domain.dokument.felles.Land;
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
        if (soeknad.arbeidUtland.arbeidsland != null) {
            soeknad.arbeidUtland.arbeidsland.stream().filter(Objects::nonNull).map(Land::getKode).forEach(landkoder::add);
        }
        if (soeknad.oppholdUtland.oppholdsland != null) {
            soeknad.oppholdUtland.oppholdsland.stream().filter(Objects::nonNull).map(Land::getKode).forEach(landkoder::add);
        }
        return landkoder;
    }

    /**
     * Henter arbeidsperioden hvis den finnes, oppholdsperioden ellers.
     * @throws RuntimeException når ingen periode finnes
     */
    public static Periode hentPeriode(SoeknadDokument soeknadDokument) {
        Optional<Periode> arbeidsperiode = Optional.ofNullable(soeknadDokument.arbeidUtland.arbeidsperiode);
        Optional<Periode> oppholdsPeriode = Optional.ofNullable(soeknadDokument.oppholdUtland.oppholdsPeriode);
        if (arbeidsperiode.isPresent()) {
            return arbeidsperiode.get();
        } else if (oppholdsPeriode.isPresent()) {
            return oppholdsPeriode.get();
        }
        throw new RuntimeException("Det finnes ikke noen arbeidsperiode eller oppholdsPeriode");
    }
}
