package no.nav.melosys.eux.model.nav;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public class AapenPeriode {

    @JsonProperty("type")
    private String ukjentEllerÅpenSluttdato;

    private String startdato;

    public String getUkjentEllerÅpenSluttdato() {
        return ukjentEllerÅpenSluttdato;
    }

    public void setUkjentEllerÅpenSluttdato(String ukjentEllerÅpenSluttdato) {
        this.ukjentEllerÅpenSluttdato = ukjentEllerÅpenSluttdato;
    }

    public String getStartdato() {
        return startdato;
    }

    public void setStartdato(String startdato) {
        this.startdato = startdato;
    }
}
