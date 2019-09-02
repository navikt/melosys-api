package no.nav.melosys.service.dokument.brev;

import java.util.Optional;

import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;

public class BrevDataAnmodningUnntakOgAvslag extends BrevData {
    public String arbeidsland;
    public AvklartVirksomhet hovedvirksomhet;
    public Optional<AnmodningsperiodeSvar> anmodningsperiodeSvar;

    public BrevDataAnmodningUnntakOgAvslag(String saksbehandler) {
        super(saksbehandler);
    }

}
