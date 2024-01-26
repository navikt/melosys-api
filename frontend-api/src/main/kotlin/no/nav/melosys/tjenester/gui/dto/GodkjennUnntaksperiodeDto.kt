package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer
import no.nav.melosys.service.unntaksperiode.Unntaksperiode
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeGodkjenning
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto

@JvmRecord
data class GodkjennUnntaksperiodeDto(
    val varsleUtland: Boolean,
    val fritekst: String,
    val endretPeriode: PeriodeDto?,
    val lovvalgsbestemmelse: String
) {
    fun til(): UnntaksperiodeGodkjenning {
        val lovvalgBestemmelsekonverterer = LovvalgBestemmelsekonverterer()

        val endretPeriode = if (this.endretPeriode == null) null else Unntaksperiode(
            endretPeriode.fom,
            endretPeriode.tom
        )

        return UnntaksperiodeGodkjenning.builder()
            .varsleUtland(this.varsleUtland)
            .fritekst(this.fritekst)
            .endretPeriode(endretPeriode)
            .lovvalgsbestemmelse(lovvalgBestemmelsekonverterer.convertToEntityAttribute(this.lovvalgsbestemmelse))
            .build()
    }
}
