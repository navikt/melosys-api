package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.ressurser.Dokumentressurser;

public class BrevDataByggerStandard implements BrevDataBygger {

    private final BrevbestillingDto brevbestillingDto;

    public BrevDataByggerStandard(BrevbestillingDto brevbestillingDto) {
        this.brevbestillingDto = brevbestillingDto;
    }

    @Override
    public BrevData lag(Dokumentressurser dokumentressurser, String saksbehandler) {
        return new BrevData(brevbestillingDto, saksbehandler);
    }
}