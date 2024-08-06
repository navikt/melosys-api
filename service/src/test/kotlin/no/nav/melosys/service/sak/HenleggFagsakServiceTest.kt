package no.nav.melosys.service.sak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


@ExtendWith(MockKExtension::class)
internal class HenleggFagsakServiceTest {
    @RelaxedMockK
    private lateinit var prosessinstansService: ProsessinstansService

    @RelaxedMockK
    private lateinit var oppgaveService: OppgaveService

    @RelaxedMockK
    private lateinit var fagsakService: FagsakService

    @RelaxedMockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    private lateinit var behandlingService: BehandlingService

    private lateinit var henleggFagsakService: HenleggFagsakService

    private val BEHANDLING_ID: Long = 1L
    private val BEGRUNNELSE_FRITEKST = "Dette er grunnen til at jeg henla saken"


    @BeforeEach
    fun setup() {
        henleggFagsakService = HenleggFagsakService(
            fagsakService,
            behandlingsresultatService,
            prosessinstansService,
            oppgaveService,
            behandlingService
        )
    }

    @Test
    fun henleggFagsak_gyldigHenleggelsesgrunn_behandlingsresultatBlirOppdatert() {
        val behandlingsresultat = Behandlingsresultat()
        val behandling = lagBehandling()
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns FagsakTestFactory.builder().behandlinger(behandling).build()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        henleggFagsakService.henleggFagsakEllerBehandling(FagsakTestFactory.SAKSNUMMER, Henleggelsesgrunner.ANNET.kode, BEGRUNNELSE_FRITEKST)


        verify { behandlingsresultatService.lagre(behandlingsresultat) }
        verify { prosessinstansService.opprettProsessinstansFagsakHenlagt(behandling) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        behandlingsresultat.run {
            type.shouldBe(Behandlingsresultattyper.HENLEGGELSE)
            begrunnelseFritekst.shouldBe(BEGRUNNELSE_FRITEKST)
            behandlingsresultatBegrunnelser.shouldHaveSize(1).single().kode.shouldBe(Henleggelsesgrunner.ANNET.kode)
        }
    }

    @Test
    fun henleggFagsakEllerBehandling_avslutterKunBehandling_nårBehandlingTypeErNyVurdering() {
        val behandling = lagBehandling(type = Behandlingstyper.NY_VURDERING)
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns FagsakTestFactory.builder().behandlinger(behandling).build()


        henleggFagsakService.henleggFagsakEllerBehandling(FagsakTestFactory.SAKSNUMMER, Henleggelsesgrunner.ANNET.kode, BEGRUNNELSE_FRITEKST)


        verify { behandlingService.avsluttAndregangsbehandling(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE) }
        verify { prosessinstansService.opprettProsessinstansFagsakHenlagt(behandling) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(any(), any()) }
    }

    @Test
    fun henleggFagsakEllerBehandling_avslutterKunBehandling_nårBehandlingTypeErManglendeInnbetalingTrygdeavgift() {
        val behandling = lagBehandling(type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT)
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns FagsakTestFactory.builder().behandlinger(behandling).build()


        henleggFagsakService.henleggFagsakEllerBehandling(FagsakTestFactory.SAKSNUMMER, Henleggelsesgrunner.ANNET.kode, BEGRUNNELSE_FRITEKST)


        verify { behandlingService.avsluttAndregangsbehandling(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE) }
        verify { prosessinstansService.opprettProsessinstansFagsakHenlagt(behandling) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(any(), any()) }
    }

    @Test
    fun henleggFagsakEllerBehandling_avslutterFagsakOgBehandling_nårBehandlingIkkeErAndregang() {
        val behandling = lagBehandling()
        val fagsak = FagsakTestFactory.builder().behandlinger(behandling).build()
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


        henleggFagsakService.henleggFagsakEllerBehandling(FagsakTestFactory.SAKSNUMMER, Henleggelsesgrunner.ANNET.kode, BEGRUNNELSE_FRITEKST)


        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.HENLAGT) }
        verify { prosessinstansService.opprettProsessinstansFagsakHenlagt(behandling) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify(exactly = 0) { behandlingService.avsluttAndregangsbehandling(any(), any()) }
    }

    @Test
    fun henleggFagsakEllerBehandling_ikkeGyldigHenleggelsesgrunn_kasterException() {
        shouldThrow<TekniskException> {
            henleggFagsakService.henleggFagsakEllerBehandling(
                FagsakTestFactory.SAKSNUMMER,
                "UGYLDIGKODE",
                BEGRUNNELSE_FRITEKST
            )
        }
            .message.shouldBe("UGYLDIGKODE er ingen gyldig henleggelsesgrunn")
    }

    @Test
    fun henleggSakEllerBehandlingSomBortfalt_fagsakMedFlereBehandlinger_avslutterAktivBehandlingOgStatusBlirHENLAGT_BORTFALT() {
        val fagsak = FagsakTestFactory.builder()
            .behandlinger(
                listOf(
                    lagBehandling(id = 123L, status = Behandlingsstatus.AVSLUTTET),
                    lagBehandling(id = BEHANDLING_ID, status = Behandlingsstatus.ANMODNING_UNNTAK_SENDT)
                )
            ).build()
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


        henleggFagsakService.henleggSakEllerBehandlingSomBortfalt(FagsakTestFactory.SAKSNUMMER)


        verify { fagsakService.lagre(fagsak) }
        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(123L, Behandlingsresultattyper.HENLEGGELSE_BORTFALT) }
        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE_BORTFALT) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify { behandlingService.avsluttBehandling(BEHANDLING_ID) }
        verify(exactly = 0) { behandlingService.avsluttBehandling(123L) }
        verify(exactly = 0) { behandlingService.avsluttAndregangsbehandling(any(), any()) }
        verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(any(), any()) }
        fagsak.status.shouldBe(Saksstatuser.HENLAGT_BORTFALT)
    }

    @Test
    fun henleggSakEllerBehandlingSomBortfalt_avslutterKunBehandling_dersomBehandlingTypeErNyVurdering() {
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns FagsakTestFactory.builder()
            .behandlinger(
                listOf(
                    lagBehandling(id = 123L, status = Behandlingsstatus.AVSLUTTET),
                    lagBehandling(id = BEHANDLING_ID, type = Behandlingstyper.NY_VURDERING)
                )
            ).build()


        henleggFagsakService.henleggSakEllerBehandlingSomBortfalt(FagsakTestFactory.SAKSNUMMER)


        verify { behandlingService.avsluttAndregangsbehandling(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE_BORTFALT) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify(exactly = 0) { behandlingsresultatService.oppdaterBehandlingsresultattype(any(), any()) }
        verify(exactly = 0) { behandlingService.avsluttBehandling(any()) }
        verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(any(), any()) }
    }

    @Test
    fun henleggSakEllerBehandlingSomBortfalt_avslutterKunBehandling_dersomBehandlingTypeErManglendeInnbetalingTrygdeavgift() {
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns FagsakTestFactory.builder()
            .behandlinger(
                listOf(
                    lagBehandling(id = 123L, status = Behandlingsstatus.AVSLUTTET),
                    lagBehandling(id = BEHANDLING_ID, type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT)
                )
            ).build()


        henleggFagsakService.henleggSakEllerBehandlingSomBortfalt(FagsakTestFactory.SAKSNUMMER)


        verify { behandlingService.avsluttAndregangsbehandling(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE_BORTFALT) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify(exactly = 0) { behandlingsresultatService.oppdaterBehandlingsresultattype(any(), any()) }
        verify(exactly = 0) { behandlingService.avsluttBehandling(any()) }
        verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(any(), any()) }
    }

    private fun lagBehandling(
        id: Long = BEHANDLING_ID,
        status: Behandlingsstatus = Behandlingsstatus.UNDER_BEHANDLING,
        type: Behandlingstyper = Behandlingstyper.FØRSTEGANG
    ) = Behandling().apply {
        this.id = id
        this.status = status
        this.type = type
    }
}
