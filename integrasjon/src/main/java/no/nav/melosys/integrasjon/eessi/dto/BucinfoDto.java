package no.nav.melosys.integrasjon.eessi.dto;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.eessi.BucInformasjon;

public class BucinfoDto {

    private String id;
    private boolean erÅpen;
    private String bucType;
    private Long opprettetDato;
    private Set<String> mottakerinstitusjoner = new HashSet<>();
    private List<SedinfoDto> seder;

    public BucinfoDto() {
    }

    public BucinfoDto(String id, boolean erÅpen, String bucType, Long opprettetDato, List<SedinfoDto> seder) {
        this.id = id;
        this.erÅpen = erÅpen;
        this.bucType = bucType;
        this.opprettetDato = opprettetDato;
        this.seder = seder;
    }

    public BucInformasjon tilDomene() {
        return new BucInformasjon(
            id,
            erÅpen,
            bucType,
            Instant.ofEpochMilli(opprettetDato).atZone(ZoneId.systemDefault()).toLocalDate(),
            mottakerinstitusjoner,
            seder.stream()
                .map(SedinfoDto::tilDomene)
                .collect(Collectors.toList())
        );
    }

    public static BucinfoDto av(BucInformasjon bucInformasjon) {
        return new BucinfoDto(
            bucInformasjon.getId(),
            bucInformasjon.erÅpen(),
            bucInformasjon.getBucType(),
            bucInformasjon.getOpprettetDato().toEpochDay(),
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

    public boolean isErÅpen() {
        return erÅpen;
    }

    public void setErÅpen(boolean erÅpen) {
        this.erÅpen = erÅpen;
    }

    public String getBucType() {
        return bucType;
    }

    public void setBucType(String bucType) {
        this.bucType = bucType;
    }

    public Long getOpprettetDato() {
        return opprettetDato;
    }

    public void setOpprettetDato(Long opprettetDato) {
        this.opprettetDato = opprettetDato;
    }

    public Set<String> getMottakerinstitusjoner() {
        return mottakerinstitusjoner;
    }

    public void setMottakerinstitusjoner(Set<String> mottakerinstitusjoner) {
        this.mottakerinstitusjoner = mottakerinstitusjoner;
    }

    public List<SedinfoDto> getSeder() {
        return seder;
    }

    public void setSeder(List<SedinfoDto> seder) {
        this.seder = seder;
    }
}
