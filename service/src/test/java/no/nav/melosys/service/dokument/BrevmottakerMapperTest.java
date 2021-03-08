package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

class BrevmottakerMapperTest {

    private BrevmottakerMapper brevmottakerMapper = new BrevmottakerMapper(mock(BrevmottakerService.class), mock(BehandlingService.class));

    @Test
    void gittMalErRegistert_skalRetunereBrevmottaker() throws Exception {
        Mottakerliste mottakerliste = brevmottakerMapper.finnBrevMottaker(Produserbaredokumenter.MANGELBREV_BRUKER, 123);
        assertThat(mottakerliste).isNotNull();
        assertThat(mottakerliste.getHovedMottaker()).isEqualTo(BRUKER);
        assertThat(mottakerliste.getKopiMottakere()).isEmpty();
        assertThat(mottakerliste.getFasteMottakere()).isEmpty();
    }

    @Test
    void gittMalIkkeRegistret_skalKasteFeil() {
        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> brevmottakerMapper.finnBrevMottaker(Produserbaredokumenter.ATTEST_A1, 123));
    }
}