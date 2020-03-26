package no.nav.melosys.domain.eessi;

import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;

public class Organisasjon {

    private String orgnr;
    private String navn;
    private StrukturertAdresse postadresse;
    private StrukturertAdresse forretningsadresse;

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

    public StrukturertAdresse getPostadresse() {
        return postadresse;
    }

    public void setPostadresse(StrukturertAdresse postadresse) {
        this.postadresse = postadresse;
    }

    public StrukturertAdresse getForretningsadresse() {
        return forretningsadresse;
    }

    public void setForretningsadresse(StrukturertAdresse forretningsadresse) {
        this.forretningsadresse = forretningsadresse;
    }
}
