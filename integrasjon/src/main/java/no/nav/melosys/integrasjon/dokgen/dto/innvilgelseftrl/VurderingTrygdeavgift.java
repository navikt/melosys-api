package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record VurderingTrygdeavgift(
    TrygdeavgiftInfo norsk,
    TrygdeavgiftInfo utenlandsk,
    boolean selvbetalende,
    String representantNavn
) {
}
