package no.nav.melosys.service.sak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.FagsakTestFactory.BEHANDLING_ID
import no.nav.melosys.domain.FagsakTestFactory.lagBehandling
import no.nav.melosys.domain.FagsakTestFactory.lagFagsakMedBehandlinger
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class HenleggelseServiceTest {
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

    private lateinit var henleggelseService: HenleggelseService

    private val BEGRUNNELSE_FRITEKST = "Dette er grunnen til at jeg henla saken"

    @BeforeEach
    fun setup() {
        henleggelseService = HenleggelseService(
            fagsakService,
            behandlingsresultatService,
            prosessinstansService,
            oppgaveService,
            behandlingService
        )
    }

    @Nested
    inner class HenleggFagsakEllerBehandlingMedHenleggelsesgrunn {
        @Test
        fun henleggFagsak_gyldigHenleggelsesgrunn_behandlingsresultatBlirOppdatert() {
            val behandlingsresultat = Behandlingsresultat()
            val behandling = lagBehandling()
            every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns FagsakTestFactory.builder().behandlinger(behandling).build()
            every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


            henleggelseService.henleggFagsakEllerBehandling(FagsakTestFactory.SAKSNUMMER, Henleggelsesgrunner.ANNET.kode, BEGRUNNELSE_FRITEKST)


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
        fun henleggFagsakEllerBehandling_nårBehandlingErEnesteBehandling_avslutterFagsakOgBehandling() {
            val behandling = lagBehandling()
            val fagsak = FagsakTestFactory.builder().behandlinger(behandling).build()
            every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


            henleggelseService.henleggFagsakEllerBehandling(FagsakTestFactory.SAKSNUMMER, Henleggelsesgrunner.ANNET.kode, BEGRUNNELSE_FRITEKST)


            verify { fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.HENLAGT) }
            verify { prosessinstansService.opprettProsessinstansFagsakHenlagt(behandling) }
            verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
            verify(exactly = 0) { behandlingService.avsluttAndregangsbehandling(any(), any()) }
        }

        @Test
        fun henleggFagsakEllerBehandling_vedFlereBehandlinger_avslutterKunBehandling() {
            val førstegangsBehandling = lagBehandling(123, type = Behandlingstyper.ÅRSAVREGNING)
            val annengangsBehandling = lagBehandling(BEHANDLING_ID)
            val fagsak = FagsakTestFactory.builder().behandlinger(listOf(førstegangsBehandling, annengangsBehandling)).build()
            every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


            henleggelseService.henleggFagsakEllerBehandling(FagsakTestFactory.SAKSNUMMER, Henleggelsesgrunner.ANNET.kode, BEGRUNNELSE_FRITEKST)


            verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.HENLAGT) }
            verify { prosessinstansService.opprettProsessinstansFagsakHenlagt(annengangsBehandling) }
            verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
            verify { behandlingService.avsluttAndregangsbehandling(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE) }
        }

        @Test
        fun henleggFagsakEllerBehandling_ikkeGyldigHenleggelsesgrunn_kasterException() {
            shouldThrow<TekniskException> {
                henleggelseService.henleggFagsakEllerBehandling(
                    FagsakTestFactory.SAKSNUMMER,
                    "UGYLDIGKODE",
                    BEGRUNNELSE_FRITEKST
                )
            }
                .message.shouldBe("UGYLDIGKODE er ingen gyldig henleggelsesgrunn")
        }
    }

    @Nested
    inner class HenleggFagsak {

        @Test
        fun henleggSakEllerBehandlingSomBortfalt_fagsakMedFlereBehandlinger_avslutterAktivBehandlingEndrerIkkeSaksStatus() {
            val fagsak = lagFagsakMedBehandlinger(
                lagBehandling(id = 123L, status = Behandlingsstatus.AVSLUTTET),
                lagBehandling(id = BEHANDLING_ID, status = Behandlingsstatus.ANMODNING_UNNTAK_SENDT)
            )

            every { behandlingService.hentBehandling(BEHANDLING_ID) } returns fagsak.behandlinger.last()
            every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


            henleggelseService.henleggSakEllerBehandlingSomBortfalt(BEHANDLING_ID)


            verify { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE_BORTFALT) }
            verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
            verify { behandlingService.avsluttBehandling(BEHANDLING_ID) }
            fagsak.status.shouldBe(Saksstatuser.OPPRETTET)
        }

        @Test
        fun henleggSakEllerBehandlingSomBortfalt_fagsakMedFlereBehandlinger_avslutterAktivBehandlingSakBlirHENLAGT_BORTFALT() {
            val enesteBehandling = lagBehandling(id = BEHANDLING_ID, status = Behandlingsstatus.UNDER_BEHANDLING)
            val fagsak = lagFagsakMedBehandlinger(enesteBehandling)

            every { behandlingService.hentBehandling(BEHANDLING_ID) } returns enesteBehandling
            every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


            henleggelseService.henleggSakEllerBehandlingSomBortfalt(BEHANDLING_ID)


            verify { fagsakService.lagre(withArg { it.status shouldBe Saksstatuser.HENLAGT_BORTFALT }) }
            verify { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE_BORTFALT) }
            verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
            verify { behandlingService.avsluttBehandling(BEHANDLING_ID) }
        }
    }
}
