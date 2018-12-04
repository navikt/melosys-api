package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.RolleType;
import no.nav.melosys.sikkerhet.context.SubjectHandler;

public class BrevData {
    public String saksbehandler;

    public RolleType mottaker;

    public String fritekst;

    public BrevData(BrevbestillingDto brevbestillingDto) {
        saksbehandler = SubjectHandler.getInstance().getUserID();
        this.mottaker = brevbestillingDto.mottaker;
        this.fritekst = brevbestillingDto.fritekst;
    }

    public BrevData() {
        saksbehandler = SubjectHandler.getInstance().getUserID();
    }

    // Brukes av saksflyt
    public BrevData(String saksbehandler) {
        this.saksbehandler = saksbehandler;
    }
}
