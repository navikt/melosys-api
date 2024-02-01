package no.nav.melosys.tjenester.gui.dto.eessi

import no.nav.melosys.domain.eessi.BucInformasjon
import no.nav.melosys.domain.eessi.SedInformasjon
import java.time.LocalDate
import java.util.stream.Collectors

@JvmRecord
data class BucInformasjonDto(
    val id: String,
    val bucType: String,
    val opprettetDato: LocalDate,
    val seder: List<SedInformasjonDto>
) {
    companion object {
        @JvmStatic
        fun av(bucInformasjon: BucInformasjon): BucInformasjonDto {
            return BucInformasjonDto(
                bucInformasjon.id,
                bucInformasjon.bucType,
                bucInformasjon.opprettetDato,
                bucInformasjon.seder.stream()
                    .map { sedInformasjon: SedInformasjon? ->
                        SedInformasjonDto.av(
                            sedInformasjon!!
                        )
                    }
                    .collect(Collectors.toList())
            )
        }
    }
}
