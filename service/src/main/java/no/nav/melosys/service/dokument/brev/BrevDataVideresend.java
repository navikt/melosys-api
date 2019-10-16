package no.nav.melosys.service.dokument.brev;

public class BrevDataVideresend extends BrevData {
    public String bostedsland;
    public String trygdemyndighetsland;

    public BrevDataVideresend(BrevbestillingDto brevbestillingDto, String saksbehandler) {
        super(brevbestillingDto, saksbehandler);
    }
}
