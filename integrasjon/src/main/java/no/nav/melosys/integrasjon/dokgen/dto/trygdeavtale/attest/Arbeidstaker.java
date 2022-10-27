package no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.attest;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public record Arbeidstaker(
    String navn,

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = STRING)
    LocalDate foedselsdato,

    String fnr,

    List<String> bostedsadresse
) {
    public static Arbeidstaker av(no.nav.melosys.domain.brev.trygdeavtale.Arbeidstaker arbeidstaker) {
        if (arbeidstaker == null) return null;

        return new Arbeidstaker(arbeidstaker.navn(), arbeidstaker.foedselsdato(), arbeidstaker.fnr(), arbeidstaker.bostedsadresse());
    }
}
