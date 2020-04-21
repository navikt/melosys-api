package no.nav.melosys.domain.eessi;

import java.time.LocalDate;

import no.nav.melosys.domain.AbstraktOrganisasjon;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;

public class SedOrganisasjon extends AbstraktOrganisasjon {

    private String navn;
    private StrukturertAdresse postadresse;
    private StrukturertAdresse forretningsadresse;

    public void setOrgnummer(String orgnummer) {
        this.orgnummer = orgnummer;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    @Override
    public StrukturertAdresse getPostadresse() {
        return postadresse;
    }

    @Override
    public StrukturertAdresse getForretningsadresse() {
        return forretningsadresse;
    }

    public void setForretningsadresse(StrukturertAdresse forretningsadresse) {
        this.forretningsadresse = forretningsadresse;
    }

    public void setPostadresse(StrukturertAdresse postadresse) {
        this.postadresse = postadresse;
    }

    @Override
    public LocalDate getOppstartsdato() {
        return oppstartsdato;
    }

    @Override
    public String getEnhetstype() {
        return enhetstype;
    }
}
