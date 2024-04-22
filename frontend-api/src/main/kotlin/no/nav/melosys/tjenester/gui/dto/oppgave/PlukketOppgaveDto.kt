package no.nav.melosys.tjenester.gui.dto.oppgave

data class PlukketOppgaveDto (
    val oppgaveID: String? = null,
    val behandlingstype: String? = null,
    val behandlingstema: String? = null,
    val saksnummer: String? = null,
    val journalpostID: String? = null,
    val behandlingID: Long? = null,
    val antallUtildelteOppgaver: Int = 0,
)
