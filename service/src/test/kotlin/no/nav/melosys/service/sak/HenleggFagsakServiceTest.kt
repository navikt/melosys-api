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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource


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

    @Nested
    inner class HenleggFagsakEllerBehandlingMedHenleggelsesgrunn {
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
        fun henleggFagsakEllerBehandling_nårBehandlingErEnesteBehandling_avslutterFagsakOgBehandling() {
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
        fun henleggFagsakEllerBehandling_vedFlereBehandlinger_avslutterKunBehandling() {
            val førstegangsBehandling = lagBehandling(123, type = Behandlingstyper.ÅRSAVREGNING)
            val annengangsBehandling = lagBehandling(BEHANDLING_ID)
            val fagsak = FagsakTestFactory.builder().behandlinger(listOf(førstegangsBehandling, annengangsBehandling)).build()
            every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


            henleggFagsakService.henleggFagsakEllerBehandling(FagsakTestFactory.SAKSNUMMER, Henleggelsesgrunner.ANNET.kode, BEGRUNNELSE_FRITEKST)


            verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.HENLAGT) }
            verify { prosessinstansService.opprettProsessinstansFagsakHenlagt(annengangsBehandling) }
            verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
            verify { behandlingService.avsluttAndregangsbehandling(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE) }
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
    }

    @Nested
    inner class HenleggFagsak {
        @Test
        fun henleggFagsakEllerBehandlingSomBortfalt_årsavregningsbehandling_med_andre_aktive_behandlingerGirUrørtSakstatus() {
            val aktivBehandling = lagBehandling(id = 123L, status = Behandlingsstatus.UNDER_BEHANDLING)
            val årsavregningsBehandling =
                lagBehandling(id = BEHANDLING_ID, status = Behandlingsstatus.UNDER_BEHANDLING, type = Behandlingstyper.ÅRSAVREGNING)
            val fagsak = lagFagsakMedBehandlinger(
                aktivBehandling,
                årsavregningsBehandling
            )

            every { behandlingService.hentBehandling(BEHANDLING_ID) } returns årsavregningsBehandling
            every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak

            henleggFagsakService.henleggSakEllerBehandlingSomBortfalt(BEHANDLING_ID)


            verify { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE_BORTFALT) }
            verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
            verify { behandlingService.avsluttBehandling(BEHANDLING_ID) }
            verify(exactly = 0) { behandlingService.avsluttBehandling(123L) }
            verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(any(), any()) }
        }

        @Test
        fun henleggFagsakEllerBehandlingSomBortfalt_årsavregningsbehandling_som_eneste_aktiv_behandlingSakBlirHENLAGT_BORTFALT() {
            val førstegangsBehandling =
                lagBehandling(id = 123, status = Behandlingsstatus.AVSLUTTET, type = Behandlingstyper.FØRSTEGANG)
            val årsavregningsBehandling =
                lagBehandling(id = BEHANDLING_ID, status = Behandlingsstatus.UNDER_BEHANDLING, type = Behandlingstyper.ÅRSAVREGNING)
            val fagsak = lagFagsakMedBehandlinger(årsavregningsBehandling, førstegangsBehandling)

            every { behandlingService.hentBehandling(BEHANDLING_ID) } returns årsavregningsBehandling
            every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak

            henleggFagsakService.henleggSakEllerBehandlingSomBortfalt(BEHANDLING_ID)


            verify { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE_BORTFALT) }
            verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
            verify { behandlingService.avsluttBehandling(BEHANDLING_ID) }
            verify {
                fagsakService.lagre(withArg { fagsak -> fagsak.status shouldBe Saksstatuser.HENLAGT_BORTFALT })
            }
        }

        @ParameterizedTest
        @EnumSource(value = Behandlingstyper::class, names = ["FØRSTEGANG", "HENVENDELSE"])
        fun henleggFagsakEllerBehandlingSomBortfalt_førstegang_eller_hendvendelse_som_eneste_behandlingSakBlirHENLAGT_BORTFALT(behandlingsType: Behandlingstyper) {
            val forstegangBehandling =
                lagBehandling(id = BEHANDLING_ID, status = Behandlingsstatus.UNDER_BEHANDLING, type = behandlingsType)
            val fagsak = lagFagsakMedBehandlinger(forstegangBehandling)

            every { behandlingService.hentBehandling(BEHANDLING_ID) } returns forstegangBehandling
            every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak

            henleggFagsakService.henleggSakEllerBehandlingSomBortfalt(BEHANDLING_ID)


            verify { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE_BORTFALT) }
            verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
            verify { behandlingService.avsluttBehandling(BEHANDLING_ID) }
            verify {
                fagsakService.lagre(withArg { fagsak -> fagsak.status shouldBe Saksstatuser.HENLAGT_BORTFALT })
            }
        }

        @Test
        fun henleggSakEllerBehandlingSomBortfalt_fagsakMedFlereBehandlinger_avslutterAktivBehandlingOgStatusBlirHENLAGT_BORTFALT() {
            val fagsak = lagFagsakMedBehandlinger(
                lagBehandling(id = 123L, status = Behandlingsstatus.AVSLUTTET),
                lagBehandling(id = BEHANDLING_ID, status = Behandlingsstatus.ANMODNING_UNNTAK_SENDT)
            )

            every { behandlingService.hentBehandling(BEHANDLING_ID) } returns fagsak.behandlinger.firstOrNull()
            every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak

            henleggFagsakService.henleggSakEllerBehandlingSomBortfalt(BEHANDLING_ID)


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
            val fagsak = lagFagsakMedBehandlinger(
                lagBehandling(id = 123L, status = Behandlingsstatus.AVSLUTTET),
                lagBehandling(id = BEHANDLING_ID, type = Behandlingstyper.NY_VURDERING)
            )

            every { behandlingService.hentBehandling(BEHANDLING_ID) } returns fagsak.behandlinger.firstOrNull()
            every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak

            henleggFagsakService.henleggSakEllerBehandlingSomBortfalt(BEHANDLING_ID)


            verify { behandlingService.avsluttAndregangsbehandling(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE_BORTFALT) }
            verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
            verify(exactly = 0) { behandlingsresultatService.oppdaterBehandlingsresultattype(any(), any()) }
            verify(exactly = 0) { behandlingService.avsluttBehandling(any()) }
            verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(any(), any()) }
        }

        @Test
        fun henleggSakEllerBehandlingSomBortfalt_avslutterKunBehandling_dersomBehandlingTypeErManglendeInnbetalingTrygdeavgift() {
            val fagsak = lagFagsakMedBehandlinger(
                lagBehandling(id = 123L, status = Behandlingsstatus.AVSLUTTET),
                lagBehandling(id = BEHANDLING_ID, type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT)
            )

            every { behandlingService.hentBehandling(BEHANDLING_ID) } returns fagsak.behandlinger.firstOrNull()
            every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak

            henleggFagsakService.henleggSakEllerBehandlingSomBortfalt(BEHANDLING_ID)


            verify { behandlingService.avsluttAndregangsbehandling(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE_BORTFALT) }
            verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
            verify(exactly = 0) { behandlingsresultatService.oppdaterBehandlingsresultattype(any(), any()) }
            verify(exactly = 0) { behandlingService.avsluttBehandling(any()) }
            verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(any(), any()) }
        }
    }
}
