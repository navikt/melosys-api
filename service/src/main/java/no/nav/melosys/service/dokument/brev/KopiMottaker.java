package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public class KopiMottaker {
    private Aktoersroller rolle;
    private String orgnr;
    private String aktørId;

    public KopiMottaker(Aktoersroller rolle, String orgnr, String aktørId) {
        this.rolle = rolle;
        this.orgnr = orgnr;
        this.aktørId = aktørId;
    }

    public Aktoersroller getRolle() {
        return rolle;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public String getAktørId() {
        return aktørId;
    }
}
