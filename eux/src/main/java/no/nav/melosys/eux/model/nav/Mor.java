
package no.nav.melosys.eux.model.nav;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


@SuppressWarnings("unused")
@JsonInclude(Include.NON_NULL)
public class Mor {


    private Person person;

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

}
