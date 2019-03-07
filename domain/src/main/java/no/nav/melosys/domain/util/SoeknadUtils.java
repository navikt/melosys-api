package no.nav.melosys.domain.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.TekniskException;

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
            soeknad.arbeidUtland.stream().filter(Objects::nonNull).forEach(arbeidUtland -> landkoder.add(arbeidUtland.adresse.landKode));
        }
        if (soeknad.oppholdUtland != null) {
            soeknad.oppholdUtland.oppholdslandKoder.stream().filter(Objects::nonNull).forEach(landkoder::add);
        }
        return landkoder;
    }

    public static Periode hentPeriode(SoeknadDokument soeknadDokument) {
        return soeknadDokument.oppholdUtland.oppholdsPeriode;
    }

    public static String hentArbeidslandFraSøknaden(Behandling behandling) {
        try {
            SoeknadDokument soeknadDokument = SaksopplysningerUtils.hentSøknadDokument(behandling);
            ArbeidUtland arbeidUtland = soeknadDokument.arbeidUtland.stream().findFirst().orElseThrow(() -> new IllegalArgumentException("arbeidUtland mangler"));
            return arbeidUtland.adresse.landKode;
        } catch (TekniskException e) {
            throw new IllegalStateException(e);
        }
    }
}
