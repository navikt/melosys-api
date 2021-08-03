package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.service.dokument.brev.*;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;

public class BrevDataByggerVedlegg implements BrevDataBygger {
    private BrevbestillingRequest brevbestillingRequest;

    private BrevDataByggerA1 a1Bygger;
    private BrevDataByggerA001 a001Bygger;

    public BrevDataByggerVedlegg(BrevDataByggerA1 a1Bygger, BrevbestillingRequest brevbestillingRequest) {
        this.a1Bygger = a1Bygger;
        this.a001Bygger = null;
        this.brevbestillingRequest = brevbestillingRequest;
    }

    public BrevDataByggerVedlegg(BrevDataByggerA001 a001Bygger, BrevbestillingRequest brevbestillingRequest) {
        this.a1Bygger = null;
        this.a001Bygger = a001Bygger;
        this.brevbestillingRequest = brevbestillingRequest;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        BrevDataVedlegg brevData = new BrevDataVedlegg(saksbehandler);

        if (a1Bygger != null) {
            brevData.brevDataA1 = (BrevDataA1) a1Bygger.lag(dataGrunnlag, saksbehandler);
        }
        if (a001Bygger != null) {
            brevData.brevDataA001 = (BrevDataA001) a001Bygger.lag(dataGrunnlag, saksbehandler);
        }

        if (brevbestillingRequest != null) {
            brevData.fritekst = brevbestillingRequest.getFritekst();
            brevData.begrunnelseKode = brevbestillingRequest.getBegrunnelseKode();
        }

        return brevData;
    }
}
