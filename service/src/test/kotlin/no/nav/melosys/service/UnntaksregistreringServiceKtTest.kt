package no.nav.melosys.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak
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
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class UnntaksregistreringServiceKtTest {

    @RelaxedMockK
    lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var oppgaveService: OppgaveService

    @RelaxedMockK
    lateinit var prosessinstansService: ProsessinstansService

    private lateinit var unntaksregistreringService: UnntaksregistreringService

    @BeforeEach
    fun init() {
        unntaksregistreringService = UnntaksregistreringService(
            behandlingService,
            behandlingsresultatService,
            oppgaveService,
            prosessinstansService
        )
    }

    @Test
    fun `registrer unntak fra medlemskap sakstype trygdeavtale lagrer alt korrekt`() {
        val behandling = lagBehandling(Sakstyper.TRYGDEAVTALE, null, Land_iso2.BA)
        val behandlingsresultat = Behandlingsresultat().apply {
            utfallRegistreringUnntak = Utfallregistreringunntak.GODKJENT
        }

        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingsresultatService.lagre(any<Behandlingsresultat>()) } answers { firstArg<Behandlingsresultat>() }

        unntaksregistreringService.registrerUnntakFraMedlemskap(BEHANDLING_ID)

        verify { prosessinstansService.opprettProsessinstansRegistrerUnntakFraMedlemskap(behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify { behandlingsresultatService.lagre(any<Behandlingsresultat>()) }
    }

    @Test
    fun `registrer unntak fra medlemskap sakstype EØS lagrer alt korrekt`() {
        val behandling = lagBehandling(Sakstyper.EU_EOS, Land_iso2.DK, null)
        val behandlingsresultat = Behandlingsresultat().apply {
            utfallRegistreringUnntak = Utfallregistreringunntak.GODKJENT
        }

        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingsresultatService.lagre(any<Behandlingsresultat>()) } answers { firstArg<Behandlingsresultat>() }

        unntaksregistreringService.registrerUnntakFraMedlemskap(BEHANDLING_ID)

        verify { prosessinstansService.opprettProsessinstansRegistrerUnntakFraMedlemskap(behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify { behandlingsresultatService.lagre(any<Behandlingsresultat>()) }
    }

    @Test
    fun `registrer unntak fra medlemskap utfall registrering unntak ikke godkjent lagrer alt korrekt`() {
        val behandling = lagBehandling(Sakstyper.EU_EOS, null, null)
        val behandlingsresultat = Behandlingsresultat().apply {
            utfallRegistreringUnntak = Utfallregistreringunntak.IKKE_GODKJENT
        }

        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingsresultatService.lagre(any<Behandlingsresultat>()) } answers { firstArg<Behandlingsresultat>() }

        unntaksregistreringService.registrerUnntakFraMedlemskap(BEHANDLING_ID)

        verify { prosessinstansService.opprettProsessinstansRegistrerUnntakFraMedlemskap(behandling, Saksstatuser.AVSLUTTET) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify { behandlingsresultatService.lagre(any<Behandlingsresultat>()) }
    }

    @Test
    fun `registrer unntak fra medlemskap mottatte opplysninger data ikke anmodning eller attest kaster feil`() {
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

    private fun lagBehandling(sakstype: Sakstyper, avsenderland: Land_iso2?, lovvalgsland: Land_iso2?) =
        FagsakTestFactory.builder().type(sakstype).build().let { fagsak ->
            val anmodningEllerAttest = AnmodningEllerAttest().apply {
                this.avsenderland = avsenderland
                this.lovvalgsland = lovvalgsland
            }

            BehandlingTestFactory.builderWithDefaults()
                .medId(BEHANDLING_ID)
                .medFagsak(fagsak)
                .medMottatteOpplysninger(MottatteOpplysninger().apply {
                    mottatteOpplysningerData = anmodningEllerAttest
                })
                .build()
        }

    companion object {
        private const val BEHANDLING_ID = 111L
    }
}
