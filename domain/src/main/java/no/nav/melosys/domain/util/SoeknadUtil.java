package no.nav.melosys.domain.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

/**
 * Metoder for å trekke ut opplysninger fra et {@code SoeknadDokument}.
 */
public class SoeknadUtil {

    private SoeknadUtil() {
        throw new IllegalStateException();
    }

    /**
     * Henter arbeidsland og oppholdsland samlet i en liste av landkoder.
     */
    public static List<String> hentLand(SoeknadDokument soeknad) {
        List<String> landkoder = new ArrayList<>();
        Optional<List<Land>> landListe = Optional.ofNullable(soeknad.arbeidUtland.arbeidsland);
        landListe.ifPresent(lands -> landkoder.addAll(soeknad.arbeidUtland.arbeidsland.stream().filter(Objects::nonNull).map(Land::getKode).collect(Collectors.toList())));
        landListe = Optional.ofNullable(soeknad.oppholdUtland.oppholdsland);
        landListe.ifPresent(lands -> landkoder.addAll(soeknad.oppholdUtland.oppholdsland.stream().filter(Objects::nonNull).map(Land::getKode).collect(Collectors.toList())));
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
