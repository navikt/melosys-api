package no.nav.melosys.service.eessi.jobb

import no.nav.melosys.domain.saksflyt.ProsessDataKey
import no.nav.melosys.repository.ProsessinstansRepository
import no.nav.melosys.service.journalforing.JournalfoeringService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class JournalfoerX100JournalposterJobb(
    private val prosessinstansRepository: ProsessinstansRepository,
    private val journalføringService: JournalfoeringService,
) {
    fun journalfoerX100Journalposter() {
        log.info("Begynner journalføring av journalposter opprettet for X100 SED-er.")

        val åpneJournalposterList = prosessinstansRepository.findAllWithSedX100().filter { it.behandling != null }
            .map { Pair(it.behandling, it.getData(ProsessDataKey.JOURNALPOST_ID)) }
            .distinct()
            .map { Pair(it.first, journalføringService.hentJournalpost(it.second)) }
            .filter { !it.second.isErFerdigstilt }
            .map {org.springframework.data.util.Pair.of(it.first, it.second)}
            .toList()
        log.info("Det er {} åpne journalposter for X100 SED-er som skal journalføres.", åpneJournalposterList.size)

        journalføringService.journalførOgKnyttTilEksisterendeSak(åpneJournalposterList)
    }


    companion object {
        private val log = LoggerFactory.getLogger(JournalfoerX100JournalposterJobb::class.java)
    }
}
