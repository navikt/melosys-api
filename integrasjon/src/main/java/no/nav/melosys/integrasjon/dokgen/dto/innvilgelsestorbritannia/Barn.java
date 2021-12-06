package no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public record Barn(
    String navn,

    boolean omfattet,

    Medfolgende_barn_begrunnelser_ftrl begrunnelse,

    String fnr,

    String dnr,

    @JsonFormat(shape = STRING)
    LocalDate foedselsdato
) {
}
