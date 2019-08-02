package no.nav.melosys.service.dokument.brev;

import java.util.Collection;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;

public class BrevDataAnmodningUnntakOgAvslag extends BrevData {
    public Collection<Anmodningsperiode> anmodningsperioder;
    public String arbeidsland;
    public AvklartVirksomhet hovedvirksomhet;

    public BrevDataAnmodningUnntakOgAvslag(String saksbehandler) {
        super(saksbehandler);
    }

}
