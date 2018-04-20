package no.nav.melosys.domain.util;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;

public class SaksopplysningerUtil {

    private SaksopplysningerUtil() {
        throw new IllegalStateException();
    }

    /**
     * Henter det første dokumentet som finnes for en gitt behandling og en gitt saksopplysningType.
     */
    public static Optional<SaksopplysningDokument> hentDokument(Behandling behandling, SaksopplysningType saksopplysningType) {
        if (behandling == null) {
            return Optional.empty();
        }
        return behandling.getSaksopplysninger().stream().filter(
                saksopplysning -> saksopplysning.getType().equals(saksopplysningType)).findFirst().map(Saksopplysning::getDokument);
    }
}
