package no.nav.melosys.service.dokument.brev;

import java.util.Set;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.person.Persondata;


public class BrevDataAvslagArbeidsgiver extends BrevData {
    public BrevDataAvslagArbeidsgiver(String saksbehandler) {
        this.saksbehandler = saksbehandler;
    }

    public Persondata person;
    public AvklartVirksomhet hovedvirksomhet;
    public Lovvalgsperiode lovvalgsperiode;
    public String arbeidsland;

    public Set<VilkaarBegrunnelse> vilkårbegrunnelser121;
    public Set<VilkaarBegrunnelse> vilkårbegrunnelser121VesentligVirksomhet;
}
