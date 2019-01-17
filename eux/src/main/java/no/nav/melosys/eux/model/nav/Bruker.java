
package no.nav.melosys.eux.model.nav;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;

@SuppressWarnings("unused")
@JsonInclude(Include.NON_NULL)
public class Bruker {


    private List<Adresse> adresse;

    private Far far;

    private Mor mor;

    private Person person;

    public List<Adresse> getAdresse() {
        return adresse;
    }

    public void setAdresse(List<Adresse> adresse) {
        this.adresse = adresse;
    }

    public Far getFar() {
        return far;
    }

    public void setFar(Far far) {
        this.far = far;
    }

    public Mor getMor() {
        return mor;
    }

    public void setMor(Mor mor) {
        this.mor = mor;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

}
