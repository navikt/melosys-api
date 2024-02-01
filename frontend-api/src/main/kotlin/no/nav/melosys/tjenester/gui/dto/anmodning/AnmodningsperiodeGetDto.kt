package no.nav.melosys.tjenester.gui.dto.anmodning

import no.nav.melosys.domain.Anmodningsperiode
import java.util.stream.Collectors

class AnmodningsperiodeGetDto {
    var anmodningsperioder: List<AnmodningsperiodeLesDto>? = null

    private constructor(anmodningsperioder: Collection<Anmodningsperiode>) {
        this.anmodningsperioder = anmodningsperioder.stream()
            .map { anmodningsperiode: Anmodningsperiode? ->
                AnmodningsperiodeLesDto.av(
                    anmodningsperiode!!
                )
            }
            .collect(Collectors.toList())
    }

    companion object {
        @JvmStatic
        fun av(anmodningsperioder: Collection<Anmodningsperiode>): AnmodningsperiodeGetDto {
            return AnmodningsperiodeGetDto(anmodningsperioder)
        }
    }
}
