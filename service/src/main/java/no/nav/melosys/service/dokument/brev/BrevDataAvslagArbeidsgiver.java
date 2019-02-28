package no.nav.melosys.service.dokument.brev;

import java.util.Set;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.service.dokument.felles.AvklartVirksomhet;


public class BrevDataAvslagArbeidsgiver extends BrevData {
    public BrevDataAvslagArbeidsgiver(String saksbehandler) {
        this.saksbehandler = saksbehandler;
    }

    public PersonDokument person;
    public AvklartVirksomhet hovedvirksomhet;
    public Lovvalgsperiode lovvalgsperiode;

    public Set<VilkaarBegrunnelse> vilkårbegrunnelser121;
    public Set<VilkaarBegrunnelse> vilkårbegrunnelser121VesentligVirksomhet;
}
