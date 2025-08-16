package no.nav.melosys.service.brev.bestilling;

import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.brev.NorskMyndighet.*;
import static org.assertj.core.api.Assertions.assertThat;

class HentTilgjengeligeNorskeMyndigheterServiceTest {

    private final HentTilgjengeligeNorskeMyndigheterService hentTilgjengeligeNorskeMyndigheterService = new HentTilgjengeligeNorskeMyndigheterService();

    @Test
    void hentTilgjengeligeNorskeMyndigheter_inneholderBareStøttedeNorskeMyndigheter() {

        var tilgjengeligeNorskeMyndigheter = hentTilgjengeligeNorskeMyndigheterService.hentTilgjengeligeNorskeMyndigheter();


        assertThat(tilgjengeligeNorskeMyndigheter).containsExactly(
            SKATTEETATEN, SKATTEINNKREVER_UTLAND, HELFO
        );
    }
}
