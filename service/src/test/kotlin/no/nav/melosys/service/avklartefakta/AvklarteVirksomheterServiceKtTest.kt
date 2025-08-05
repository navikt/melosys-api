package no.nav.melosys.service.avklartefakta

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.service.MottatteOpplysningerStub.lagMottatteOpplysninger
import no.nav.melosys.service.SaksopplysningStubs.lagArbeidsforholdOpplysninger
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AvklarteVirksomheterServiceKtTest {

    @MockK
    private lateinit var avklartefaktaService: AvklartefaktaService

    @MockK
    private lateinit var organisasjonOppslagService: OrganisasjonOppslagService

    @MockK
    private lateinit var mockKodeverkService: KodeverkService

    @MockK
    private lateinit var behandlingService: BehandlingService

    private lateinit var behandling: Behandling
    private lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    private val orgnr1 = "111111111"
    private val uuid1 = "a2k2jf-a3khs"

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .build()
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns setOf(orgnr1, uuid1)
        every { mockKodeverkService.dekod(any(), any()) } returns "Poststed"
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns behandling
        every { organisasjonOppslagService.hentOrganisasjoner(any()) } returns emptySet()

        avklarteVirksomheterService = AvklarteVirksomheterService(
            avklartefaktaService,
            organisasjonOppslagService,
            behandlingService,
            mockKodeverkService
        )
    }

    @Test
    fun hentUtenlandskeVirksomheter_girListeMedKunAvklarteForetak() {
        val foretakUtland1 = lagForetakUtland("Utland1", uuid1, null)
        val foretakUtlandListe = listOf(foretakUtland1)
        val saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), foretakUtlandListe, emptyList())

        val utenlandskeVirksomheter = avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling)

        utenlandskeVirksomheter.shouldHaveSize(1)
        utenlandskeVirksomheter.first().navn shouldBe "Utland1"
    }

    @Test
    fun hentUtenlandskeVirksomheter_girListeAvklartVirksomhetMedOrgnrIkkeUuid() {
        val foretakUtland = lagForetakUtland("Utland1", uuid1, "SE-123456789")
        val foretakUtlandListe = listOf(foretakUtland)
        val saksopplysninger = lagArbeidsforholdOpplysninger(emptyList())
        behandling.saksopplysninger = saksopplysninger
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), foretakUtlandListe, emptyList())

        val utenlandskeVirksomheter = avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling)

        utenlandskeVirksomheter.shouldHaveSize(1)
        utenlandskeVirksomheter.first().orgnr shouldBe "SE-123456789"
    }

    @Test
    fun harOpphørtAvklartVirksomhet_ingenOpphørsdato_girFalse() {
        val foretakUtland = lagForetakUtland("Test Foretak", uuid1, "SE-123456789")
        val foretakUtlandListe = listOf(foretakUtland)
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), foretakUtlandListe, emptyList())

        val harOpphørt = avklarteVirksomheterService.harOpphørtAvklartVirksomhet(behandling)

        harOpphørt shouldBe false
    }

    @Test
    fun lagreVirksomheterSomAvklartefakta_virksomhetIDerGyldig_virksomheterLagret() {
        every { avklartefaktaService.slettAvklarteFakta(any(), any()) } returns Unit
        every { avklartefaktaService.leggTilAvklarteFakta(any(), any(), any(), any(), any()) } returns Unit
        val foretakUtland = lagForetakUtland("Test", uuid1, null)
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(emptyList(), listOf(foretakUtland), emptyList())
        val virksomhetIDer = listOf(uuid1)

        avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, virksomhetIDer)

        verify { avklartefaktaService.slettAvklarteFakta(any(), any()) }
        verify { avklartefaktaService.leggTilAvklarteFakta(any(), any(), any(), any(), any()) }
    }

    private fun lagForetakUtland(navn: String, uuid: String, orgnr: String?): ForetakUtland = ForetakUtland().apply {
        this.navn = navn
        this.uuid = uuid
        this.orgnr = orgnr
    }
}
