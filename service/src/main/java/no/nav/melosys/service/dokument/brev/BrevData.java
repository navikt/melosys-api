package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.RolleType;
import no.nav.melosys.sikkerhet.context.SubjectHandler;

public class BrevData {
    public String saksbehandler;

    public RolleType mottaker;

    public String fritekst;

    public BrevData(BrevbestillingDto brevbestillingDto) {
        this(brevbestillingDto, SubjectHandler.getInstance().getUserID());
    }

    public BrevData(BrevbestillingDto brevbestillingDto, String saksbehandler) {
        this.saksbehandler = saksbehandler;
        this.mottaker = brevbestillingDto.mottaker;
        this.fritekst = brevbestillingDto.fritekst;
    }

    // Kun for deserialisering
    public BrevData() { }

    // Brukes av saksflyt
    public BrevData(String saksbehandler) {
        this.saksbehandler = saksbehandler;
    }
}
