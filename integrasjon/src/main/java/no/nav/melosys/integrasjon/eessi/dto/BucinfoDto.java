package no.nav.melosys.integrasjon.eessi.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.eessi.BucInformasjon;

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

    public BucInformasjon tilDomene() {
        return new BucInformasjon(
            id,
            bucType,
            opprettetDato,
            seder.stream()
                .map(SedinfoDto::tilDomene)
                .collect(Collectors.toList())
        );
    }

    public static BucinfoDto av(BucInformasjon bucInformasjon) {
        return new BucinfoDto(
            bucInformasjon.getId(),
            bucInformasjon.getBucType(),
            bucInformasjon.getOpprettetDato(),
            bucInformasjon.getSeder().stream()
                .map(SedinfoDto::av)
                .collect(Collectors.toList())
        );
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
