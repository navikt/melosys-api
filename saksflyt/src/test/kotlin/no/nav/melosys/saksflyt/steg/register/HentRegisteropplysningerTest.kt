package no.nav.melosys.saksflyt.steg.register

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.mottatteOpplysningerForTest
import no.nav.melosys.domain.mottatteopplysninger.soeknad
import no.nav.melosys.domain.mottatteOpplysninger
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
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

    private val fakeUnleash = FakeUnleash()

    @BeforeEach
    fun setUp() {
        val registeropplysningerFactory = RegisteropplysningerFactory(saksbehandlingRegler, fakeUnleash)
        hentRegisteropplysninger = HentRegisteropplysninger(
            registeropplysningerService,
            behandlingService,
            saksbehandlingRegler,
            persondataFasade,
            registeropplysningerFactory
        )

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

    private fun lagBehandling(
        init: no.nav.melosys.domain.BehandlingTestFactory.BehandlingTestBuilder.() -> Unit = {}
    ): Behandling = Behandling.forTest {
        id = 222L
        type = Behandlingstyper.FØRSTEGANG
        fagsak { medBruker() }
        init()
    }.also { behandling ->
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
    }

    @Test
    fun `utfør skal hoppe over steg`() {
        val behandling = lagBehandling {
            tema = Behandlingstema.ARBEID_KUN_NORGE
            fagsak {
                type = Sakstyper.FTRL
                medBruker()
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling { id = behandling.id }
            this.behandling = behandling
        }


        hentRegisteropplysninger.utfør(prosessinstans)


        verify(exactly = 0) { registeropplysningerService.hentOgLagreOpplysninger(any()) }
    }

    @Test
    fun `utfør skal hoppe over steg for virksomhet`() {
        val behandling = lagBehandling {
            tema = Behandlingstema.ARBEID_KUN_NORGE
            fagsak {
                type = Sakstyper.FTRL
                medVirksomhet()
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling { id = behandling.id }
            this.behandling = behandling
        }


        hentRegisteropplysninger.utfør(prosessinstans)


        verify(exactly = 0) { registeropplysningerService.hentOgLagreOpplysninger(any()) }
    }

    @Test
    fun `utfør skal hente periode fra søknad når behandlingstema er utsendt arbeidstaker`() {
        val ident = "143545"
        every { persondataFasade.hentFolkeregisterident(FagsakTestFactory.BRUKER_AKTØR_ID) } returns ident

        val periodeFom = LocalDate.now()
        val periodeTom = LocalDate.now().plusYears(2)
        val behandling = lagBehandling {
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            fagsak {
                type = Sakstyper.EU_EOS
                medBruker()
            }
            mottatteOpplysninger {
                soeknad {
                    periode(periodeFom, periodeTom)
                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling { id = behandling.id }
            this.behandling = behandling
        }


        hentRegisteropplysninger.utfør(prosessinstans)


        verify { registeropplysningerService.hentOgLagreOpplysninger(capture(requestCaptor)) }

        requestCaptor.captured.run {
            behandlingID shouldBe behandling.id
            fnr shouldBe ident
            fom shouldBe periodeFom
            tom shouldBe periodeTom
        }
    }

    @Test
    fun `utfør skal ikke lagre noe når sakstype er FTRL`() {
        val behandling = lagBehandling {
            tema = Behandlingstema.YRKESAKTIV
            fagsak {
                type = Sakstyper.FTRL
                medBruker()
            }
            mottatteOpplysninger = mottatteOpplysningerForTest {
                mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS()
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling { id = behandling.id }
            this.behandling = behandling
        }


        hentRegisteropplysninger.utfør(prosessinstans)


        verify(exactly = 0) { registeropplysningerService.hentOgLagreOpplysninger(any()) }
    }

    @Test
    fun `utfør skal ikke lagre noe når sakstype er TRYGDEAVTALE`() {
        val behandling = lagBehandling {
            tema = Behandlingstema.YRKESAKTIV
            fagsak {
                type = Sakstyper.TRYGDEAVTALE
                medBruker()
            }
            mottatteOpplysninger = mottatteOpplysningerForTest {
                mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS()
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling { id = behandling.id }
            this.behandling = behandling
        }


        hentRegisteropplysninger.utfør(prosessinstans)


        verify(exactly = 0) { registeropplysningerService.hentOgLagreOpplysninger(any()) }
    }

    @Test
    fun `utfør skal ikke lagre noe når sakstype er EØS og unntak`() {
        val behandling = lagBehandling {
            tema = Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR
            type = Behandlingstyper.FØRSTEGANG
            fagsak {
                type = Sakstyper.EU_EOS
                medBruker()
            }
            mottatteOpplysninger = mottatteOpplysningerForTest {
                mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS()
            }
        }
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns true

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling { id = behandling.id }
            this.behandling = behandling
        }


        hentRegisteropplysninger.utfør(prosessinstans)


        verify(exactly = 0) { registeropplysningerService.hentOgLagreOpplysninger(any()) }
    }

    @Test
    fun `utfør skal ikke lagre noe når sakstype er EØS og ikke yrkesaktiv`() {
        val behandling = lagBehandling {
            tema = Behandlingstema.IKKE_YRKESAKTIV
            type = Behandlingstyper.FØRSTEGANG
            fagsak {
                type = Sakstyper.EU_EOS
                medBruker()
            }
            mottatteOpplysninger = mottatteOpplysningerForTest {
                mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS()
            }
        }
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns true

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling { id = behandling.id }
            this.behandling = behandling
        }


        hentRegisteropplysninger.utfør(prosessinstans)


        verify(exactly = 0) { registeropplysningerService.hentOgLagreOpplysninger(any()) }
    }

    @Test
    fun `utfør skal ikke lagre noe når det er Årsavregning for EØS-Pensjonist`() {
        val behandling = lagBehandling {
            tema = Behandlingstema.PENSJONIST
            type = Behandlingstyper.ÅRSAVREGNING
            fagsak {
                type = Sakstyper.EU_EOS
                medBruker()
            }
            mottatteOpplysninger = mottatteOpplysningerForTest {
                mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS()
            }
        }
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns true

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling { id = behandling.id }
            this.behandling = behandling
        }


        hentRegisteropplysninger.utfør(prosessinstans)


        verify(exactly = 0) { registeropplysningerService.hentOgLagreOpplysninger(any()) }
    }

    @Test
    fun `utfør skal hente ingenting når har ingen flyt`() {
        val behandling = lagBehandling {
            tema = Behandlingstema.TRYGDETID
            fagsak {
                type = Sakstyper.EU_EOS
                medBruker()
            }
        }
        every { saksbehandlingRegler.harIngenFlyt(any(), any(), any(), any()) } returns true

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling { id = behandling.id }
            this.behandling = behandling
        }


        hentRegisteropplysninger.utfør(prosessinstans)


        verify { registeropplysningerService.hentOgLagreOpplysninger(capture(requestCaptor)) }
        requestCaptor.captured.opplysningstyper.shouldBeEmpty()
    }
}
