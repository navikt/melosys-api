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

    fun htmlTableRow(): String {
        val forMangeOppgaverStyle = if (oppgaver.size > 1) {
            "style=\"background-color:YELLOW\""
        } else ""

        return """
                <tr ${forMangeOppgaverStyle}>
                    ${sak.htmlTableData(oppgaver.size)}
                    ${ny.htmlTableData(oppgaver.size)}
                </tr>
                """.trimIndent() +
            oppgaver.joinToString("\n") {
                """<tr>${it.htmlTableData()}</tr>""".trimIndent()
            }
    }

    private fun OppgaveOppdatering.htmlTableData(count: Int): String {
        val rowspan = if (count > 0) "rowspan=${count + 1}" else ""
        if (mappingError != null) {
            return """<td colspan=4 $rowspan style="background-color:RED" >${mappingError}</td>"""
        }
        val teamBackGroundColor = if (teamErForsjellig()) "RED" else "LIGHTGREEN"
        val oppgavetypeBackGroundColor = if (oppgavetypeErForsjellig()) "RED" else "LIGHTGREEN"
        return """
            <td $rowspan style="background-color:LIGHTGREEN" title=${oppgaveBehandlingstema?.name}>${oppgaveBehandlingstema?.kode}</td>
            <td $rowspan style="background-color:$teamBackGroundColor">$tema</td>
            <td $rowspan style="background-color:$oppgavetypeBackGroundColor">$oppgaveType</td>
            <td $rowspan ${styleBeskrivelse()}>$beskrivelse</td>
        """.trimIndent()
    }

    private fun Oppgave.htmlTableData(): String {
        val teamBackGroundColor = if (teamErForsjellig()) "RED" else "LIGHTBLUE"
        val oppgavetypeBackGroundColor = if (oppgavetypeErForsjellig()) "RED" else "LIGHTBLUE"
        return """
            <td style="background-color:LIGHTBLUE">$behandlingstema</td>
            <td style="background-color:$teamBackGroundColor">${tema.kode}</td>
            <td style="background-color:$oppgavetypeBackGroundColor">${oppgavetype.kode}</td>
            <td style="background-color:LIGHTBLUE">$behandlingstype</td>
            <td style="background-color:LIGHTBLUE">$beskrivelse</td>
            <td style="background-color:LIGHTBLUE">$tilordnetRessurs</td>
            <td style="background-color:LIGHTBLUE">$opprettetTidspunkt</td>
            <td style="background-color:LIGHTBLUE">$fristFerdigstillelse</td>
            <td style="background-color:LIGHTBLUE">$</td>
            """
    }
}
