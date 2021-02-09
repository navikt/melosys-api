package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public class ProduserBrevDTO {
    private Aktoersroller mottaker;
    private String orgNr;
    private String innledningFritekst;
    private String manglerFritekst;
    private String fullmektigNavn;

    public Aktoersroller getMottaker() {
        return mottaker;
    }

    public String getOrgNr() {
        return orgNr;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getManglerFritekst() {
        return manglerFritekst;
    }

    public String getFullmektigNavn() {
        return fullmektigNavn;
    }
}
