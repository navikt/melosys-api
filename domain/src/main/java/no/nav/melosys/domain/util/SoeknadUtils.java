package no.nav.melosys.domain.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.dokument.soeknad.MaritimtArbeid;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.FunksjonellException;
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

    /**
     * Benytter kun første element til å vurdere maritimt arbeid i Leveranse 1
     */
    public static MaritimtArbeid hentMaritimtArbeid(SoeknadDokument søknad) throws FunksjonellException {
        if (søknad.maritimtArbeid.isEmpty()) {
            throw new FunksjonellException("Søknad mangler detaljer om Maritimt Arbeid");
        }
        return søknad.maritimtArbeid.get(0);
    }
}