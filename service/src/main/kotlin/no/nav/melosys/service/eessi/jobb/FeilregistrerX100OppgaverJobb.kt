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
            prosesserFraX100.map { p: Prosessinstans -> p.getData(ProsessDataKey.JOURNALPOST_ID) }.toList()
        log.info("Det er {} journalposter for X100 SED-er.", journalpostIdList.size)
        val oppgaveIdSet = journalpostIdList.map { journalpostID: String? ->
            oppgaveService.finnÅpneOppgaverMedJournalpostID(journalpostID)
        }.flatten().map { obj: Oppgave -> obj.oppgaveId }.toSet()

        when {
            oppgaveIdSet.isEmpty() -> log.info("Ingen åpne oppgaver for X100 SED-er finnes.")

            else -> {
                log.info("{} oppgaver for X100 SED-er skal feilregistreres.", oppgaveIdSet.size)
                oppgaveService.feilregistrerOppgave(oppgaveIdSet)
            }
        }
        log.info("Feilregistrering av oppgaver opprettet for X100 SED-er er ferdig.")
    }

    companion object {
        private val log = LoggerFactory.getLogger(FeilregistrerX100OppgaverJobb::class.java)
    }
}
