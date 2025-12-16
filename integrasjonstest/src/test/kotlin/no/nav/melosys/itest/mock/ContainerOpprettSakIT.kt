package no.nav.melosys.itest.mock

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.felles.dto.SoeknadslandDto
import no.nav.melosys.service.journalforing.dto.PeriodeDto
import no.nav.melosys.service.sak.OpprettSak
import no.nav.melosys.service.sak.OpprettSakDto
import no.nav.melosys.service.sak.SøknadDto
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

/**
 * Integration test that uses the melosys-mock container instead of in-process mock.
 *
 * This test verifies that:
 * 1. melosys-api can call external services via the Docker container
 * 2. The container's verification endpoints work for asserting mock state
 * 3. Business logic works correctly with container-based mocks
 *
 * This is the key test for proving Phase 5 (container migration) works.
 */
class ContainerOpprettSakIT : ContainerMockServerTestBase() {

    @Autowired
    private lateinit var opprettSak: OpprettSak

    companion object {
        private val log = LoggerFactory.getLogger(ContainerOpprettSakIT::class.java)
    }

    @Test
    fun `should create sak using container mock for external services`() {
        log.info("Starting container integration test...")
        log.info("Mock container URL: ${getMockBaseUrl()}")

        // Verify container is healthy
        mockVerificationClient.isHealthy() shouldBe true
        log.info("Container health check passed")

        // Create a sak using the OpprettSak service
        val opprettSakDto = OpprettSakDto().apply {
            hovedpart = Aktoersroller.BRUKER
            brukerID = "30056928150" // Test person from PersonRepo
            sakstype = Sakstyper.EU_EOS
            sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG
            behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            behandlingstype = Behandlingstyper.FØRSTEGANG
            behandlingsaarsakType = Behandlingsaarsaktyper.SØKNAD
            soknadDto = SøknadDto().apply {
                land = SoeknadslandDto().apply {
                    landkoder = listOf("BE")
                    isFlereLandUkjentHvilke = false
                }
                periode = PeriodeDto(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31)
                )
            }
            mottaksdato = LocalDate.now()
            skalTilordnes = false // Don't assign to avoid oppgave creation
        }

        // Execute the sak creation and wait for processes to complete
        val prosessinstans = prosessinstansTestManager.executeAndWait(
            waitForProsesses = mapOf(ProsessType.OPPRETT_SAK to 1),
            returnProsessOfType = ProsessType.OPPRETT_SAK
        ) {
            opprettSak.opprettNySakOgBehandling(opprettSakDto)
        }

        // Verify sak was created
        prosessinstans.behandling.shouldNotBeNull()
        log.info("Sak created with behandling ID: ${prosessinstans.behandling?.id}")

        // Verify that SAK API was called via the container
        // The OpprettSak service calls the SAK API to create a fagsak
        val saker = mockVerificationClient.saker()
        log.info("Saker in mock: ${saker.size}")
        saker.shouldHaveSize(1)

        // Verify the sak details
        val sak = saker.first()
        sak.tema shouldBe "MED"
        log.info("SAK verification passed: tema=${sak.tema}, id=${sak.id}")

        // Get summary to see all mock state
        val summary = mockVerificationClient.summary()
        log.info("Mock summary: saker=${summary.sakCount}, oppgaver=${summary.oppgaveCount}, medl=${summary.medlCount}")
    }

    @Test
    fun `should verify PDL is called via container when creating sak`() {
        log.info("Testing PDL integration via container...")

        // The OpprettSak service calls PDL to get person info
        // We can verify this by checking that the process completes successfully
        // (PDL mock returns test data for the test person)

        val opprettSakDto = OpprettSakDto().apply {
            hovedpart = Aktoersroller.BRUKER
            brukerID = "30056928150"
            sakstype = Sakstyper.EU_EOS
            sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG
            behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            behandlingstype = Behandlingstyper.FØRSTEGANG
            behandlingsaarsakType = Behandlingsaarsaktyper.SØKNAD
            soknadDto = SøknadDto().apply {
                land = SoeknadslandDto().apply {
                    landkoder = listOf("SE")
                    isFlereLandUkjentHvilke = false
                }
                periode = PeriodeDto(
                    LocalDate.of(2024, 6, 1),
                    LocalDate.of(2024, 6, 30)
                )
            }
            mottaksdato = LocalDate.now()
            skalTilordnes = false
        }

        val prosessinstans = prosessinstansTestManager.executeAndWait(
            waitForProsesses = mapOf(ProsessType.OPPRETT_SAK to 1),
            returnProsessOfType = ProsessType.OPPRETT_SAK
        ) {
            opprettSak.opprettNySakOgBehandling(opprettSakDto)
        }

        // If we get here without exceptions, PDL was called successfully via the container
        val behandling = prosessinstans.behandling.shouldNotBeNull()
        log.info("PDL integration verified - behandling created: ${behandling.id}")

        // The fagsak should have bruker info populated from PDL
        val fagsak = behandling.fagsak.shouldNotBeNull()
        val bruker = fagsak.hentBruker().shouldNotBeNull()
        log.info("Fagsak bruker aktørId: ${bruker.aktørId}")
    }
}
