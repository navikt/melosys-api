package no.nav.melosys.service.placeholder

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.service.behandling.BehandlingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class PlaceholderServiceTest {

    @MockK
    private lateinit var behandlingService: BehandlingService

    private lateinit var service: PlaceholderService

    @BeforeEach
    fun setup() {
        service = PlaceholderService(behandlingService, PlaceholderRegister())
    }

    @Test
    fun `katalogen inneholder saksnummer med eksempel`() {
        val definisjon = service.hentKatalog().single { it.nokkel == "saksnummer" }

        definisjon.visningsnavn shouldBe "Saksnummer"
        definisjon.beskrivelse shouldBe "Sakens saksnummer i Melosys"
        definisjon.eksempel shouldBe "2024/123456"
        definisjon.sakstyper.shouldBeEmpty()
    }

    @Test
    fun `henter saksnummer-verdi fra behandlingens fagsak`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns
            Behandling.forTest { fagsak { saksnummer = "2024/123456" } }

        service.hentVerdier(BEHANDLING_ID) shouldContainExactly
            listOf(PlaceholderVerdi(nokkel = "saksnummer", verdi = "2024/123456"))
    }

    @Test
    fun `resolver som kaster utelater feltet uten aa velte kallet`() {
        val register = mockk<PlaceholderRegister> {
            every { definisjoner } returns listOf(
                PlaceholderDefinisjon(
                    nokkel = "kaster",
                    visningsnavn = "Kaster",
                    beskrivelse = "Feiler alltid",
                    eksempel = "x",
                    resolver = { error("Oppslag feilet") },
                ),
                PlaceholderDefinisjon(
                    nokkel = "saksnummer",
                    visningsnavn = "Saksnummer",
                    beskrivelse = "Sakens saksnummer i Melosys",
                    eksempel = "2024/123456",
                    resolver = { it.fagsak.saksnummer },
                ),
            )
        }
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns
            Behandling.forTest { fagsak { saksnummer = "2024/123456" } }

        PlaceholderService(behandlingService, register).hentVerdier(BEHANDLING_ID) shouldContainExactly
            listOf(PlaceholderVerdi(nokkel = "saksnummer", verdi = "2024/123456"))
    }

    @Test
    fun `resolver som gir null utelater feltet`() {
        val register = mockk<PlaceholderRegister> {
            every { definisjoner } returns listOf(
                PlaceholderDefinisjon(
                    nokkel = "mangler",
                    visningsnavn = "Mangler",
                    beskrivelse = "Finnes ikke for behandlingen",
                    eksempel = "x",
                    resolver = { null },
                ),
            )
        }
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns Behandling.forTest()

        PlaceholderService(behandlingService, register).hentVerdier(BEHANDLING_ID).shouldBeEmpty()
    }

    private companion object {
        const val BEHANDLING_ID = 1234L
    }
}
