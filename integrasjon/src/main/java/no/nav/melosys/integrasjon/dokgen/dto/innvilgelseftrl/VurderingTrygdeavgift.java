package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

public record VurderingTrygdeavgift(
    TrygdeavgiftInfo norsk,
    TrygdeavgiftInfo utenlandsk
) {
}
