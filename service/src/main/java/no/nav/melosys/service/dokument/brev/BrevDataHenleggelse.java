package no.nav.melosys.service.dokument.brev;

import java.time.Instant;

public class BrevDataHenleggelse extends BrevData {

    public BrevDataHenleggelse(String saksbehandler, BrevbestillingDto brevbestillingDto) {
        super(brevbestillingDto, saksbehandler);
    }

    public Instant initierendeJournalpostForsendelseMottattTidspunkt;
}
