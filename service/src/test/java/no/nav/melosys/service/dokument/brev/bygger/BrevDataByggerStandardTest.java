package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class BrevDataByggerStandardTest {

    @Test
    public void lagBrevData() {
        BrevbestillingRequest bestilling = new BrevbestillingRequest.Builder()
            .medMottaker(Aktoersroller.BRUKER)
            .medFritekst("FRITEKST")
            .build();
        BrevDataByggerStandard brevDataByggerStandard = new BrevDataByggerStandard(bestilling);

        String saksbehandler = "Z123456";
        BrevData brevData = brevDataByggerStandard.lag(null, saksbehandler);
        assertThat(brevData).isInstanceOf(BrevData.class);
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
        assertThat(brevData.fritekst).isEqualTo(bestilling.getFritekst());
        assertThat(brevData.begrunnelseKode).isEqualTo(bestilling.getBegrunnelseKode());
    }
}
