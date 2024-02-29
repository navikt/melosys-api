package no.nav.melosys.domain.dokument.person

import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.HarPeriode
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Periode
import java.time.LocalDateTime

data class StatsborgerskapPeriode(
    var periode: Periode?,
    // Attributtet endretAv er på formen "KODE_SYSTEM_KILDE" eller "ENDRET_AV, KODE_SYSTEM_KILDE"
    var endretAv: String = "",
    var endringstidspunkt: LocalDateTime?,
    var statsborgerskap: Land?
) : HarPeriode {
    companion object {
        private const val SKATTEDIREKTORATET = "SKD"
    }

    override fun getPeriode(): ErPeriode? {
        return periode
    }

    fun erFraSkattedirektoratet(): Boolean {
        return endretAv.contains(SKATTEDIREKTORATET)
    }
}
