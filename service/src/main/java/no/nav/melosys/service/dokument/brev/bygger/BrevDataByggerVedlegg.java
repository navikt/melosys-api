package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.service.dokument.brev.*;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;

public class BrevDataByggerVedlegg implements BrevDataBygger {
    private BrevbestillingDto brevbestillingDto;

    private BrevDataByggerA1 a1Bygger;
    private BrevDataByggerA001 a001Bygger;

    public BrevDataByggerVedlegg(BrevDataByggerA1 a1Bygger, BrevbestillingDto brevbestillingDto) {
        this.a1Bygger = a1Bygger;
        this.a001Bygger = null;
        this.brevbestillingDto = brevbestillingDto;
    }

    public BrevDataByggerVedlegg(BrevDataByggerA001 a001Bygger, BrevbestillingDto brevbestillingDto) {
        this.a1Bygger = null;
        this.a001Bygger = a001Bygger;
        this.brevbestillingDto = brevbestillingDto;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        BrevDataVedlegg brevData = new BrevDataVedlegg(saksbehandler);

        if (a1Bygger != null) {
            brevData.setBrevDataA1((BrevDataA1) a1Bygger.lag(dataGrunnlag, saksbehandler));
        }
        if (a001Bygger != null) {
            brevData.setBrevDataA001((BrevDataA001) a001Bygger.lag(dataGrunnlag, saksbehandler));
        }

        if (brevbestillingDto != null) {
            brevData.setFritekst(brevbestillingDto.getFritekst());
            brevData.setBegrunnelseKode(brevbestillingDto.getBegrunnelseKode());
        }

        return brevData;
    }
}
