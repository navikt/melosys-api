package no.nav.melosys.integrasjon.dokgen.dto.atteststorbritannia;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public record Arbeidstaker(
    String navn,

    @JsonFormat(shape = STRING)
    LocalDate foedselsdato,

    String fnr,

    List<String> bostedsadresse
) {
    public static Arbeidstaker av(no.nav.melosys.domain.brev.storbritannia.Arbeidstaker arbeidstaker) {
        if (arbeidstaker == null) return null;

        return new Arbeidstaker(arbeidstaker.navn(), arbeidstaker.foedselsdato(), arbeidstaker.fnr(), arbeidstaker.bostedsadresse());
    }
}
