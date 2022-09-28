package no.nav.melosys.itest

import no.finn.unleash.FakeUnleash
import no.nav.melosys.melosysmock.testdata.TestDataGenerator
import no.nav.melosys.repository.ProsessinstansRepository
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class JournalfoeringIT(
    @Autowired testDataGenerator: TestDataGenerator,
    @Autowired journalføringService: JournalfoeringService,
    @Autowired oppgaveService: OppgaveService,
    @Autowired prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val unleash: FakeUnleash
) : JournalfoeringBase(testDataGenerator, journalføringService, oppgaveService, prosessinstansRepository) {

    @Test
    fun journalførOgOpprettSakMedToggleBehandleAlleSaker_EU_EOS_prosesserKjørerAlleSteg() {
        unleash.enable("melosys.behandle_alle_saker")

        val startTime = LocalDateTime.now()
        val journalfoeringOpprettDto = lagJournalfoeringOpprettDto()

        journalførOgOpprettSak(journalfoeringOpprettDto)

        val journalføringProsessID = waitForProsesses(startTime)
        sjekkBehandlingOgBehandlingsgrunnlag(journalføringProsessID)
    }

    @Test
    fun journalførOgOpprettSak_EU_EOS_prosesserKjørerAlleSteg() {
        val startTime = LocalDateTime.now()
        val journalfoeringOpprettDto = lagJournalfoeringOpprettDto()

        journalførOgOpprettSak(journalfoeringOpprettDto)

        val journalføringProsessID = waitForProsesses(startTime)
        sjekkBehandlingOgBehandlingsgrunnlag(journalføringProsessID)
    }
}
