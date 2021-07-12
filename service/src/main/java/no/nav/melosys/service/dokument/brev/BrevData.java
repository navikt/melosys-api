package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.sikkerhet.context.SubjectHandler;

public class BrevData {
    public String saksbehandler;

    public String fritekst;

    public String begrunnelseKode;

    public BrevData(BrevbestillingRequest brevbestillingRequest) {
        this(brevbestillingRequest, SubjectHandler.getInstance().getUserID());
    }

    public BrevData(BrevbestillingRequest brevbestillingRequest, String saksbehandler) {
        this.saksbehandler = saksbehandler;
        this.fritekst = brevbestillingRequest.getFritekst();
        this.begrunnelseKode = brevbestillingRequest.getBegrunnelseKode();
    }

    public BrevData() { }

    // Brukes av saksflyt
    public BrevData(String saksbehandler) {
        this.saksbehandler = saksbehandler;
    }
}
