package no.nav.melosys.tjenester.gui.dto.anmodning;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Anmodningsperiode;

public final class AnmodningsperiodePostDto {

    private List<AnmodningsperiodeSkrivDto> anmodningsperioder;

    public AnmodningsperiodePostDto(){
    }

    public AnmodningsperiodePostDto(Collection<Anmodningsperiode> anmodningsperioder) {
        this.anmodningsperioder = anmodningsperioder.stream()
            .map(AnmodningsperiodeSkrivDto::av)
            .collect(Collectors.toList());
    }

    public static AnmodningsperiodePostDto av(Collection<Anmodningsperiode> anmodningsperioder) {
        return new AnmodningsperiodePostDto(anmodningsperioder);
    }

    public List<AnmodningsperiodeSkrivDto> getAnmodningsperioder() {
        return anmodningsperioder;
    }

    public void setAnmodningsperioder(List<AnmodningsperiodeSkrivDto> anmodningsperioder) {
        this.anmodningsperioder = anmodningsperioder;
    }
}
