package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.Maritimtyper;

public class BrevDataInnvilgelse extends BrevData {

    public Lovvalgsperiode lovvalgsperiode;
    public String arbeidsland;
    public AvklartVirksomhet hovedvirksomhet;
    public Maritimtyper avklartMaritimType;
    public String trygdemyndighetsland;
    public BrevDataA1 vedleggA1;

    public BrevDataInnvilgelse(BrevbestillingDto brevbestillingDto, String saksbehandler) {
        super(brevbestillingDto, saksbehandler);
    }
}