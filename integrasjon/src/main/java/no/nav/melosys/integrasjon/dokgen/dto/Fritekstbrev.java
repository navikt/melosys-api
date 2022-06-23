package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Saksinfo;

public class Fritekstbrev extends DokgenDto {
    private final String fritekstTittel;
    private final String fritekst;
    private final boolean medKontaktopplysninger;
    private final String navnFullmektig;

    protected Fritekstbrev(FritekstbrevBrevbestilling brevbestilling, Aktoersroller mottakerType, Saksinfo saksinfo) {
        super(brevbestilling, mottakerType, saksinfo);
        this.fritekstTittel = brevbestilling.getFritekstTittel();
        this.fritekst = brevbestilling.getFritekst();
        this.medKontaktopplysninger = brevbestilling.isKontaktopplysninger();
        this.navnFullmektig = brevbestilling.getNavnFullmektig();
    }

    public String getFritekstTittel() {
        return fritekstTittel;
    }

    public String getFritekst() {
        return fritekst;
    }

    public boolean isMedKontaktopplysninger() {
        return medKontaktopplysninger;
    }

    public String getNavnFullmektig() {
        return navnFullmektig;
    }
}
