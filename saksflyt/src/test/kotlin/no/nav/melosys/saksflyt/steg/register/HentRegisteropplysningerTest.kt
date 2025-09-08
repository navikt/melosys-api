package no.nav.melosys.saksflyt.steg.register

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class HentRegisteropplysningerTest {

    @MockK
    private lateinit var registeropplysningerService: RegisteropplysningerService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var persondataFasade: PersondataFasade

    @MockK
    private lateinit var saksbehandlingRegler: SaksbehandlingRegler

    private lateinit var hentRegisteropplysninger: HentRegisteropplysninger

    private val requestCaptor = slot<RegisteropplysningerRequest>()

    private lateinit var behandling: Behandling

    private val fakeUnleash = FakeUnleash()

    @BeforeEach
    fun setUp() {
        val fagsak = Fagsak.forTest {
            medBruker()
        }

        behandling = Behandling.forTest {
            id = 222L
            this.fagsak = fagsak
            type = Behandlingstyper.FØRSTEGANG
        }

        val registeropplysningerFactory = RegisteropplysningerFactory(saksbehandlingRegler, fakeUnleash)
        hentRegisteropplysninger = HentRegisteropplysninger(
            registeropplysningerService,
            behandlingService,
            saksbehandlingRegler,
            persondataFasade,
            registeropplysningerFactory
        )

        every { behandlingService.hentBehandling(behandling.id) } returns behandling

        // Mock saksbehandlingRegler metoder
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns false
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any(), any(), any()) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) } returns false
        every { saksbehandlingRegler.harIngenFlyt(any(), any(), any(), any()) } returns false

        // Mock persondataFasade
        every { persondataFasade.hentFolkeregisterident(any()) } returns "12345678901"

        // Mock registeropplysningerService
        every { registeropplysningerService.hentOgLagreOpplysninger(any()) } just Runs
    }

    private fun opprettProsessinstans(behandling: Behandling): Prosessinstans {
        return Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medBehandling(behandling)
        }
    }

    @Test
    fun `utfør skal hoppe over steg`() {
        val fagsak = Fagsak.forTest {
            type = Sakstyper.FTRL
            medBruker()
        }
        behandling.fagsak = fagsak
        behandling.tema = Behandlingstema.ARBEID_KUN_NORGE

        val prosessinstans = opprettProsessinstans(behandling)


        hentRegisteropplysninger.utfør(prosessinstans)


        verify(exactly = 0) { registeropplysningerService.hentOgLagreOpplysninger(any()) }
    }

    @Test
    fun `utfør skal hoppe over steg for virksomhet`() {
        val fagsak = Fagsak.forTest {
            type = Sakstyper.FTRL
            medVirksomhet()
        }
        behandling.fagsak = fagsak
        behandling.tema = Behandlingstema.ARBEID_KUN_NORGE

        val prosessinstans = opprettProsessinstans(behandling)


        hentRegisteropplysninger.utfør(prosessinstans)


        verify(exactly = 0) { registeropplysningerService.hentOgLagreOpplysninger(any()) }
    }

    @Test
    fun `utfør skal hente periode fra søknad når behandlingstema er utsendt arbeidstaker`() {
        val ident = "143545"
        every { persondataFasade.hentFolkeregisterident(FagsakTestFactory.BRUKER_AKTØR_ID) } returns ident

        behandling.fagsak.type = Sakstyper.EU_EOS
        behandling.tema = Behandlingstema.UTSENDT_ARBEIDSTAKER

        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(2))
        val mottatteOpplysninger = MottatteOpplysninger().apply {
            mottatteOpplysningerData = Soeknad().apply {
                this.periode = periode
            }
        }
        behandling.mottatteOpplysninger = mottatteOpplysninger

        val prosessinstans = opprettProsessinstans(behandling)


        hentRegisteropplysninger.utfør(prosessinstans)


        verify { registeropplysningerService.hentOgLagreOpplysninger(capture(requestCaptor)) }

        requestCaptor.captured.run {
            behandlingID shouldBe behandling.id
            fnr shouldBe ident
            fom shouldBe periode.fom
            tom shouldBe periode.tom
        }
    }

    @Test
    fun `utfør skal ikke lagre noe når sakstype er FTRL`() {
        behandling.tema = Behandlingstema.YRKESAKTIV
        behandling.fagsak.type = Sakstyper.FTRL

        val mottatteOpplysninger = MottatteOpplysninger().apply {
            mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS()
        }
        behandling.mottatteOpplysninger = mottatteOpplysninger

        val prosessinstans = opprettProsessinstans(behandling)


        hentRegisteropplysninger.utfør(prosessinstans)


        verify(exactly = 0) { registeropplysningerService.hentOgLagreOpplysninger(any()) }
    }

    @Test
    fun `utfør skal ikke lagre noe når sakstype er TRYGDEAVTALE`() {
        behandling.tema = Behandlingstema.YRKESAKTIV
        behandling.fagsak.type = Sakstyper.TRYGDEAVTALE

        val mottatteOpplysninger = MottatteOpplysninger().apply {
            mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS()
        }
        behandling.mottatteOpplysninger = mottatteOpplysninger

        val prosessinstans = opprettProsessinstans(behandling)


        hentRegisteropplysninger.utfør(prosessinstans)


        verify(exactly = 0) { registeropplysningerService.hentOgLagreOpplysninger(any()) }
    }

    @Test
    fun `utfør skal ikke lagre noe når sakstype er EØS og unntak`() {
        behandling.tema = Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR
        behandling.fagsak.type = Sakstyper.EU_EOS
        behandling.type = Behandlingstyper.FØRSTEGANG
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns true

        val mottatteOpplysninger = MottatteOpplysninger().apply {
            mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS()
        }
        behandling.mottatteOpplysninger = mottatteOpplysninger

        val prosessinstans = opprettProsessinstans(behandling)


        hentRegisteropplysninger.utfør(prosessinstans)


        verify(exactly = 0) { registeropplysningerService.hentOgLagreOpplysninger(any()) }
    }

    @Test
    fun `utfør skal ikke lagre noe når sakstype er EØS og ikke yrkesaktiv`() {
        behandling.tema = Behandlingstema.IKKE_YRKESAKTIV
        behandling.fagsak.type = Sakstyper.EU_EOS
        behandling.type = Behandlingstyper.FØRSTEGANG
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns true

        val mottatteOpplysninger = MottatteOpplysninger().apply {
            mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS()
        }
        behandling.mottatteOpplysninger = mottatteOpplysninger

        val prosessinstans = opprettProsessinstans(behandling)


        hentRegisteropplysninger.utfør(prosessinstans)


        verify(exactly = 0) { registeropplysningerService.hentOgLagreOpplysninger(any()) }
    }

    @Test
    fun `utfør skal hente ingenting når har ingen flyt`() {
        behandling.tema = Behandlingstema.TRYGDETID
        behandling.fagsak.type = Sakstyper.EU_EOS
        val prosessinstans = opprettProsessinstans(behandling)
        every { saksbehandlingRegler.harIngenFlyt(any(), any(), any(), any()) } returns true


        hentRegisteropplysninger.utfør(prosessinstans)


        verify { registeropplysningerService.hentOgLagreOpplysninger(capture(requestCaptor)) }
        requestCaptor.captured.opplysningstyper.shouldBeEmpty()
    }
}
