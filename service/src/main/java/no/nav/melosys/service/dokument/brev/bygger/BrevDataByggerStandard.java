package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;

public class BrevDataByggerStandard implements BrevDataBygger {

    private final BrevbestillingRequest brevbestillingRequest;

    public BrevDataByggerStandard(BrevbestillingRequest brevbestillingRequest) {
        this.brevbestillingRequest = brevbestillingRequest;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        return new BrevData(brevbestillingRequest, saksbehandler);
    }
}
