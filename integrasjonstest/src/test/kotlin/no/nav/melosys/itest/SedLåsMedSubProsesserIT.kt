package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.Application
import no.nav.melosys.LoggingTestUtils
import no.nav.melosys.LoggingTestUtils.filterBuilder
import no.nav.melosys.ProsessRegister
import no.nav.melosys.ProsessUtil
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.saksflyt.ProsessinstansBehandler
import no.nav.melosys.saksflyt.ProsessinstansFerdigListener
import no.nav.melosys.saksflyt.steg.behandling.OpprettFagsakOgBehandlingFraSed
import no.nav.melosys.saksflyt.steg.jfr.FerdigstillJournalpostSed
import no.nav.melosys.saksflyt.steg.jfr.OpprettArkivsak
import no.nav.melosys.saksflyt.steg.register.HentRegisteropplysninger
import no.nav.melosys.saksflyt.steg.register.RegisterKontroll
import no.nav.melosys.saksflyt.steg.sed.BestemBehandlingsmåteSed
import no.nav.melosys.saksflyt.steg.sed.OppdaterSaksrelasjon
import no.nav.melosys.saksflyt.steg.sed.OpprettSedDokument
import no.nav.melosys.saksflyt.steg.sed.mottak.SedMottakRuting
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.util.*

@ActiveProfiles("test")
@SpringBootTest(
    classes = [Application::class, SaksflytTestConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["teammelosys.eessi.v1-local", "teammelosys.soknad-mottak.v1-local", "teammelosys.melosys-utstedt-a1.v1-local", "teammelosys.fattetvedtak.v1-local"],
    brokerProperties = ["offsets.topic.replication.factor=1", "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
@EnableMockOAuth2Server
@Import(SedLåsMedSubProsesserIT.TestConfig::class)
internal class SedLåsMedSubProsesserIT(
    @Autowired private val prosessinstansService: ProsessinstansService,
    @Autowired private val prosessRegister: ProsessRegister,
    @Autowired private val prosessUtil: ProsessUtil
) : OracleTestContainerBase() {

    @AfterEach
    fun setUp() {
        prosessRegister.clear()
        prosessUtil.clear()
    }

    @Test
    fun `ved lås må sub-prosesser fra første sed kjøres før neste sed blir kjørt`() {
        val rinaSaksnummer = Random().nextInt(100000).toString()

        val a009 = MelosysEessiMelding().apply {
            this.rinaSaksnummer = rinaSaksnummer
            sedId = SedType.A009.name
            sedVersjon = "1"
        }
        val x008 = MelosysEessiMelding().apply {
            this.rinaSaksnummer = rinaSaksnummer
            sedId = SedType.X008.name
            sedVersjon = "1"
        }

        LoggingTestUtils.withLogCapture { logItems ->

            prosessUtil.executeAndWait(
                waitForprosessType = ProsessType.MOTTAK_SED,
                alsoWaitForprosessType = listOf(ProsessType.MOTTAK_SED_JOURNALFØRING, ProsessType.REGISTRERING_UNNTAK_NY_SAK),
                waitForProcessCount = 5
            ) {
                prosessRegister.registrer("a009Prosess") { prosessinstansService.opprettProsessinstansSedMottak(a009) }
                prosessRegister.registrer("x008Prosess") { prosessinstansService.opprettProsessinstansSedMottak(x008) }
            }

            val a009Lås = a009.lagUnikIdentifikator()
            val x0008Lås = x008.lagUnikIdentifikator()

            logItems.filterBuilder
                .match<ProsessinstansService> { it.formattedMessage.contains("Melosys har opprettet prosessinstans") }
                .match<ProsessinstansBehandler> { it.formattedMessage.contains("Utfører steg") }
                .match<ProsessinstansFerdigListener>()
                .replace(prosessRegister.prosessIdStringToName())
                .replace(a009Lås, "<a009Lås>")
                .replace(x0008Lås, "<x0008Lås>")
                .sort(Regex("gruppe-prefiks: \\[(.*?)]"))
                .check { next ->
                    next { it shouldBe "Melosys har opprettet prosessinstans <a009Prosess> av type MOTTAK_SED." }
                    next { it shouldBe "Melosys har opprettet prosessinstans <x008Prosess> av type MOTTAK_SED." }
                    next { it shouldBe "Utfører steg SED_MOTTAK_RUTING for prosessinstans <a009Prosess>" }
                    next { it shouldBe "Melosys har opprettet prosessinstans <sub-prosess-1 av a009Prosess> av type MOTTAK_SED_JOURNALFØRING." }
                    next { it shouldBe "Melosys har opprettet prosessinstans <sub-prosess-2 av a009Prosess> av type MOTTAK_SED_JOURNALFØRING." }
                    next { it shouldBe "Prosessinstans <a009Prosess> ferdig, sjekker om neste med låsreferanse:<a009Lås> kan startes" }
                    next { it shouldBe "Prosessinstans(er) på vent med samme gruppe-prefiks: [<sub-prosess-1 av a009Prosess>, <sub-prosess-2 av a009Prosess>, <x008Prosess>]" }
                    next { it shouldBe "Prosessinstans <sub-prosess-1 av a009Prosess> med låsreferanse <a009Lås> startes opp etter å ha vært på vent" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_FERDIGSTILL_JOURNALPOST for prosessinstans <sub-prosess-1 av a009Prosess>" }
                    next { it shouldBe "Prosessinstans <sub-prosess-1 av a009Prosess> ferdig, sjekker om neste med låsreferanse:<a009Lås> kan startes" }
                    next { it shouldBe "Prosessinstans(er) på vent med samme gruppe-prefiks: [<sub-prosess-2 av a009Prosess>, <x008Prosess>]" }
                    next { it shouldBe "Prosessinstans <sub-prosess-2 av a009Prosess> med låsreferanse <a009Lås> startes opp etter å ha vært på vent" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_FERDIGSTILL_JOURNALPOST for prosessinstans <sub-prosess-2 av a009Prosess>" }
                    next { it shouldBe "Prosessinstans <sub-prosess-2 av a009Prosess> ferdig, sjekker om neste med låsreferanse:<a009Lås> kan startes" }
                    next { it shouldBe "Prosessinstans(er) på vent med samme gruppe-prefiks: [<x008Prosess>]" }
                    next { it shouldBe "Prosessinstans <x008Prosess> med låsreferanse <x0008Lås> startes opp etter å ha vært på vent" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_RUTING for prosessinstans <x008Prosess>" }
                    next { it shouldBe "Melosys har opprettet prosessinstans <sub-prosess av x008Prosess> av type REGISTRERING_UNNTAK_NY_SAK." }
                    next { it shouldBe "Prosessinstans <x008Prosess> ferdig, sjekker om neste med låsreferanse:<x0008Lås> kan startes" }
                    next { it shouldBe "Prosessinstans(er) på vent med samme gruppe-prefiks: [<sub-prosess av x008Prosess>]" }
                    next { it shouldBe "Prosessinstans <sub-prosess av x008Prosess> med låsreferanse <x0008Lås> startes opp etter å ha vært på vent" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH for prosessinstans <sub-prosess av x008Prosess>" }
                    next { it shouldBe "Utfører steg OPPRETT_ARKIVSAK for prosessinstans <sub-prosess av x008Prosess>" }
                    next { it shouldBe "Utfører steg OPPDATER_SAKSRELASJON for prosessinstans <sub-prosess av x008Prosess>" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_FERDIGSTILL_JOURNALPOST for prosessinstans <sub-prosess av x008Prosess>" }
                    next { it shouldBe "Utfører steg OPPRETT_SEDDOKUMENT for prosessinstans <sub-prosess av x008Prosess>" }
                    next { it shouldBe "Utfører steg HENT_REGISTEROPPLYSNINGER for prosessinstans <sub-prosess av x008Prosess>" }
                    next { it shouldBe "Utfører steg REGISTERKONTROLL for prosessinstans <sub-prosess av x008Prosess>" }
                    next { it shouldBe "Utfører steg BESTEM_BEHANDLINGMÅTE_SED for prosessinstans <sub-prosess av x008Prosess>" }
                    next { it shouldBe "Prosessinstans <sub-prosess av x008Prosess> ferdig, sjekker om neste med låsreferanse:<x0008Lås> kan startes" }
                    next { it shouldBe "Prosessinstans(er) på vent med samme gruppe-prefiks: []" }
                }
        }
    }

    @Test
    fun `ved samme låsreferanse må også sub-prossesser kjøres riktig`() {
        val rinaSaksnummer = Random().nextInt(100000).toString()

        val a009 = MelosysEessiMelding().apply {
            this.rinaSaksnummer = rinaSaksnummer
            sedId = SedType.A009.name
            sedVersjon = "1"
        }
        val a009Lås = a009.lagUnikIdentifikator()

        LoggingTestUtils.withLogCapture { logItems ->

            prosessUtil.executeAndWait(
                waitForprosessType = ProsessType.MOTTAK_SED,
                alsoWaitForprosessType = listOf(ProsessType.MOTTAK_SED_JOURNALFØRING),
                waitForProcessCount = 6
            ) {
                prosessRegister.registrer("førsteProsess") { prosessinstansService.opprettProsessinstansSedMottak(a009) }
                prosessRegister.registrer("duplikatProsess") { prosessinstansService.opprettProsessinstansSedMottak(a009) }
            }

            logItems.filterBuilder
                .match<ProsessinstansService> { it.formattedMessage.contains("Melosys har opprettet prosessinstans") }
                .match<ProsessinstansBehandler> { it.formattedMessage.contains("Utfører steg") }
                .match<ProsessinstansFerdigListener>()
                .replace(prosessRegister.prosessIdStringToName())
                .replace(a009Lås, "<a009Lås>")
                .sort(Regex("gruppe-prefiks: \\[(.*?)]"))
                .check { next ->
                    next { it shouldBe "Melosys har opprettet prosessinstans <førsteProsess> av type MOTTAK_SED." }
                    next { it shouldBe "Melosys har opprettet prosessinstans <duplikatProsess> av type MOTTAK_SED." }
                    next { it shouldBe "Utfører steg SED_MOTTAK_RUTING for prosessinstans <førsteProsess>" }
                    next { it shouldBe "Melosys har opprettet prosessinstans <sub-prosess-1 av førsteProsess> av type MOTTAK_SED_JOURNALFØRING." }
                    next { it shouldBe "Melosys har opprettet prosessinstans <sub-prosess-2 av førsteProsess> av type MOTTAK_SED_JOURNALFØRING." }
                    next { it shouldBe "Prosessinstans <førsteProsess> ferdig, sjekker om neste med låsreferanse:<a009Lås> kan startes" }
                    next { it shouldBe "Prosessinstans(er) på vent med samme gruppe-prefiks: [<duplikatProsess>, <sub-prosess-1 av førsteProsess>, <sub-prosess-2 av førsteProsess>]" }
                    next { it shouldBe "Prosessinstans <sub-prosess-1 av førsteProsess> med låsreferanse <a009Lås> startes opp etter å ha vært på vent" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_FERDIGSTILL_JOURNALPOST for prosessinstans <sub-prosess-1 av førsteProsess>" }
                    next { it shouldBe "Prosessinstans <sub-prosess-1 av førsteProsess> ferdig, sjekker om neste med låsreferanse:<a009Lås> kan startes" }
                    next { it shouldBe "Prosessinstans(er) på vent med samme gruppe-prefiks: [<duplikatProsess>, <sub-prosess-2 av førsteProsess>]" }
                    next { it shouldBe "Prosessinstans <sub-prosess-2 av førsteProsess> med låsreferanse <a009Lås> startes opp etter å ha vært på vent" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_FERDIGSTILL_JOURNALPOST for prosessinstans <sub-prosess-2 av førsteProsess>" }
                    next { it shouldBe "Prosessinstans <sub-prosess-2 av førsteProsess> ferdig, sjekker om neste med låsreferanse:<a009Lås> kan startes" }
                    next { it shouldBe "Prosessinstans(er) på vent med samme gruppe-prefiks: [<duplikatProsess>]" }
                    next { it shouldBe "Prosessinstans <duplikatProsess> med låsreferanse <a009Lås> startes opp etter å ha vært på vent" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_RUTING for prosessinstans <duplikatProsess>" }
                    next { it shouldBe "Melosys har opprettet prosessinstans <sub-prosess-1 av duplikatProsess> av type MOTTAK_SED_JOURNALFØRING." }
                    next { it shouldBe "Melosys har opprettet prosessinstans <sub-prosess-2 av duplikatProsess> av type MOTTAK_SED_JOURNALFØRING." }
                    next { it shouldBe "Prosessinstans <duplikatProsess> ferdig, sjekker om neste med låsreferanse:<a009Lås> kan startes" }
                    next { it shouldBe "Prosessinstans(er) på vent med samme gruppe-prefiks: [<sub-prosess-1 av duplikatProsess>, <sub-prosess-2 av duplikatProsess>]" }
                    next { it shouldBe "Prosessinstans <sub-prosess-1 av duplikatProsess> med låsreferanse <a009Lås> startes opp etter å ha vært på vent" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_FERDIGSTILL_JOURNALPOST for prosessinstans <sub-prosess-1 av duplikatProsess>" }
                    next { it shouldBe "Prosessinstans <sub-prosess-1 av duplikatProsess> ferdig, sjekker om neste med låsreferanse:<a009Lås> kan startes" }
                    next { it shouldBe "Prosessinstans(er) på vent med samme gruppe-prefiks: [<sub-prosess-2 av duplikatProsess>]" }
                    next { it shouldBe "Prosessinstans <sub-prosess-2 av duplikatProsess> med låsreferanse <a009Lås> startes opp etter å ha vært på vent" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_FERDIGSTILL_JOURNALPOST for prosessinstans <sub-prosess-2 av duplikatProsess>" }
                    next { it shouldBe "Prosessinstans <sub-prosess-2 av duplikatProsess> ferdig, sjekker om neste med låsreferanse:<a009Lås> kan startes" }
                    next { it shouldBe "Prosessinstans(er) på vent med samme gruppe-prefiks: []" }
                }
        }
    }

    @TestConfiguration
    class TestConfig(
        @Autowired private val prosessinstansService: ProsessinstansService,
        @Autowired private val prosessRegister: ProsessRegister
    ) {
        @Bean
        fun opprettSedMottakRutingTest(): SedMottakRuting = mockk<SedMottakRuting>().apply {
            every { inngangsSteg() } returns ProsessSteg.SED_MOTTAK_RUTING

            val slot = CapturingSlot<Prosessinstans>()
            every { utfør(capture(slot)) } answers {
                val parentProsess = slot.captured
                val parentNavn = prosessRegister.nameFromId(parentProsess.id) ?: throw IllegalStateException("Fant ikke navn for ${parentProsess.id}")
                val låsReferanse = parentProsess.låsReferanse
                val parts = låsReferanse.split("_").shouldHaveSize(3).toList()
                val melosysEessiMelding = MelosysEessiMelding().apply {
                    rinaSaksnummer = parts[0]
                    sedId = parts[1]
                    sedVersjon = parts[2]
                }
                when (melosysEessiMelding.sedId) {
                    SedType.A009.name -> {
                        prosessRegister.registrer("sub-prosess-1 av $parentNavn") {
                            prosessinstansService.opprettProsessinstansSedJournalføring(null, melosysEessiMelding)
                        }
                        prosessRegister.registrer("sub-prosess-2 av $parentNavn") {
                            prosessinstansService.opprettProsessinstansSedJournalføring(null, melosysEessiMelding)
                        }
                    }

                    SedType.X008.name -> prosessRegister.registrer("sub-prosess av $parentNavn") {
                        prosessinstansService.opprettProsessinstansNySakUnntaksregistrering(melosysEessiMelding, null, "test")
                    }
                }
            }
        }

        @Bean
        fun opprettFerdigstillJournalpostSedTest(): FerdigstillJournalpostSed {
            return mockk<FerdigstillJournalpostSed>().apply {
                every { inngangsSteg() } returns ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST
                every { utfør(any()) } returns Unit
            }
        }

        @Bean
        fun opprettFagsakOgBehandlingFraSedTest(): OpprettFagsakOgBehandlingFraSed {
            return mockk<OpprettFagsakOgBehandlingFraSed>().apply {
                every { inngangsSteg() } returns ProsessSteg.SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH
                every { utfør(any()) } returns Unit
            }
        }

        @Bean
        fun opprettArkivsakTest(): OpprettArkivsak {
            return mockk<OpprettArkivsak>().apply {
                every { inngangsSteg() } returns ProsessSteg.OPPRETT_ARKIVSAK
                every { utfør(any()) } returns Unit
            }
        }

        @Bean
        fun oppdaterSaksrelasjonTest(): OppdaterSaksrelasjon {
            return mockk<OppdaterSaksrelasjon>().apply {
                every { inngangsSteg() } returns ProsessSteg.OPPDATER_SAKSRELASJON
                every { utfør(any()) } returns Unit
            }
        }

        @Bean
        fun opprettSedDokumentTest(): OpprettSedDokument {
            return mockk<OpprettSedDokument>().apply {
                every { inngangsSteg() } returns ProsessSteg.OPPRETT_SEDDOKUMENT
                every { utfør(any()) } returns Unit
            }
        }

        @Bean
        fun hentRegisteropplysningerTest(): HentRegisteropplysninger {
            return mockk<HentRegisteropplysninger>().apply {
                every { inngangsSteg() } returns ProsessSteg.HENT_REGISTEROPPLYSNINGER
                every { utfør(any()) } returns Unit
            }
        }

        @Bean
        fun registerKontrollTest(): RegisterKontroll {
            return mockk<RegisterKontroll>().apply {
                every { inngangsSteg() } returns ProsessSteg.REGISTERKONTROLL
                every { utfør(any()) } returns Unit
            }
        }

        @Bean
        fun bestemBehandlingsmåteSedTest(): BestemBehandlingsmåteSed {
            return mockk<BestemBehandlingsmåteSed>().apply {
                every { inngangsSteg() } returns ProsessSteg.BESTEM_BEHANDLINGMÅTE_SED
                every { utfør(any()) } returns Unit
            }
        }
    }
}
