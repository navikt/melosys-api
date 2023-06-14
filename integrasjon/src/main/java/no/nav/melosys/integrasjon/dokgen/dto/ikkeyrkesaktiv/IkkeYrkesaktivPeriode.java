package no.nav.melosys.integrasjon.dokgen.dto.ikkeyrkesaktiv;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import java.time.LocalDate;

public class IkkeYrkesaktivPeriode {

    @JsonSerialize(using = LocalDateSerializer.class)
    private final LocalDate fom;

    @JsonSerialize(using = LocalDateSerializer.class)
    private final LocalDate tom;


    public IkkeYrkesaktivPeriode(LocalDate fom, LocalDate tom) {
        this.fom = fom;
        this.tom = tom;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }
}
