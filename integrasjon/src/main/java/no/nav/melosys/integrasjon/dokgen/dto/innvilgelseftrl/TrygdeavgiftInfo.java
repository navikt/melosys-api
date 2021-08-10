package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

public record TrygdeavgiftInfo(
    long avgiftspliktigInntektMd,
    boolean trygdeavgiftNav,
    boolean erSkattepliktig,
    boolean arbeidsgiverBetalerAvgift,
    String saerligeavgiftsgruppe
) {
}
