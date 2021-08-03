package no.nav.melosys.service.dokument.brev;

import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.Landkoder;

public final class BrevDataInnvilgelseFlereLand extends BrevData {
    public Collection<AvklartVirksomhet> arbeidsgivere;
    public List<String> alleArbeidsland;
    public Lovvalgsperiode lovvalgsperiode;
    public boolean harAvklartMaritimTypeSokkel;
    public boolean harAvklartMaritimTypeSkip;
    public String bostedsland;
    public boolean erBegrensetPeriode;
    public boolean erMarginaltArbeid;
    public Landkoder trydemyndighetsland;

    public BrevDataA1 vedleggA1;

    public BrevDataInnvilgelseFlereLand(BrevbestillingRequest brevbestillingRequest, String saksbehandler) {
        super(brevbestillingRequest, saksbehandler);
    }
}
