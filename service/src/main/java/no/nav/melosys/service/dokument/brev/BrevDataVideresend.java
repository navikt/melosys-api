package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.UtenlandskMyndighet;

public class BrevDataVideresend extends BrevData {
    public String bostedsland;
    public UtenlandskMyndighet trygdemyndighet;

    public BrevDataVideresend(BrevbestillingDto brevbestillingDto, String saksbehandler) {
        super(brevbestillingDto, saksbehandler);
    }
}
