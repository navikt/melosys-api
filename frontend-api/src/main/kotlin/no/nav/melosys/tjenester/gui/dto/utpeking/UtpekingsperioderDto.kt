package no.nav.melosys.tjenester.gui.dto.utpeking

import no.nav.melosys.domain.Utpekingsperiode
import java.util.stream.Collectors

@JvmRecord
data class UtpekingsperioderDto(val utpekingsperioder: List<UtpekingsperiodeDto>) {
    companion object {
        @JvmStatic
        fun av(utpekingsperioder: Collection<Utpekingsperiode>): UtpekingsperioderDto {
            return UtpekingsperioderDto(
                utpekingsperioder.stream()
                    .map { utpekingsperiode: Utpekingsperiode -> UtpekingsperiodeDto.av(utpekingsperiode) }
                    .collect(Collectors.toList())
            )
        }

        @JvmStatic
        fun tilDomene(utpekingsperioderDto: UtpekingsperioderDto): List<Utpekingsperiode> {
            return utpekingsperioderDto.utpekingsperioder.stream()
                .map { obj: UtpekingsperiodeDto -> obj.tilDomene() }
                .collect(Collectors.toList())
        }
    }
}
