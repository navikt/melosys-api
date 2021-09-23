package no.nav.melosys.tjenester.gui.dto.utpeking;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Utpekingsperiode;

public record UtpekingsperioderDto(List<UtpekingsperiodeDto> utpekingsperioder) {
    public static UtpekingsperioderDto av(Collection<Utpekingsperiode> utpekingsperioder) {
        return new UtpekingsperioderDto(
            utpekingsperioder.stream()
                .map(UtpekingsperiodeDto::av)
                .collect(Collectors.toList())
        );
    }

    public static List<Utpekingsperiode> tilDomene(UtpekingsperioderDto utpekingsperioderDto) {
        return utpekingsperioderDto.utpekingsperioder.stream()
            .map(UtpekingsperiodeDto::tilDomene)
            .collect(Collectors.toList());
    }
}
