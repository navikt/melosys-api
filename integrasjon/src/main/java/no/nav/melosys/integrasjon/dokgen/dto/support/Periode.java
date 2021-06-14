package no.nav.melosys.integrasjon.dokgen.dto.support;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class Periode {

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = STRING)
    private final LocalDate fom;

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = STRING)
    private final LocalDate tom;

    private final String bestemmelse;
    private final String innvilgelsesResultat;
    private final String medlemskapstype;
    private final String dekning;

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public String getBestemmelse() {
        return bestemmelse;
    }

    public String getInnvilgelsesResultat() {
        return innvilgelsesResultat;
    }

    public String getMedlemskapstype() {
        return medlemskapstype;
    }

    public String getDekning() {
        return dekning;
    }
}
