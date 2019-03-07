package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;

public class BrevDataAnmodningUnntakOgAvslag extends BrevData {

    public AvklartVirksomhet hovedvirksomhet;

    public BrevDataAnmodningUnntakOgAvslag(String saksbehandler) {
        super(saksbehandler);
    }

}
