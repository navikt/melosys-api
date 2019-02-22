package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;

public class BrevDataAnmodningUnntakOgAvslag extends BrevData {

    public Virksomhet hovedvirksomhet;

    public BrevDataAnmodningUnntakOgAvslag(String saksbehandler) {
        super(saksbehandler);
    }

}
