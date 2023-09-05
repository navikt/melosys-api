package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.FritekstvedleggBrevbestilling;
import no.nav.melosys.domain.kodeverk.Mottakerroller;

public class Fritekstvedlegg extends DokgenDto {
    private final String fritekstTittel;
    private final String fritekst;

    protected Fritekstvedlegg(FritekstvedleggBrevbestilling brevbestilling, Mottakerroller mottakerType) {
        super(brevbestilling, mottakerType);
        this.fritekstTittel = brevbestilling.getFritekstvedleggTittel();
        this.fritekst = brevbestilling.getFritekstvedleggTekst();
    }

    public static Fritekstvedlegg av(FritekstvedleggBrevbestilling brevbestilling) {
        return new Fritekstvedlegg(brevbestilling, brevbestilling.getMottakerType());
    }

    public String getFritekstTittel() {
        return fritekstTittel;
    }

    public String getFritekst() {
        return fritekst;
    }
}
