package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker;

public class FritekstbrevBruker extends Fritekstbrev {

    private FritekstbrevBruker(FritekstbrevBrevbestilling brevbestilling, Aktoersroller mottakerType) {
        super(brevbestilling, mottakerType, SaksinfoBruker.av(brevbestilling));
    }

    @Override
    public SaksinfoBruker getSaksinfo() {
        return (SaksinfoBruker) super.getSaksinfo();
    }

    public static FritekstbrevBruker av(FritekstbrevBrevbestilling brevbestilling, Aktoersroller mottakerType) {
        return new FritekstbrevBruker(brevbestilling, mottakerType);
    }
}
