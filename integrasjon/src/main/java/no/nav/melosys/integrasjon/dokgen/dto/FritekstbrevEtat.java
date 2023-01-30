package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker;

public class FritekstbrevEtat extends Fritekstbrev {

    private FritekstbrevEtat(FritekstbrevBrevbestilling brevbestilling) {
        super(brevbestilling, Aktoersroller.ETAT, SaksinfoBruker.av(brevbestilling));
    }

    @Override
    public SaksinfoBruker getSaksinfo() {
        return (SaksinfoBruker) super.getSaksinfo();
    }

    public static FritekstbrevEtat av(FritekstbrevBrevbestilling brevbestilling) {
        return new FritekstbrevEtat(brevbestilling);
    }
}
