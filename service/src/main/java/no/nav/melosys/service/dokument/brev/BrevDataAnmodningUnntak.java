package no.nav.melosys.service.dokument.brev;

import java.util.Set;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;

public class BrevDataAnmodningUnntak extends BrevData {
    public String arbeidsland;
    public AvklartVirksomhet hovedvirksomhet;
    public Yrkesaktivitetstyper yrkesaktivitet;
    public Set<VilkaarBegrunnelse> anmodningBegrunnelser;
    public Set<VilkaarBegrunnelse> anmodningUtenArt12Begrunnelser;

    public BrevDataAnmodningUnntak(String saksbehandler) {
        super(saksbehandler);
    }
}
