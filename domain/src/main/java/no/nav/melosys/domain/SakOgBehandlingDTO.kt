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
        val rowspan = if (count > 0) "rowspan=${count + 1}" else ""
        return """
            <td $rowspan>$sakstype</td>
            <td $rowspan>${splitLong(sakstema.name)}</td>
            <td $rowspan>${splitLong(behandlingstype.name)}</td>
            <td $rowspan>${splitLong(behandlingstema.name)}</td>
            <td $rowspan>${splitLong(behandlingstatus.name)}</td>
            <td $rowspan title=$behandlingID>$saksnummer</td>
        """.trimIndent()
    }

    fun splitLong(name: String, max: Int = 10): String {
        if (name.length > max)
            return name.split("_").joinToString("</br>")
        return name
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
