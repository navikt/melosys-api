package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.*;

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
    public BrevData lag(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataVedlegg brevData = new BrevDataVedlegg(saksbehandler);

        if (a1Bygger != null) {
            brevData.brevDataA1 = (BrevDataA1) a1Bygger.lag(behandling, saksbehandler);
        }
        if (a001Bygger != null) {
            brevData.brevDataA001 = (BrevDataA001) a001Bygger.lag(behandling, saksbehandler);
        }

        if (brevbestillingDto != null) {
            brevData.fritekst = brevbestillingDto.fritekst;
            brevData.begrunnelseKode = brevbestillingDto.begrunnelseKode;
        }

        return brevData;
    }
}
