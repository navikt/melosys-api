package no.nav.melosys.domain.dokument.arbeidsforhold;

import java.math.BigDecimal;
import java.time.YearMonth;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.dokument.felles.Periode;


public class AntallTimerIPerioden {

    @JsonProperty("timelonnetPeriode")
    private Periode periode;

    private BigDecimal antallTimer;

    @JsonProperty("rapporteringsAarMaaned")
    private YearMonth rapporteringsperiode;

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    public BigDecimal getAntallTimer() {
        return antallTimer;
    }

    public void setAntallTimer(BigDecimal antallTimer) {
        this.antallTimer = antallTimer;
    }

    public YearMonth getRapporteringsperiode() {
        return rapporteringsperiode;
    }

    public void setRapporteringsperiode(YearMonth rapporteringsperiode) {
        this.rapporteringsperiode = rapporteringsperiode;
    }
}
