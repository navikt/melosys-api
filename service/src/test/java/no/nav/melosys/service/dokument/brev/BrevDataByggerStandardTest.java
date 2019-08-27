package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerStandard;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class BrevDataByggerStandardTest {

    private BrevDataByggerStandard brevDataByggerStandard;

    @Test
    public void lagBrevData() {
        BrevbestillingDto bestilling = new BrevbestillingDto();
        bestilling.mottaker = Aktoersroller.BRUKER;
        bestilling.fritekst = "FRITEKST";
        brevDataByggerStandard = new BrevDataByggerStandard(bestilling);

        String saksbehandler = "Z123456";
        BrevData brevData = brevDataByggerStandard.lag(saksbehandler);
        assertThat(brevData).isInstanceOf(BrevData.class);
        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
        assertThat(brevData.fritekst).isEqualTo(bestilling.fritekst);
        assertThat(brevData.begrunnelseKode).isEqualTo(bestilling.begrunnelseKode);
    }
}
