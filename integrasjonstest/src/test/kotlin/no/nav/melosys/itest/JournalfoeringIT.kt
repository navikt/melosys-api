package no.nav.melosys.itest

import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.melosysmock.testdata.TestDataGenerator
import no.nav.melosys.repository.ProsessinstansRepository
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

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
        val journalfoeringOpprettDto = defaultJournalføringDto().apply {
            fagsak.sakstype = Sakstyper.EU_EOS.kode
            fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
            behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
            behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
        }


        val prosessinstans = journalførOgVentTilProsesserErFerdige(journalfoeringOpprettDto)


        sjekkBehandlingOgBehandlingsgrunnlag(prosessinstans)
    }

    @Test
    fun journalførOgOpprettSak_EU_EOS_prosesserKjørerAlleSteg() {
        val journalfoeringOpprettDto = defaultJournalføringDto().apply {
            fagsak.sakstype = Sakstyper.EU_EOS.kode
            fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
            behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
            behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
        }


        val journalføringProsessID = journalførOgVentTilProsesserErFerdige(journalfoeringOpprettDto)


        sjekkBehandlingOgBehandlingsgrunnlag(journalføringProsessID)
    }
}
