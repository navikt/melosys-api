package no.nav.melosys.tjenester.gui.dto.anmodning;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Anmodningsperiode;

public final class AnmodningsperiodeListeDto {
    private List<AnmodningsperiodeGetDto> anmodningsperioder;

    public AnmodningsperiodeListeDto() {
    }

    private AnmodningsperiodeListeDto(Collection<Anmodningsperiode> anmodningsperioder) {
            this.anmodningsperioder = anmodningsperioder.stream()
            .map(AnmodningsperiodeGetDto::av)
            .collect(Collectors.toList());
    }

    public static AnmodningsperiodeListeDto av(Collection<Anmodningsperiode> anmodningsperioder) {
        return new AnmodningsperiodeListeDto(anmodningsperioder);
    }

    public void setAnmodningsperioder(List<AnmodningsperiodeGetDto> anmodningsperioder) {
        this.anmodningsperioder = anmodningsperioder;
    }

    public List<AnmodningsperiodeGetDto> getAnmodningsperioder() {
        return anmodningsperioder;
    }
}
