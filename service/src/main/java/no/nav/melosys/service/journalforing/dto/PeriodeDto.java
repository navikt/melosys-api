package no.nav.melosys.service.journalforing.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import no.nav.melosys.domain.ErPeriode;

@JsonPropertyOrder({
        "fom",
        "tom"
})
public class PeriodeDto implements ErPeriode {

    @JsonProperty("fom")
    private LocalDate fom;
    @JsonProperty("tom")
    private LocalDate tom;
    
    public PeriodeDto() {
    }

    public PeriodeDto(LocalDate fom, LocalDate tom) {
        this.fom = fom;
        this.tom = tom;
    }

    @Override
    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    @Override
    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }
}
