package no.nav.melosys.domain.eessi;

import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;

public class Organisasjon {

    private String orgnr;
    private String navn;
    private StrukturertAdresse adresse;

    public String getOrgnr() {
        return orgnr;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public StrukturertAdresse getAdresse() {
        return adresse;
    }

    public void setAdresse(StrukturertAdresse adresse) {
        this.adresse = adresse;
    }
}
