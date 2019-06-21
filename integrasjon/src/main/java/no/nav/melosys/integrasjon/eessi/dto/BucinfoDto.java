package no.nav.melosys.integrasjon.eessi.dto;

import java.time.LocalDate;
import java.util.List;

public class BucinfoDto {

    private String id;
    private String bucType;
    private LocalDate opprettetDato;
    private List<SedinfoDto> seder;

    public BucinfoDto() {
    }

    public BucinfoDto(String id, String bucType, LocalDate opprettetDato, List<SedinfoDto> seder) {
        this.id = id;
        this.bucType = bucType;
        this.opprettetDato = opprettetDato;
        this.seder = seder;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBucType() {
        return bucType;
    }

    public void setBucType(String bucType) {
        this.bucType = bucType;
    }

    public LocalDate getOpprettetDato() {
        return opprettetDato;
    }

    public void setOpprettetDato(LocalDate opprettetDato) {
        this.opprettetDato = opprettetDato;
    }

    public List<SedinfoDto> getSeder() {
        return seder;
    }

    public void setSeder(List<SedinfoDto> seder) {
        this.seder = seder;
    }
}
