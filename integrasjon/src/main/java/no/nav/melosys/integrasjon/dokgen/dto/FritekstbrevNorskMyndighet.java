package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker;

public class FritekstbrevNorskMyndighet extends Fritekstbrev {

    private FritekstbrevNorskMyndighet(FritekstbrevBrevbestilling brevbestilling) {
        super(brevbestilling, Mottakerroller.NORSK_MYNDIGHET, SaksinfoBruker.av(brevbestilling));
    }

    @Override
    public SaksinfoBruker getSaksinfo() {
        return (SaksinfoBruker) super.getSaksinfo();
    }

    public static FritekstbrevNorskMyndighet av(FritekstbrevBrevbestilling brevbestilling) {
        return new FritekstbrevNorskMyndighet(brevbestilling);
    }
}
