package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.Utpekingsperiode;

public class BrevDataUtpekingAnnetLand extends BrevData {
    public Utpekingsperiode utpekingsperiode;

    public BrevDataUtpekingAnnetLand(BrevbestillingDto brevbestillingDto, String saksbehandler) {
        super(brevbestillingDto, saksbehandler);
    }
}
