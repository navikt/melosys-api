package no.nav.melosys.domain

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

data class SakOgBehandlingDTO(
    val saksnummer: String,
    val behandlingID: Long,
    val sakstype: Sakstyper,
    val sakstema: Sakstemaer,
    val behandlingstype: Behandlingstyper,
    val behandlingstema: Behandlingstema,
    val behandlingstatus: Behandlingsstatus
) {
    fun htmlTableData(count: Int): String {
        val rowspan = if (count > 0) "rowspan=${count+1}" else ""
        return """
            <td $rowspan>$sakstype</td>
            <td $rowspan>$sakstema</td>
            <td $rowspan>$behandlingstype</td>
            <td $rowspan>$behandlingstema</td>
            <td $rowspan>$behandlingstatus</td>
            <td $rowspan>$saksnummer ($behandlingID)</td>
        """.trimIndent()
    }

    fun erRedigerbar(): Boolean {
        return erAktiv() && behandlingstatus != Behandlingsstatus.IVERKSETTER_VEDTAK &&
            !(behandlingstatus == Behandlingsstatus.ANMODNING_UNNTAK_SENDT && behandlingstema != Behandlingstema.IKKE_YRKESAKTIV)
    }

    private fun erAktiv(): Boolean {
        return !erInaktiv()
    }

    private fun erInaktiv(): Boolean {
        return erAvsluttet() || erMidlertidigLovvalgsbeslutning()
    }

    private fun erAvsluttet(): Boolean {
        return behandlingstatus == Behandlingsstatus.AVSLUTTET
    }

    private fun erMidlertidigLovvalgsbeslutning(): Boolean {
        return behandlingstatus == Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
    }
}
