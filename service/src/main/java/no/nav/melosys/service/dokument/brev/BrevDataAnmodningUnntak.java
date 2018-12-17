package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;

public class BrevDataAnmodningUnntak extends BrevData {

    public BrevDataAnmodningUnntak(String saksbehandler) {
        super(saksbehandler);
    }

    public Virksomhet hovedvirksomhet;
}
