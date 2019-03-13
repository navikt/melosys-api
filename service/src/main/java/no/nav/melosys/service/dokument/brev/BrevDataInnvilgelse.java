package no.nav.melosys.service.dokument.brev;

import java.util.List;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartInnstallasjonsType;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;

public class BrevDataInnvilgelse extends BrevData {

    public BrevDataA1 vedleggA1;
    public Lovvalgsperiode lovvalgsperiode;
    public AvklartInnstallasjonsType avklartSokkelEllerSkip;
    public List<AvklartVirksomhet> norskeVirksomheter;

    public BrevDataInnvilgelse(String saksbehandler, BrevbestillingDto brevbestillingDto) {
        super(brevbestillingDto, saksbehandler);
    }
}
