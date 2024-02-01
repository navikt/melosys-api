package no.nav.melosys.tjenester.gui.dto.oppgave

class PlukketOppgaveDto {
    @JvmField
    var oppgaveID: String? = null
    @JvmField
    var behandlingstype: String? = null
    @JvmField
    var behandlingstema: String? = null
    @JvmField
    var saksnummer: String? = null
    @JvmField
    var journalpostID: String? = null
    @JvmField
    var behandlingID: Long? = null
    @JvmField
    var antallUtildelteOppgaver: Int = 0
}
