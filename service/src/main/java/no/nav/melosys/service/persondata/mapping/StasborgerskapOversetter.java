package no.nav.melosys.service.persondata.mapping;

import no.nav.melosys.domain.person.Statsborgerskap;

public final class StasborgerskapOversetter {
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
            statsborgerskapPDL.hentKilde(),
            statsborgerskapPDL.metadata().historisk()
        );
    }
}
