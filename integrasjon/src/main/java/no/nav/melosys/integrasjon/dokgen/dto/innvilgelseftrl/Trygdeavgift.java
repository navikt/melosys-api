package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

import java.math.BigDecimal;

public class Trygdeavgift {
    private final BigDecimal beloepMd;
    private final BigDecimal sats;
    private final String avgiftskode;
    private final String forInntekt;

    public Trygdeavgift(no.nav.melosys.domain.avgift.Trygdeavgift trygdeavgift) {
        this.beloepMd = trygdeavgift.getTrygdeavgiftsbeløpMd();
        this.sats = trygdeavgift.getTrygdesats();
        this.avgiftskode = trygdeavgift.getAvgiftskode();
        this.forInntekt = trygdeavgift.getAvgiftForInntekt().name();
    }

    public BigDecimal getBeloepMd() {
        return beloepMd;
    }

    public BigDecimal getSats() {
        return sats;
    }

    public String getAvgiftskode() {
        return avgiftskode;
    }

    public String getForInntekt() {
        return forInntekt;
    }
}
