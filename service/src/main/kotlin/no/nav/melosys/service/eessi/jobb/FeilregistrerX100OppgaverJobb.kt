package no.nav.melosys.service.eessi.jobb

import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.domain.saksflyt.ProsessDataKey
import no.nav.melosys.domain.saksflyt.Prosessinstans
import no.nav.melosys.repository.ProsessinstansRepository
import no.nav.melosys.service.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FeilregistrerX100OppgaverJobb(
    private val prosessinstansRepository: ProsessinstansRepository,
    private val oppgaveService: OppgaveService
) {
    fun feilregistrerX100Oppgaver() {
        log.info("Begynner automatisk feilregistrering av oppgaver opprettet for X100 SED-er.")
        val prosesserFraX100 = prosessinstansRepository.findAllWithSedX100()
        log.info("{} prosesser opprettet for X100 SED-er funnet.", prosesserFraX100.size)

        val journalpostIdList =
            prosesserFraX100.stream().map { p: Prosessinstans -> p.getData(ProsessDataKey.JOURNALPOST_ID) }.toList()
        log.info("{} journalposter for X100 SED-er.", journalpostIdList.size)
        val oppgaverFraX100 = journalpostIdList.stream()
            .map { journalpostID: String? -> oppgaveService.finnÅpneOppgaverMedJournalpostID(journalpostID) }
            .flatMap { obj: List<Oppgave> -> obj.stream() }.toList()
        log.info("{} oppgaver for X100 SED-er skal feilregistreres.", oppgaverFraX100.size)

        oppgaverFraX100.stream().map { obj: Oppgave -> obj.oppgaveId }
            .forEach { oppgaveID: String? -> oppgaveService.feilregistrerOppgave(oppgaveID) }
        log.info("Feilregistrering av oppgaver opprettet for X100 SED-er er ferdig.")
    }

    companion object {
        private val log = LoggerFactory.getLogger(FeilregistrerX100OppgaverJobb::class.java)
    }
}
