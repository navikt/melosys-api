package no.nav.melosys.service.dokument.brev;

import java.time.Instant;

public class BrevDataMottattDato extends BrevData {

    public BrevDataMottattDato(String saksbehandler, BrevbestillingRequest brevbestillingRequest) {
        super(brevbestillingRequest, saksbehandler);
    }

    public Instant initierendeJournalpostForsendelseMottattTidspunkt;
}
