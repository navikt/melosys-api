package no.nav.melosys.service.oppgave.migrering

import no.nav.melosys.domain.SakOgBehandlingDTO
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.service.oppgave.OppgaveBehandlingstema

data class MigreringsInfo(
    val sak: SakOgBehandlingDTO,
    val oppgave: MigreringsOppgave,
    val ny: OppgaveOppdatering,
    val ekstraMigreringsOppgaver: List<MigreringsOppgave> = listOf()
) {
    constructor(
        sak: SakOgBehandlingDTO,
        oppgave: Oppgave,
        ny: OppgaveOppdatering,
        ekstraMigreringsOppgaver: List<Oppgave> = listOf()
    ) :
        this(sak, MigreringsOppgave(oppgave), ny, ekstraMigreringsOppgaver.map { MigreringsOppgave(it) })

    fun harFeil(): Boolean = ny.harFeil() || ekstraMigreringsOppgaver.isNotEmpty()

    val oppgaver
        get() = listOf(oppgave) + ekstraMigreringsOppgaver

    fun htmlTableRow(): String {
        val forMangeOppgaverStyle = if (ekstraMigreringsOppgaver.isNotEmpty()) {
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

    fun report(): String {
        val behandlingstema = OppgaveBehandlingstema.values().find { it.kode == oppgave.behandlingstema }
        val sb = StringBuilder()
        sb.appendLine("oppgaveId= ${oppgave.oppgaveId}")
        sb.appendLine("behandlingstype= ${oppgave.behandlingstype}")
        sb.appendLine("opprettetTidspunkt= ${oppgave.opprettetTidspunkt}")
        sb.appendLine("fristFerdigstillelse= ${oppgave.fristFerdigstillelse}")
        sb.appendLine("aktørId= ${oppgave.aktørId}")
        sb.appendLine("=========== gammel ===========")
        sb.appendLine("type= ${oppgave.oppgavetype}")
        sb.appendLine("tema= ${oppgave.tema}")
        sb.appendLine("behandlingstema= ${oppgave.behandlingstema} (${behandlingstema?.name})")
        sb.appendLine("beskrivelse= ${oppgave.beskrivelse}")

        sb.appendLine("=========== ny ===========")
        sb.appendLine("type= ${ny.oppgaveType}")
        sb.appendLine("tema= ${ny.tema}")
        sb.appendLine("behandlingstema= ${ny.oppgaveBehandlingstema?.kode} (${ny.oppgaveBehandlingstema?.name})")
        sb.appendLine("beskrivelse= ${ny.beskrivelse}")
        sb.appendLine("------------------------------")
        sb.appendLine()
        return sb.toString()
    }
}
