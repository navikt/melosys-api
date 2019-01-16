
package no.nav.melosys.eux.model.nav;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


@SuppressWarnings("unused")
@JsonInclude(Include.NON_NULL)
public class Fastperiode {


    private String sluttdato;

    private String startdato;

    public String getSluttdato() {
        return sluttdato;
    }

    public void setSluttdato(String sluttdato) {
        this.sluttdato = sluttdato;
    }

    public String getStartdato() {
        return startdato;
    }

    public void setStartdato(String startdato) {
        this.startdato = startdato;
    }

}
