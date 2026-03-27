package no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.innvilgelse;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public record Soknad(
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = STRING)
    LocalDate soknadsdato,

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = STRING)
    LocalDate periodeFom,

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = STRING)
    LocalDate periodeTom,

    String virksomhetsnavn,
    String soknadsland
) {
}
