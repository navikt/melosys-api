package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.sikkerhet.context.SubjectHandler;

public class BrevData {
    public String saksbehandler;

    public String fritekst;

    public String begrunnelseKode;

    public BrevData(BrevbestillingDto brevbestillingDto) {
        this(brevbestillingDto, SubjectHandler.getInstance().getUserID());
    }

    public BrevData(BrevbestillingDto brevbestillingDto, String saksbehandler) {
        this.saksbehandler = saksbehandler;
        this.fritekst = brevbestillingDto.getFritekst();
        this.begrunnelseKode = brevbestillingDto.getBegrunnelseKode();
    }

    public BrevData() { }

    // Brukes av saksflyt
    public BrevData(String saksbehandler) {
        this.saksbehandler = saksbehandler;
    }
}
