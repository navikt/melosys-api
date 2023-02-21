package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker;

public class FritekstbrevTrygdemyndighet extends Fritekstbrev {
    private FritekstbrevTrygdemyndighet(FritekstbrevBrevbestilling brevbestilling, Mottakerroller mottakerType) {
        super(brevbestilling, mottakerType, SaksinfoBruker.av(brevbestilling));
    }

    @Override
    public SaksinfoBruker getSaksinfo() {
        return (SaksinfoBruker) super.getSaksinfo();
    }

    public static FritekstbrevTrygdemyndighet av(FritekstbrevBrevbestilling brevbestilling, Mottakerroller mottakerType) {
        return new FritekstbrevTrygdemyndighet(brevbestilling, mottakerType);
    }
}
