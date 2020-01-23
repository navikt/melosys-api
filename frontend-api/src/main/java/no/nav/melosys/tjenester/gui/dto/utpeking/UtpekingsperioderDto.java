package no.nav.melosys.tjenester.gui.dto.utpeking;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Utpekingsperiode;

public class UtpekingsperioderDto {
    private List<UtpekingsperiodeDto> utpekingsperioder;

    public UtpekingsperioderDto(Collection<Utpekingsperiode> utpekingsperioder) {
        this.utpekingsperioder = utpekingsperioder.stream()
            .map(UtpekingsperiodeDto::av)
            .collect(Collectors.toList());
    }

    public List<UtpekingsperiodeDto> getUtpekingsperioder() {
        return utpekingsperioder;
    }

    public void setUtpekingsperioder(List<UtpekingsperiodeDto> utpekingsperioder) {
        this.utpekingsperioder = utpekingsperioder;
    }

    public static UtpekingsperioderDto av(Collection<Utpekingsperiode> utpekingsperioder) {
        return new UtpekingsperioderDto(utpekingsperioder);
    }

    public static List<Utpekingsperiode> tilDomene(UtpekingsperioderDto utpekingsperioderDto) {
        return utpekingsperioderDto.utpekingsperioder.stream()
            .map(UtpekingsperiodeDto::tilDomene)
            .collect(Collectors.toList());
    }
}
