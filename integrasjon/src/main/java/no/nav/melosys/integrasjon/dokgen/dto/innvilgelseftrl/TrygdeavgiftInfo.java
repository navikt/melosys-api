package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record TrygdeavgiftInfo(
    long avgiftspliktigInntektMd,
    boolean trygdeavgiftNav,
    boolean erSkattepliktig,
    boolean arbeidsgiverBetalerAvgift,
    String saerligeavgiftsgruppe
) {
}
