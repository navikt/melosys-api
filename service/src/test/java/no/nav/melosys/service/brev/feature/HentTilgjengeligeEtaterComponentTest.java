package no.nav.melosys.service.brev.feature;

import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.brev.Etat.*;
import static org.assertj.core.api.Assertions.assertThat;

class HentTilgjengeligeEtaterComponentTest {

    private final HentTilgjengeligeEtaterComponent hentTilgjengeligeEtaterComponent = new HentTilgjengeligeEtaterComponent();

    @Test
    void hentTilgjengeligeEtater_inneholderBareStøttedeEtater() {

        var tilgjengeligeEtater = hentTilgjengeligeEtaterComponent.hentTilgjengeligeEtater();


        assertThat(tilgjengeligeEtater).containsExactly(
            SKATTEETATEN_ORGNR, SKATTINNKREVER_UTLAND_ORGNR, HELFO_ORGNR
        );
    }
}
