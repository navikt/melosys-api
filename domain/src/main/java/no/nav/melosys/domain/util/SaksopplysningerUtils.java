package no.nav.melosys.domain.util;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.TekniskException;

public final class SaksopplysningerUtils {

    private SaksopplysningerUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Henter det første dokumentet som finnes for en gitt behandling og en gitt saksopplysningType.
     */
    public static Optional<SaksopplysningDokument> hentDokument(Behandling behandling, SaksopplysningType saksopplysningType) {
        if (behandling == null) {
            return Optional.empty();
        }
        return behandling.getSaksopplysninger().stream()
            .filter(saksopplysning -> saksopplysning.getType().equals(saksopplysningType))
            .findFirst().map(Saksopplysning::getDokument);
    }

    public static PersonDokument hentPersonDokument(Behandling behandling) throws TekniskException {
        Optional<SaksopplysningDokument> saksopplysning = hentDokument(behandling, SaksopplysningType.PERSONOPPLYSNING);
        return (PersonDokument) saksopplysning
                .orElseThrow(() -> new TekniskException("Finner ikke persondokument"));
    }

    public static SoeknadDokument hentSøknadDokument(Behandling behandling) throws TekniskException {
        Optional<SaksopplysningDokument> saksopplysning = hentDokument(behandling, SaksopplysningType.SØKNAD);
        return (SoeknadDokument) saksopplysning
                .orElseThrow(() -> new TekniskException("Finner ikke søknaddokument"));
    }
}
