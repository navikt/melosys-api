package no.nav.melosys.tjenester.gui.dto.anmodning

import no.nav.melosys.domain.Anmodningsperiode
import java.util.stream.Collectors

class AnmodningsperiodePostDto {
    var anmodningsperioder: List<AnmodningsperiodeSkrivDto>? = null

    constructor()

    constructor(anmodningsperioder: Collection<Anmodningsperiode>) {
        this.anmodningsperioder = anmodningsperioder.stream()
            .map { anmodningsperiode: Anmodningsperiode -> AnmodningsperiodeSkrivDto.av(anmodningsperiode) }
            .collect(Collectors.toList())
    }

    companion object {
        @JvmStatic
        fun av(anmodningsperioder: Collection<Anmodningsperiode>): AnmodningsperiodePostDto {
            return AnmodningsperiodePostDto(anmodningsperioder)
        }
    }
}
