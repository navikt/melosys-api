package no.nav.melosys.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.mottatteopplysninger.AnmodningEllerAttest
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UnntaksregistreringServiceKtTest {

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var oppgaveService: OppgaveService

    @MockK
    private lateinit var prosessinstansService: ProsessinstansService

    private lateinit var unntaksregistreringService: UnntaksregistreringService

    @BeforeEach
    fun init() {
        MockKAnnotations.init(this)
        unntaksregistreringService = UnntaksregistreringService(behandlingService, behandlingsresultatService, oppgaveService, prosessinstansService)
    }

    @Test
    fun registrerUnntakFraMedlemskap_sakstypeTrygdeavtale_lagrerAltKorrekt() {
        val behandling = lagBehandling(Sakstyper.TRYGDEAVTALE, null, Land_iso2.BA)
        val behandlingsresultat = Behandlingsresultat().apply {
            utfallRegistreringUnntak = Utfallregistreringunntak.GODKJENT
        }
        val captor = slot<Behandlingsresultat>()
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { prosessinstansService.opprettProsessinstansRegistrerUnntakFraMedlemskap(any(), any()) } returns Unit
        every { oppgaveService.ferdigstillOppgaveMedBehandlingID(any()) } returns Unit
        every { behandlingsresultatService.lagre(capture(captor)) } returns Behandlingsresultat()


        unntaksregistreringService.registrerUnntakFraMedlemskap(BEHANDLING_ID)


        verify { prosessinstansService.opprettProsessinstansRegistrerUnntakFraMedlemskap(behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify { behandlingsresultatService.lagre(any()) }
        captor.captured.run {
            shouldNotBeNull()
            type shouldBe Behandlingsresultattyper.REGISTRERT_UNNTAK
            fastsattAvLand shouldBe Land_iso2.BA
        }
    }

    @Test
    fun registrerUnntakFraMedlemskap_sakstypeEØS_lagrerAltKorrekt() {
        val behandling = lagBehandling(Sakstyper.EU_EOS, Land_iso2.DK, null)
        val behandlingsresultat = Behandlingsresultat().apply {
            utfallRegistreringUnntak = Utfallregistreringunntak.GODKJENT
        }
        val captor = slot<Behandlingsresultat>()
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { prosessinstansService.opprettProsessinstansRegistrerUnntakFraMedlemskap(any(), any()) } returns Unit
        every { oppgaveService.ferdigstillOppgaveMedBehandlingID(any()) } returns Unit
        every { behandlingsresultatService.lagre(capture(captor)) } returns Behandlingsresultat()


        unntaksregistreringService.registrerUnntakFraMedlemskap(BEHANDLING_ID)


        verify { prosessinstansService.opprettProsessinstansRegistrerUnntakFraMedlemskap(behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify { behandlingsresultatService.lagre(any()) }
        captor.captured.run {
            shouldNotBeNull()
            type shouldBe Behandlingsresultattyper.REGISTRERT_UNNTAK
            fastsattAvLand shouldBe Land_iso2.DK
        }
    }

    @Test
    fun registrerUnntakFraMedlemskap_utfallRegistreringUnntakIkkeGodkjent_lagrerAltKorrekt() {
        val behandling = lagBehandling(Sakstyper.EU_EOS, null, null)
        val behandlingsresultat = Behandlingsresultat().apply {
            utfallRegistreringUnntak = Utfallregistreringunntak.IKKE_GODKJENT
        }
        val captor = slot<Behandlingsresultat>()
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { prosessinstansService.opprettProsessinstansRegistrerUnntakFraMedlemskap(any(), any()) } returns Unit
        every { oppgaveService.ferdigstillOppgaveMedBehandlingID(any()) } returns Unit
        every { behandlingsresultatService.lagre(capture(captor)) } returns Behandlingsresultat()


        unntaksregistreringService.registrerUnntakFraMedlemskap(BEHANDLING_ID)


        verify { prosessinstansService.opprettProsessinstansRegistrerUnntakFraMedlemskap(behandling, Saksstatuser.AVSLUTTET) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify { behandlingsresultatService.lagre(any()) }
        captor.captured.run {
            shouldNotBeNull()
            type shouldBe Behandlingsresultattyper.FERDIGBEHANDLET
        }
    }

    @Test
    fun registrerUnntakFraMedlemskap_mottatteOpplysningerDataIkkeAnmodningEllerAttest_kasterFeil() {
        val behandling = lagBehandling(Sakstyper.EU_EOS, null, null)
        behandling.mottatteOpplysninger?.mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS()
        val behandlingsresultat = Behandlingsresultat()
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        val exception = shouldThrow<FunksjonellException> {
            unntaksregistreringService.registrerUnntakFraMedlemskap(BEHANDLING_ID)
        }


        exception.message shouldContain "Unntaksregistrering er kun tilgjengelig for behandlinger med AnmodningEllerAttest. Det har ikke behandling"
    }

    private fun lagBehandling(sakstype: Sakstyper, avsenderland: Land_iso2?, lovvalgsland: Land_iso2?): Behandling {
        val anmodningEllerAttest = AnmodningEllerAttest().apply {
            this.avsenderland = avsenderland
            this.lovvalgsland = lovvalgsland
        }

        val fagsak = FagsakTestFactory.builder().type(sakstype).build()

        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(BEHANDLING_ID)
            .medFagsak(fagsak)
            .medMottatteOpplysninger(MottatteOpplysninger())
            .build()
        behandling.mottatteOpplysninger?.mottatteOpplysningerData = anmodningEllerAttest
        return behandling
    }

    companion object {
        private const val BEHANDLING_ID = 111L
    }
}
