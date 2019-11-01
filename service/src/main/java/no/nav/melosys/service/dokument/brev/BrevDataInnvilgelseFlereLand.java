package no.nav.melosys.service.dokument.brev;

import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.Maritimtyper;

public class BrevDataInnvilgelseFlereLand extends BrevData {
    public Lovvalgsperiode lovvalgsperiode;
    public List<String> alleArbeidsland;
    public Maritimtyper avklartMaritimType;
    public Collection<AvklartVirksomhet> arbeidsgivere;
    public String bostedsland;
    public boolean erBegrensetPeriode;
    public boolean erMarginaltArbeid;

    public BrevDataA1 vedleggA1;

    public BrevDataInnvilgelseFlereLand(BrevbestillingDto brevbestillingDto, String saksbehandler) {
        super(brevbestillingDto, saksbehandler);
    }
}