package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.integrasjon.dokgen.dto.BrevMottaker;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class BrevmottakerMapperTest {

    private BrevmottakerMapper brevmottakerMapper = new BrevmottakerMapper();

    @Test
    void gittMalErRegistert_skalRetunereBrevmottaker() {
        BrevMottaker brevMottaker = brevmottakerMapper.finnBrevMottaker(Produserbaredokumenter.MANGELBREV_BRUKER);
        assertThat(brevMottaker).isNotNull();
        assertThat(brevMottaker.getHovedMottaker()).isEqualTo(BRUKER);
        assertThat(brevMottaker.getKopiMottakere()).isEmpty();
        assertThat(brevMottaker.getFasteMottakere()).isEmpty();
    }

    @Test
    void gittMalIkkeRegistret_skalKasteFeil() {
        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> brevmottakerMapper.finnBrevMottaker(Produserbaredokumenter.ATTEST_A1));
    }
}