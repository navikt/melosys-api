
package no.nav.melosys.eux.model.nav;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@SuppressWarnings("unused")
@JsonInclude(Include.NON_NULL)
public class Arbeidssted {


    private Adresse adresse;

    private String erikkefastadresse;

    private String hjemmebase;

    private String navn;

    public Adresse getAdresse() {
        return adresse;
    }

    public void setAdresse(Adresse adresse) {
        this.adresse = adresse;
    }

    public String getErikkefastadresse() {
        return erikkefastadresse;
    }

    public void setErikkefastadresse(String erikkefastadresse) {
        this.erikkefastadresse = erikkefastadresse;
    }

    public String getHjemmebase() {
        return hjemmebase;
    }

    public void setHjemmebase(String hjemmebase) {
        this.hjemmebase = hjemmebase;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

}
