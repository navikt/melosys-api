package no.nav.melosys.service.dokument.brev;

import java.util.List;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.Maritimtyper;

public class BrevDataInnvilgelse extends BrevData {

    public Lovvalgsperiode lovvalgsperiode;
    public String arbeidsland;
    public List<AvklartVirksomhet> norskeVirksomheter;
    public Maritimtyper avklartMaritimType;
    public String trygdemyndighetsland;
    public BrevDataA1 vedleggA1;

    public BrevDataInnvilgelse(String saksbehandler, BrevbestillingDto brevbestillingDto) {
        super(brevbestillingDto, saksbehandler);
    }
}