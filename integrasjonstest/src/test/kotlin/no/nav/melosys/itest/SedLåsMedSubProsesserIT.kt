package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import mu.KotlinLogging
import no.nav.melosys.Application
import no.nav.melosys.AwaitUtil.throwOnLogError
import no.nav.melosys.LoggingTestUtils
import no.nav.melosys.LoggingTestUtils.filterBuilder
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.saksflyt.ProsessinstansBehandler
import no.nav.melosys.saksflyt.ProsessinstansFerdigListener
import no.nav.melosys.saksflyt.ProsessinstansRepository
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
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.util.*


private val log = KotlinLogging.logger { }

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
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val prosessinstansService: ProsessinstansService,
) : OracleTestContainerBase() {

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
            prosessinstansService.opprettProsessinstansSedMottak(a009)
            prosessinstansService.opprettProsessinstansSedMottak(x008)

            await.throwOnLogError(logItems)
                .timeout(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
                .until {
                    prosessinstansRepository.findAllByStatusNotInAndLåsReferanseStartingWith(
                        listOf(ProsessStatus.FERDIG),
                        rinaSaksnummer
                    ).isEmpty()
                }

            val a009Lås = a009.lagUnikIdentifikator()
            val x0008Lås = x008.lagUnikIdentifikator()

            logItems.filterBuilder
                .match<ProsessinstansService> { it.formattedMessage.contains("Melosys har opprettet prosessinstans") }
                .match<ProsessinstansBehandler> { it.formattedMessage.contains("Utfører steg") }
                .match<ProsessinstansFerdigListener> { it.formattedMessage.contains("på vent med") }
                .match<ProsessinstansFerdigListener> { it.formattedMessage.contains("på vent, neste") }
                .remove(Regex("\\s\\w+-\\w+-\\w+-\\w+-\\w+"))
                .check { next ->
                    next { it shouldBe "Melosys har opprettet prosessinstans null av type MOTTAK_SED." }
                    next { it shouldBe "Melosys har opprettet prosessinstans null av type MOTTAK_SED." }
                    next { it shouldBe "Utfører steg SED_MOTTAK_RUTING for prosessinstans" }
                    next { it shouldBe "Melosys har opprettet prosessinstans null av type MOTTAK_SED_JOURNALFØRING." }
                    next { it shouldBe "Prosessinstanser på vent med samme låsreferanse: [$a009Lås, $x0008Lås]" }
                    next { it shouldBe "2 på vent, neste som kan kjøres er $a009Lås for ferdig låsreferanse $a009Lås" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_FERDIGSTILL_JOURNALPOST for prosessinstans" }
                    next { it shouldBe "Prosessinstanser på vent med samme låsreferanse: [$x0008Lås]" }
                    next { it shouldBe "1 på vent, neste som kan kjøres er $x0008Lås for ferdig låsreferanse $a009Lås" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_RUTING for prosessinstans" }
                    next { it shouldBe "Melosys har opprettet prosessinstans null av type REGISTRERING_UNNTAK_NY_SAK." }
                    next { it shouldBe "Prosessinstanser på vent med samme låsreferanse: [$x0008Lås]" }
                    next { it shouldBe "1 på vent, neste som kan kjøres er $x0008Lås for ferdig låsreferanse $x0008Lås" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH for prosessinstans" }
                    next { it shouldBe "Utfører steg OPPRETT_ARKIVSAK for prosessinstans" }
                    next { it shouldBe "Utfører steg OPPDATER_SAKSRELASJON for prosessinstans" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_FERDIGSTILL_JOURNALPOST for prosessinstans" }
                    next { it shouldBe "Utfører steg OPPRETT_SEDDOKUMENT for prosessinstans" }
                    next { it shouldBe "Utfører steg HENT_REGISTEROPPLYSNINGER for prosessinstans" }
                    next { it shouldBe "Utfører steg REGISTERKONTROLL for prosessinstans" }
                    next { it shouldBe "Utfører steg BESTEM_BEHANDLINGMÅTE_SED for prosessinstans" }
                    next { it shouldBe "Prosessinstanser på vent med samme låsreferanse: []" }
                }
        }
    }


    @TestConfiguration
    class TestConfig(
        @Autowired private val prosessinstansService: ProsessinstansService,
    ) {

        @Bean
        @Primary
        fun opprettSedMottakRutingTest(): SedMottakRuting {
            return mockk<SedMottakRuting>().apply {
                every { inngangsSteg() } answers {
                    log.info { "inngangsSteg - ${ProsessSteg.SED_MOTTAK_RUTING}" }
                    ProsessSteg.SED_MOTTAK_RUTING
                }

                val slot = CapturingSlot<Prosessinstans>()
                every { utfør(capture(slot)) } answers {
                    val låsReferanse = slot.captured.låsReferanse
                    log.info("SedMottakRuting - Utfører for prosess ${slot.captured.id} - $låsReferanse")
                    val parts = låsReferanse.split("_").shouldHaveSize(3).toList()
                    val melosysEessiMelding = MelosysEessiMelding().apply {
                        rinaSaksnummer = parts[0]
                        sedId = parts[1]
                        sedVersjon = parts[2]
                    }
                    when (melosysEessiMelding.sedId) {
                        SedType.A009.name -> prosessinstansService.opprettProsessinstansSedJournalføring(
                            null,
                            melosysEessiMelding
                        )

                        SedType.X008.name -> prosessinstansService.opprettProsessinstansNySakUnntaksregistrering(
                            melosysEessiMelding,
                            null, "test"
                        )
                    }
                }
            }
        }

        @Bean
        @Primary
        fun opprettFerdigstillJournalpostSedTest(): FerdigstillJournalpostSed {
            return mockk<FerdigstillJournalpostSed>().apply {
                every { inngangsSteg() } answers {
                    log.info { "inngangsSteg - ${ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST}" }
                    ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST
                }

                val slot = CapturingSlot<Prosessinstans>()
                every { utfør(capture(slot)) } answers {
                    log.info("FerdigstillJournalpostSed - Utfører for prosess ${slot.captured.id} - ${slot.captured.låsReferanse}")
                }
            }
        }

        @Bean
        @Primary
        fun ferdigstillJournalpostSedTest(): FerdigstillJournalpostSed {
            return mockk<FerdigstillJournalpostSed>().apply {
                every { inngangsSteg() } answers {
                    log.info { "inngangsSteg - ${ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST}" }
                    ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST
                }

                val slot = CapturingSlot<Prosessinstans>()
                every { utfør(capture(slot)) } answers {
                    log.info("FerdigstillJournalpostSed - Utfører for prosess ${slot.captured.id} - ${slot.captured.låsReferanse}")
                }
            }
        }

        @Bean
        @Primary
        fun opprettFagsakOgBehandlingFraSedTest(): OpprettFagsakOgBehandlingFraSed {
            return mockk<OpprettFagsakOgBehandlingFraSed>().apply {
                every { inngangsSteg() } answers {
                    log.info { "inngangsSteg - ${ProsessSteg.SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH}" }
                    ProsessSteg.SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH
                }

                val slot = CapturingSlot<Prosessinstans>()
                every { utfør(capture(slot)) } answers {
                    log.info("OpprettFagsakOgBehandlingFraSed - Utfører for prosess ${slot.captured.id} - ${slot.captured.låsReferanse}")
                }
            }
        }

        @Bean
        @Primary
        fun opprettArkivsakTest(): OpprettArkivsak {
            return mockk<OpprettArkivsak>().apply {
                every { inngangsSteg() } answers {
                    log.info { "inngangsSteg - ${ProsessSteg.OPPRETT_ARKIVSAK}" }
                    ProsessSteg.OPPRETT_ARKIVSAK
                }

                val slot = CapturingSlot<Prosessinstans>()
                every { utfør(capture(slot)) } answers {
                    log.info("OpprettArkivsak - Utfører for prosess ${slot.captured.id} - ${slot.captured.låsReferanse}")
                }
            }
        }

        @Bean
        @Primary
        fun oppdaterSaksrelasjonTest(): OppdaterSaksrelasjon {
            return mockk<OppdaterSaksrelasjon>().apply {
                every { inngangsSteg() } answers {
                    log.info { "inngangsSteg - ${ProsessSteg.OPPDATER_SAKSRELASJON}" }
                    ProsessSteg.OPPDATER_SAKSRELASJON
                }

                val slot = CapturingSlot<Prosessinstans>()
                every { utfør(capture(slot)) } answers {
                    log.info("OppdaterSaksrelasjon - Utfører for prosess ${slot.captured.id} - ${slot.captured.låsReferanse}")
                }
            }
        }

        @Bean
        @Primary
        fun opprettSedDokumentTest(): OpprettSedDokument {
            return mockk<OpprettSedDokument>().apply {
                every { inngangsSteg() } answers {
                    log.info { "inngangsSteg - ${ProsessSteg.OPPRETT_SEDDOKUMENT}" }
                    ProsessSteg.OPPRETT_SEDDOKUMENT
                }

                val slot = CapturingSlot<Prosessinstans>()
                every { utfør(capture(slot)) } answers {
                    log.info("OpprettSedDokument - Utfører for prosess ${slot.captured.id} - ${slot.captured.låsReferanse}")
                }
            }
        }

        @Bean
        @Primary
        fun hentRegisteropplysningerTest(): HentRegisteropplysninger {
            return mockk<HentRegisteropplysninger>().apply {
                every { inngangsSteg() } answers {
                    log.info { "inngangsSteg - ${ProsessSteg.HENT_REGISTEROPPLYSNINGER}" }
                    ProsessSteg.HENT_REGISTEROPPLYSNINGER
                }

                val slot = CapturingSlot<Prosessinstans>()
                every { utfør(capture(slot)) } answers {
                    log.info("HentRegisteropplysninger - Utfører for prosess ${slot.captured.id} - ${slot.captured.låsReferanse}")
                }
            }
        }

        @Bean
        @Primary
        fun registerKontrollTest(): RegisterKontroll {
            return mockk<RegisterKontroll>().apply {
                every { inngangsSteg() } answers {
                    log.info { "inngangsSteg - ${ProsessSteg.REGISTERKONTROLL}" }
                    ProsessSteg.REGISTERKONTROLL
                }

                val slot = CapturingSlot<Prosessinstans>()
                every { utfør(capture(slot)) } answers {
                    log.info("RegisterKontroll - Utfører for prosess ${slot.captured.id} - ${slot.captured.låsReferanse}")
                }
            }
        }

        @Bean
        @Primary
        fun bestemBehandlingsmåteSedTest(): BestemBehandlingsmåteSed {
            return mockk<BestemBehandlingsmåteSed>().apply {
                every { inngangsSteg() } answers {
                    log.info { "inngangsSteg - ${ProsessSteg.BESTEM_BEHANDLINGMÅTE_SED}" }
                    ProsessSteg.BESTEM_BEHANDLINGMÅTE_SED
                }

                val slot = CapturingSlot<Prosessinstans>()
                every { utfør(capture(slot)) } answers {
                    log.info("BestemBehandlingsmåteSed - Utfører for prosess ${slot.captured.id} - ${slot.captured.låsReferanse}")
                }
            }
        }
    }
}
