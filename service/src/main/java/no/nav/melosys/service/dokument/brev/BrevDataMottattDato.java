package no.nav.melosys.service.dokument.brev;

import java.time.Instant;

public class BrevDataMottattDato extends BrevData {

    public BrevDataMottattDato(String saksbehandler, BrevbestillingDto brevbestillingDto) {
        super(brevbestillingDto, saksbehandler);
    }

    public Instant initierendeJournalpostForsendelseMottattTidspunkt;
}
