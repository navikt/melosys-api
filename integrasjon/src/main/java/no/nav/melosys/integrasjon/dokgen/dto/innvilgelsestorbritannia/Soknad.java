package no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public record Soknad(
    @JsonFormat(shape = STRING)
    LocalDate soknadsdato,

    @JsonFormat(shape = STRING)
    LocalDate periodeFom,

    @JsonFormat(shape = STRING)
    LocalDate periodeTom,

    String virksomhetsnavn
) {
}
