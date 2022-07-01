package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoVirksomhet;

public class FritekstbrevVirksomhet extends Fritekstbrev {

    private FritekstbrevVirksomhet(FritekstbrevBrevbestilling brevbestilling, Aktoersroller mottakerType) {
        super(brevbestilling, mottakerType, SaksinfoVirksomhet.av(brevbestilling));
    }

    @Override
    public SaksinfoVirksomhet getSaksinfo() {
        return (SaksinfoVirksomhet) super.getSaksinfo();
    }

    public static FritekstbrevVirksomhet av(FritekstbrevBrevbestilling brevbestilling, Aktoersroller mottakerType) {
        return new FritekstbrevVirksomhet(brevbestilling, mottakerType);
    }
}
