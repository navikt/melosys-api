package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.service.dokument.felles.AvklartVirksomhet;

public class BrevDataAnmodningUnntakOgAvslag extends BrevData {

    public AvklartVirksomhet hovedvirksomhet;

    public BrevDataAnmodningUnntakOgAvslag(String saksbehandler) {
        super(saksbehandler);
    }

}
