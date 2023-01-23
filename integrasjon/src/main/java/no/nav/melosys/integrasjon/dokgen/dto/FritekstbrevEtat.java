package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoVirksomhet;

public class FritekstbrevEtat extends Fritekstbrev {

    private FritekstbrevEtat(FritekstbrevBrevbestilling brevbestilling) {
        super(brevbestilling, Aktoersroller.ETAT, SaksinfoVirksomhet.av(brevbestilling));
    }

    @Override
    public SaksinfoVirksomhet getSaksinfo() {
        return (SaksinfoVirksomhet) super.getSaksinfo();
    }

    public static FritekstbrevEtat av(FritekstbrevBrevbestilling brevbestilling) {
        return new FritekstbrevEtat(brevbestilling);
    }
}
