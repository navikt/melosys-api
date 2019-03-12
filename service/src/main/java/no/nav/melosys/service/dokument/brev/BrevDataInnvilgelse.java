package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartInnstallasjonsType;

public class BrevDataInnvilgelse extends BrevData {

    public BrevDataA1 vedleggA1;
    public Lovvalgsperiode lovvalgsperiode;
    public AvklartInnstallasjonsType avklartSokkelEllerSkip;

    public BrevDataInnvilgelse(String saksbehandler, BrevbestillingDto brevbestillingDto) {
        super(brevbestillingDto, saksbehandler);
    }
}
