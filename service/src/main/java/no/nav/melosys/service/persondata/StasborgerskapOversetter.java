package no.nav.melosys.service.persondata;

import java.util.Comparator;
import java.util.List;

import no.nav.melosys.domain.person.Statsborgerskap;
import no.nav.melosys.integrasjon.pdl.dto.Endring;

public class StasborgerskapOversetter {
    private StasborgerskapOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static no.nav.melosys.domain.person.Statsborgerskap oversett(
        no.nav.melosys.integrasjon.pdl.dto.person.Statsborgerskap statsborgerskapPDL
    ) {
        return new Statsborgerskap(
            statsborgerskapPDL.land(),
            statsborgerskapPDL.bekreftelsesdato(),
            statsborgerskapPDL.gyldigFraOgMed(),
            statsborgerskapPDL.gyldigTilOgMed(),
            statsborgerskapPDL.metadata().master(),
            hentKilde(statsborgerskapPDL.metadata().endringer()),
            statsborgerskapPDL.metadata().historisk()
        );
    }

    private static String hentKilde(List<Endring> endringer) {
        return endringer.stream().max(Comparator.comparing(Endring::registrert)).map(Endring::kilde)
            .orElseThrow(() -> new IllegalArgumentException("Kilde er obligatorisk i PDL-endringer."));
    }
}
