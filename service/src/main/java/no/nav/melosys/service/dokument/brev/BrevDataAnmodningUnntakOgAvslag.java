package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.Landkoder;

public class BrevDataAnmodningUnntakOgAvslag extends BrevData {

    public AvklartVirksomhet hovedvirksomhet;
    public Landkoder arbeidsland;

    public BrevDataAnmodningUnntakOgAvslag(String saksbehandler) {
        super(saksbehandler);
    }

}
