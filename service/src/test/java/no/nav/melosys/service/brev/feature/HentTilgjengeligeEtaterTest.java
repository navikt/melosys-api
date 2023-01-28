package no.nav.melosys.service.brev.feature;

import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.brev.Etat.*;
import static org.assertj.core.api.Assertions.assertThat;

class HentTilgjengeligeEtaterTest {

    private final HentTilgjengeligeEtater hentTilgjengeligeEtater = new HentTilgjengeligeEtater();

    @Test
    void hentTilgjengeligeEtater_inneholderBareStøttedeEtater() {
        
        var tilgjengeligeEtater = hentTilgjengeligeEtater.hentTilgjengeligeEtater();


        assertThat(tilgjengeligeEtater).containsExactly(
            SKATTEETATEN_ORGNR, SKATTINNKREVER_UTLAND_ORGNR, HELFO_ORGNR
        );
    }
}
