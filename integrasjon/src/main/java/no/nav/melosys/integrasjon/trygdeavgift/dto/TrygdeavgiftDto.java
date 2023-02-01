package no.nav.melosys.integrasjon.trygdeavgift.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TrygdeavgiftDto {
    private final LocalDate avgiftPeriodeFom;
    private final LocalDate avgiftPeriodeTom;
    private final String avgiftskode;
    private final BigDecimal avgiftssats;
    private final BigDecimal maanedsavgift;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TrygdeavgiftDto(
        @JsonProperty("avgiftPeriodeFra") LocalDate avgiftPeriodeFom,
        @JsonProperty("avgiftPeriodeTil") LocalDate avgiftPeriodeTom,
        @JsonProperty("avgiftskode") String avgiftskode,
        @JsonProperty("avgiftssats") BigDecimal avgiftssats,
        @JsonProperty("kvartalsavgift") BigDecimal maanedsavgift) {
        this.avgiftskode = avgiftskode;
        this.avgiftssats = avgiftssats;
        this.maanedsavgift = maanedsavgift;
        this.avgiftPeriodeFom = avgiftPeriodeFom;
        this.avgiftPeriodeTom = avgiftPeriodeTom;
    }

    public String getAvgiftskode() {
        return avgiftskode;
    }

    public BigDecimal getAvgiftssats() {
        return avgiftssats;
    }

    public BigDecimal getMaanedsavgift() {
        return maanedsavgift;
    }

    public LocalDate getAvgiftPeriodeFom() {
        return avgiftPeriodeFom;
    }

    public LocalDate getAvgiftPeriodeTom() {
        return avgiftPeriodeTom;
    }
}
