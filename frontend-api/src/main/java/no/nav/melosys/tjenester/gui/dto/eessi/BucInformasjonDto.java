package no.nav.melosys.tjenester.gui.dto.eessi;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.eessi.BucInformasjon;

public class BucInformasjonDto {
    private final String id;
    private final String bucType;
    private final LocalDate opprettetDato;
    private final List<SedInformasjonDto> seder;

    public BucInformasjonDto(String id, String bucType, LocalDate opprettetDato, List<SedInformasjonDto> seder) {
        this.id = id;
        this.bucType = bucType;
        this.opprettetDato = opprettetDato;
        this.seder = seder;
    }

    public static BucInformasjonDto av(BucInformasjon bucInformasjon) {
        return new BucInformasjonDto(
            bucInformasjon.getId(),
            bucInformasjon.getBucType(),
            bucInformasjon.getOpprettetDato(),
            bucInformasjon.getSeder().stream()
                .map(SedInformasjonDto::av)
                .collect(Collectors.toList())
        );
    }

    public String getId() {
        return id;
    }

    public String getBucType() {
        return bucType;
    }

    public LocalDate getOpprettetDato() {
        return opprettetDato;
    }

    public List<SedInformasjonDto> getSeder() {
        return seder;
    }
}
