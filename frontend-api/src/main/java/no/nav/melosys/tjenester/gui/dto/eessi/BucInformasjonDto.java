package no.nav.melosys.tjenester.gui.dto.eessi;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.eessi.BucInformasjon;

public record BucInformasjonDto(String id, String bucType, LocalDate opprettetDato, List<SedInformasjonDto> seder) {

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
}
