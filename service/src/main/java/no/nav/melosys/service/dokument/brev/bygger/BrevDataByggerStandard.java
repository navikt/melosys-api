package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;

public class BrevDataByggerStandard implements BrevDataBygger {

    private final BrevbestillingDto brevbestillingDto;

    public BrevDataByggerStandard(BrevbestillingDto brevbestillingDto) {
        this.brevbestillingDto = brevbestillingDto;
    }

    @Override
    public BrevData lag(String saksbehandler) {
        return new BrevData(brevbestillingDto, saksbehandler);
    }
}