package no.nav.melosys.integrasjon.dokgen.dto.ikkeyrkesaktiv;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class IkkeYrkesaktivPeriode {

    @JsonFormat(shape = STRING)
    private final LocalDate fom;

    @JsonFormat(shape = STRING)
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
