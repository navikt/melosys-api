package no.nav.melosys.integrasjon.trygdeavgift.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TrygdeavgiftDto {
    private final String avgiftskode;
    private final BigDecimal avgiftssats;
    private final BigDecimal kvartalsavgift;

    @JsonCreator
    public TrygdeavgiftDto(@JsonProperty("avgiftskode") String avgiftskode,
                           @JsonProperty("avgiftssats") BigDecimal avgiftssats,
                           @JsonProperty("kvartalsavgift") BigDecimal kvartalsavgift) {
        this.avgiftskode = avgiftskode;
        this.avgiftssats = avgiftssats;
        this.kvartalsavgift = kvartalsavgift;
    }

    public String getAvgiftskode() {
        return avgiftskode;
    }

    public BigDecimal getAvgiftssats() {
        return avgiftssats;
    }

    public BigDecimal getKvartalsavgift() {
        return kvartalsavgift;
    }
}
