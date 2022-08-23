package no.nav.melosys.service.eessi.jobb

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
    fun feilregistrerX100Journalføringsoppgaver() {
        log.info("Begynner feilregistrering av journalføringsoppgaver opprettet for X100 SED-er.")
        val prosesserFraX100 = prosessinstansRepository.findAllWithSedX100()
        log.info("${prosesserFraX100.size} prosesser opprettet for X100 SED-er funnet.")

        val journalpostIdList =
            prosesserFraX100.map { p: Prosessinstans -> p.getData(ProsessDataKey.JOURNALPOST_ID) }.toList()
        log.info("Det er ${journalpostIdList.size} journalposter for X100 SED-er.")
        val oppgaveSet = journalpostIdList.map { journalpostID: String? ->
            oppgaveService.finnÅpneOppgaverMedJournalpostID(journalpostID)
        }.flatten().toSet()

        if (oppgaveSet.isEmpty()) {
            log.info("Ingen åpne oppgaver for X100 SED-er finnes.")
        } else {
            log.info("${oppgaveSet.size} oppgaver for X100 SED-er skal feilregistreres.")
            oppgaveService.feilregistrerOppgaver(oppgaveSet)
        }
        log.info("Feilregistrering av journalføringsoppgaver opprettet for X100 SED-er er ferdig.")
    }


    fun feilregistrerX100Behandlingsoppgaver() {
        log.info("Begynner feilregistrering av behandlingoppgaver opprettet for X100 SED-er.")
        val oppgaveSet = prosessinstansRepository.findAllWithSedX100()
            .filter { it.behandling != null }
            .map { it.behandling }
            .filter { it.erInaktiv() }
            .map { oppgaveService.finnÅpenOppgaveMedFagsaksnummer(it.fagsak.saksnummer) }
            .filter { it.isPresent }
            .map { it.get() }
            .toSet()

        if (oppgaveSet.isEmpty()) {
            log.info("Ingen åpne behandlingoppgaver for inaktive X100 SED-behandlinger finnes.")
        } else {
            log.info("${oppgaveSet.size} åpne behandlingoppgaver for X100 SED-er skal feilregistreres.")
            oppgaveService.feilregistrerOppgaver(oppgaveSet)
        }
        log.info("Feilregistrering av behandlingsoppgaver opprettet for X100 SED-er er ferdig.")
    }

    companion object {
        private val log = LoggerFactory.getLogger(FeilregistrerX100OppgaverJobb::class.java)
    }
}
