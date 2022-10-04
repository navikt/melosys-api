package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.FritekstvedleggBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;

public class Fritekstvedlegg extends DokgenDto {
    private final String fritekstTittel;
    private final String fritekst;

    protected Fritekstvedlegg(FritekstvedleggBrevbestilling brevbestilling, Aktoersroller mottakerType) {
        super(brevbestilling, mottakerType);
        this.fritekstTittel = brevbestilling.getFritekstTittel();
        this.fritekst = brevbestilling.getFritekst();
    }

    public static Fritekstvedlegg av(FritekstvedleggBrevbestilling brevbestilling, Aktoersroller mottakerType) {
        return new Fritekstvedlegg(brevbestilling, mottakerType);
    }

    public String getFritekstTittel() {
        return fritekstTittel;
    }

    public String getFritekst() {
        return fritekst;
    }
}
