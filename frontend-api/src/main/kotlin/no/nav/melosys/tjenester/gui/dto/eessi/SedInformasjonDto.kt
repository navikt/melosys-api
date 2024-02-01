package no.nav.melosys.tjenester.gui.dto.eessi

import no.nav.melosys.domain.eessi.SedInformasjon
import java.time.LocalDate

class SedInformasjonDto(
    val bucId: String,
    val sedId: String,
    val opprettetDato: LocalDate,
    val sistOppdatert: LocalDate,
    val sedType: String,
    val status: String,
    val rinaUrl: String
) {
    companion object {
        fun av(sedInformasjon: SedInformasjon): SedInformasjonDto {
            return SedInformasjonDto(
                sedInformasjon.bucId,
                sedInformasjon.sedId,
                sedInformasjon.opprettetDato,
                sedInformasjon.sistOppdatert,
                sedInformasjon.sedType,
                sedInformasjon.status,
                sedInformasjon.rinaUrl
            )
        }
    }
}
