package no.nav.melosys.integrasjon.dokgen.dto.felles;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public record Person(
    String navn,

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = STRING)
    LocalDate foedselsdato,

    String fnr,

    String dnr
) {
    public static Person av(no.nav.melosys.domain.brev.Person person) {
        return new Person(person.navn(), person.foedselsdato(), person.fnr(), person.dnr());
    }
}
