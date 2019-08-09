package no.nav.melosys.tjenester.gui.dto.anmodning;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Anmodningsperiode;

public final class AnmodningsperiodePostDto {

    private List<AnmodningsperiodeDto> anmodningsperioder;

    public AnmodningsperiodePostDto(Collection<Anmodningsperiode> anmodningsperioder) {
        this.anmodningsperioder = anmodningsperioder.stream()
            .map(AnmodningsperiodeDto::av)
            .collect(Collectors.toList());
    }

    public static AnmodningsperiodePostDto av(Collection<Anmodningsperiode> anmodningsperioder) {
        return new AnmodningsperiodePostDto(anmodningsperioder);
    }

    public List<AnmodningsperiodeDto> getAnmodningsperioder() {
        return anmodningsperioder;
    }

    public void setAnmodningsperioder(List<AnmodningsperiodeDto> anmodningsperioder) {
        this.anmodningsperioder = anmodningsperioder;
    }
}
