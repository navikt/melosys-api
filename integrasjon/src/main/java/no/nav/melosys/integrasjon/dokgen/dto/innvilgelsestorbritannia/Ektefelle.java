package no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public record Ektefelle(
    String navn,

    boolean omfattet,

    Medfolgende_ektefelle_samboer_begrunnelser_ftrl begrunnelse,

    String fnr,

    // Brukes som backup om fnr mangler
    String dnr,

    // Brukes som backup om fnr og dnr mangler
    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    LocalDate foedselsdato
) {
}
