package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoVirksomhet;

public class FritekstbrevOffentligEtat extends Fritekstbrev {

    private FritekstbrevOffentligEtat(FritekstbrevBrevbestilling brevbestilling) {
        super(brevbestilling, Aktoersroller.OFFENTLIG_ETAT, SaksinfoVirksomhet.av(brevbestilling));
    }

    @Override
    public SaksinfoVirksomhet getSaksinfo() {
        return (SaksinfoVirksomhet) super.getSaksinfo();
    }

    public static FritekstbrevOffentligEtat av(FritekstbrevBrevbestilling brevbestilling) {
        return new FritekstbrevOffentligEtat(brevbestilling);
    }
}
