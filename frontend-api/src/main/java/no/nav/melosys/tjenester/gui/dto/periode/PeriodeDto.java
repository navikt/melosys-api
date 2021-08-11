package no.nav.melosys.tjenester.gui.dto.periode;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "fom",
        "tom"
})
public class PeriodeDto {

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

    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    public boolean erTom() {
        return fom == null && tom == null;
    }
}
