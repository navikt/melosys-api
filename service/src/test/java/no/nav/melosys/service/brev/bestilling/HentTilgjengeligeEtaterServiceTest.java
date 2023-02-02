package no.nav.melosys.service.brev.bestilling;

import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.brev.Etat.*;
import static org.assertj.core.api.Assertions.assertThat;

class HentTilgjengeligeEtaterServiceTest {

    private final HentTilgjengeligeEtaterService hentTilgjengeligeEtaterService = new HentTilgjengeligeEtaterService();

    @Test
    void hentTilgjengeligeEtater_inneholderBareStøttedeEtater() {

        var tilgjengeligeEtater = hentTilgjengeligeEtaterService.hentTilgjengeligeEtater();


        assertThat(tilgjengeligeEtater).containsExactly(
            SKATTEETATEN_ORGNR, SKATTINNKREVER_UTLAND_ORGNR, HELFO_ORGNR
        );
    }
}
