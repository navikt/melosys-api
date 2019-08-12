package no.nav.melosys.tjenester.gui.dto.anmodning;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Anmodningsperiode;

public final class AnmodningsperiodeGetDto {
    private List<AnmodningsperiodeLesDto> anmodningsperioder;

    public AnmodningsperiodeGetDto() {
    }

    private AnmodningsperiodeGetDto(Collection<Anmodningsperiode> anmodningsperioder) {
            this.anmodningsperioder = anmodningsperioder.stream()
            .map(AnmodningsperiodeLesDto::av)
            .collect(Collectors.toList());
    }

    public static AnmodningsperiodeGetDto av(Collection<Anmodningsperiode> anmodningsperioder) {
        return new AnmodningsperiodeGetDto(anmodningsperioder);
    }

    public void setAnmodningsperioder(List<AnmodningsperiodeLesDto> anmodningsperioder) {
        this.anmodningsperioder = anmodningsperioder;
    }

    public List<AnmodningsperiodeLesDto> getAnmodningsperioder() {
        return anmodningsperioder;
    }
}
