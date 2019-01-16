
package no.nav.melosys.eux.model.nav;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;


@SuppressWarnings("unused")
@JsonInclude(Include.NON_NULL)
public class Arbeidsgiver {


    private Adresse adresse;

    private List<Identifikator> identifikator;

    private String navn;

    public Adresse getAdresse() {
        return adresse;
    }

    public void setAdresse(Adresse adresse) {
        this.adresse = adresse;
    }

    public List<Identifikator> getIdentifikator() {
        return identifikator;
    }

    public void setIdentifikator(List<Identifikator> identifikator) {
        this.identifikator = identifikator;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

}
