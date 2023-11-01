package no.nav.melosys.service.journalforing.dto;

import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.saksflytapi.journalfoering.Periode;

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

    public Periode tilPeriode(){
        return new Periode(this.fom, this.tom);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PeriodeDto)) return false;
        PeriodeDto that = (PeriodeDto) o;
        return getFom().equals(that.getFom()) &&
            Objects.equals(getTom(), that.getTom());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFom(), getTom());
    }
}
