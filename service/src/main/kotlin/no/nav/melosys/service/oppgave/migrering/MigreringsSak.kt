package no.nav.melosys.service.oppgave.migrering

import no.nav.melosys.domain.SakOgBehandlingDTO
import no.nav.melosys.domain.oppgave.Oppgave

data class MigreringsSak(
    val sak: SakOgBehandlingDTO,
    val oppgaver: List<Oppgave>,
    val ny: OppgaveOppdatering,
) {

    fun harFeil(): Boolean = ny.harFeil() || oppgaver.size != 1

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
    private fun Oppgave.htmlTableData(): String {
        return """
            <td  style="background-color:LIGHTGREEN">$oppgavetype</td>
            <td>$behandlingstype</td>
            <td>$behandlingstema</td>
            <td>$tilordnetRessurs</td>
            <td>$opprettetTidspunkt</td>
            <td>$fristFerdigstillelse</td>
        """.trimIndent()
    }
}
