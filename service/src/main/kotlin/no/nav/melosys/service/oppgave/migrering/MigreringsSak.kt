package no.nav.melosys.service.oppgave.migrering

import no.nav.melosys.domain.SakOgBehandlingDTO
import no.nav.melosys.domain.oppgave.Oppgave

data class MigreringsSak(
    val sak: SakOgBehandlingDTO,
    val oppgaver: List<Oppgave>,
    val ny: OppgaveOppdatering,
) {

    fun harFeil(): Boolean = ny.harFeil() || oppgaver.size != 1
    fun teamErForsjellig(): Boolean = oppgaver.any { it.tema != ny.tema }
    fun oppgavetypeErForsjellig(): Boolean = oppgaver.any { it.oppgavetype.kode != ny.oppgaveType?.kode }

    private val sakStyle = if (oppgaver.size > 1) "class=\"sakfeil\"" else "class=\"sak\""
    fun htmlTableRow(): String {
        return """
            <tr>
                ${sak.htmlTableData(oppgaver.size)}
                ${ny.htmlTableData(oppgaver.size)}
            </tr>
            """.trimIndent() +
            oppgaver.joinToString("\n") {
                """<tr>${it.htmlTableData()}</tr>""".trimIndent()
            }
    }

    private fun SakOgBehandlingDTO.htmlTableData(count: Int): String {
        val rowspan = if (count > 0) "rowspan=${count + 1}" else ""
        return """
            <td $sakStyle $rowspan>$sakstype</td>
            <td $sakStyle $rowspan>${splitLong(sakstema.name)}</td>
            <td $sakStyle $rowspan>${splitLong(behandlingstype.name)}</td>
            <td $sakStyle $rowspan>${splitLong(behandlingstema.name)}</td>
            <td $sakStyle $rowspan>${splitLong(behandlingstatus.name)}</td>
            <td $sakStyle $rowspan title=$behandlingID>$saksnummer</td>
        """.trimIndent()
    }

    private fun OppgaveOppdatering.htmlTableData(count: Int): String {
        val rowspan = if (count > 0) "rowspan=${count + 1}" else ""
        if (mappingError != null) {
            return """<td colspan=4 $rowspan class="feil" >$mappingError</td>"""
        }
        val teamBackClass = if (teamErForsjellig()) "forskjell" else "ny"
        val oppgavetypeClass = if (oppgavetypeErForsjellig()) "forskjell" else "ny"
        val beskrivelseClass = if (beskrivelseInneholderErrorMessage()) "feil" else "ny"

        return """
            <td $rowspan class="ny" title=${oppgaveBehandlingstema?.name}>${oppgaveBehandlingstema?.kode}</td>
            <td $rowspan class="$teamBackClass">$tema</td>
            <td $rowspan class="$oppgavetypeClass">$oppgaveType</td>
            <td $rowspan class="$beskrivelseClass">$beskrivelse</td>
        """.trimIndent()
    }

    private fun Oppgave.htmlTableData(): String {
        val teamClass = if (teamErForsjellig()) "forskjell" else "oppgave"
        val oppgavetypeClass = if (oppgavetypeErForsjellig()) "forskjell" else "oppgave"
        return """
            <td class="oppgave">$behandlingstema</td>
            <td class="$teamClass">${tema.kode}</td>
            <td class="$oppgavetypeClass">${oppgavetype.kode}</td>
            <td class="oppgave">$behandlingstype</td>
            <td class="oppgave">$beskrivelse</td>
            <td class="oppgave">$tilordnetRessurs</td>
            <td class="oppgave">$opprettetTidspunkt</td>
            <td class="oppgave">$fristFerdigstillelse</td>
            """
    }
    private fun splitLong(name: String, max: Int = 10): String {
        if (name.length > max)
            return name.split("_").joinToString("</br>")
        return name
    }

}
