package no.nav.melosys.service.oppgave.migrering

import no.nav.melosys.domain.SakOgBehandlingDTO
import no.nav.melosys.domain.oppgave.Oppgave

data class MigreringsSak(
    val sak: SakOgBehandlingDTO,
    val oppgaver: MutableList<Oppgave>,
    val ny: OppgaveMigreringsOppdatering,
) {

    fun erOppgaveMigrert(): Boolean {
        if (oppgaver.size != 1) return false
        val oppgave = oppgaver.first()

        return ny.oppgaveBehandlingstema?.kode == oppgave.behandlingstema &&
            ny.tema == oppgave.tema &&
            ny.oppgaveType == oppgave.oppgavetype
    }
}
