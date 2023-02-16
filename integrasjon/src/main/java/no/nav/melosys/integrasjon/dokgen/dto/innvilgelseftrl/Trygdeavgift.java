package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

import java.math.BigDecimal;

public record Trygdeavgift(
    BigDecimal beloepMd,
    BigDecimal sats,
    String avgiftskode,
    String forInntekt
) {
}
