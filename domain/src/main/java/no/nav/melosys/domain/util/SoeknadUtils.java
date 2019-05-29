package no.nav.melosys.domain.util;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.dokument.soeknad.MaritimtArbeid;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;

/**
 * Metoder for å trekke ut opplysninger fra et {@code SoeknadDokument}.
 */
public final class SoeknadUtils {

    private SoeknadUtils() {
        throw new UnsupportedOperationException();
    }

    public static Periode hentPeriode(SoeknadDokument soeknadDokument) {
        return soeknadDokument.periode;
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

    /**
     * Returnerer søknadsland som landkoder,
     * og sjekker at det er minst et søknadsland oppgitt i søknad
     */
    public static List<Landkoder> hentSøknadslandkoder(SoeknadDokument søknad) {
        List<String> søknadsland = hentSøknadsland(søknad);
        if (søknadsland.isEmpty()) {
            throw new IllegalStateException("Søknad mangler søknadsland");
        }
        return søknadsland.stream()
            .map(Landkoder::valueOf)
            .collect(Collectors.toList());
    }

    public static List<String> hentSøknadsland(SoeknadDokument søknad) {
        return søknad.soeknadsland.landkoder;
    }

    public static Optional<Landkoder> hentOppgittBostedsland(SoeknadDokument søknad) {
        Objects.requireNonNull(søknad.bosted.oppgittAdresse, "Kan ikke hente bostedsland fra oppgitt adresse, landkode er null.");

        if (søknad.bosted.oppgittAdresse.landkode != null) {
            return Optional.of(Landkoder.valueOf(søknad.bosted.oppgittAdresse.landkode));
        }
        return Optional.empty();
    }
}