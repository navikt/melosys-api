package no.nav.melosys.service.oppgave.migrering

import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.service.oppgave.OppgaveBehandlingstema
import no.nav.melosys.service.oppgave.OppgaveBehandlingstype

data class OppgaveOppdatering(
    val oppgaveBehandlingstema: OppgaveBehandlingstema?,
    val oppgaveBehandlingstype: OppgaveBehandlingstype?,
    val tema: Tema?,
    val oppgaveType: Oppgavetyper?,
    val beskrivelse: String?,
    val mappingError: String? = null
) {
    constructor(mappingError: String?) : this(
        null,
        null,
        null,
        null,
        null,
        mappingError = mappingError
    )

    fun harFeil(): Boolean {
        if (mappingError != null) return true
        return beskrivelseInneholderErrorMessage()
    }

    fun htmlTableData(count: Int): String {
        val rowspan = if (count > 0) "rowspan=${count + 1}" else ""
        if (mappingError != null) {
            return """
                    <td colspan=4 $rowspan style="background-color:RED" >${mappingError}</td>
                """.trimIndent()
        }
        return """
            <td $rowspan style="background-color:LIGHTGREEN" title=${oppgaveBehandlingstema?.name}>${oppgaveBehandlingstema?.kode}</td>
            <td $rowspan style="background-color:LIGHTGREEN">$tema</td>
            <td $rowspan style="background-color:LIGHTGREEN">$oppgaveType</td>
            <td $rowspan ${styleBeskrivelse()}>$beskrivelse</td>
        """.trimIndent()
    }

    private fun styleBeskrivelse(): String {
        if (beskrivelseInneholderErrorMessage()) {
            return """style="background-color:RED""""
        }
        return """style="background-color:LIGHTGREEN""""
    }
    private fun beskrivelseInneholderErrorMessage() = beskrivelse?.contains("feilet for") == true
}
