package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class BrevDataByggerStandardTest {

    @Test
    void lagBrevData() {
        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setMottaker(Mottakerroller.BRUKER);
        brevbestillingDto.setFritekst("FRITEKST");

        BrevDataByggerStandard brevDataByggerStandard = new BrevDataByggerStandard(brevbestillingDto);

        String saksbehandler = "Z123456";
        BrevData brevData = brevDataByggerStandard.lag(null, saksbehandler);
        assertThat(brevData).isInstanceOf(BrevData.class);
        assertThat(brevData.getSaksbehandler()).isEqualTo(saksbehandler);
        assertThat(brevData.getFritekst()).isEqualTo(brevbestillingDto.getFritekst());
        assertThat(brevData.getBegrunnelseKode()).isEqualTo(brevbestillingDto.getBegrunnelseKode());
    }
}
