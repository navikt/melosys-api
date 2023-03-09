package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Saksinfo;

public class Fritekstbrev extends DokgenDto {
    private final String fritekstTittel;
    private final String fritekst;
    private final boolean medKontaktopplysninger;
    private final String navnFullmektig;
    private final String saksbehandlerNrToNavn;
    private final boolean brukerSkalHaKopi;

    protected Fritekstbrev(FritekstbrevBrevbestilling brevbestilling, Mottakerroller mottakerType, Saksinfo saksinfo) {
        super(brevbestilling, mottakerType, saksinfo);
        this.fritekstTittel = brevbestilling.getFritekstTittel();
        this.fritekst = brevbestilling.getFritekst();
        this.medKontaktopplysninger = brevbestilling.isKontaktopplysninger();
        this.navnFullmektig = brevbestilling.getNavnFullmektig();
        this.saksbehandlerNrToNavn = brevbestilling.getSaksbehandlerNrToNavn();
        this.brukerSkalHaKopi = brevbestilling.isBrukerSkalHaKopi();
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

    public String getSaksbehandlerNrToNavn() {
        return saksbehandlerNrToNavn;
    }

    public boolean isBrukerSkalHaKopi() {
        return brukerSkalHaKopi;
    }
}
