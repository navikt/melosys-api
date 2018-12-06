package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.Behandling;

public class BrevDataByggerStandard implements BrevDataBygger {

    private final BrevbestillingDto brevbestillingDto;

    public BrevDataByggerStandard(BrevbestillingDto brevbestillingDto) {
        this.brevbestillingDto = brevbestillingDto;
    }

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) {
        BrevData brevData = new BrevData(brevbestillingDto, saksbehandler);
        return brevData;
    }
}