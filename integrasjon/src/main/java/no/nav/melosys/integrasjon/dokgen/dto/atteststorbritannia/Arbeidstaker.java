package no.nav.melosys.integrasjon.dokgen.dto.atteststorbritannia;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public record Arbeidstaker(
    String navn,

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    Instant foedselsdato,

    String fnr,

    List<String> bostedadresse
) {
    public static Arbeidstaker av(no.nav.melosys.domain.brev.storbritannia.Arbeidstaker arbeidstaker) {
        return new Arbeidstaker(arbeidstaker.navn(), arbeidstaker.foedselsdato(), arbeidstaker.fnr(), arbeidstaker.bostedsadresse());
    }
}
