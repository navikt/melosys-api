package no.nav.melosys.service.avgift.aarsavregning

import io.getunleash.FakeUnleash
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.aarsavregning.Skattehendelse
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*


@ExtendWith(MockKExtension::class)
class SkattehendelserConsumerTest {

    @MockK
    private lateinit var prosessinstansService: ProsessinstansService

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var årsavregningService: ÅrsavregningService

    @MockK
    private lateinit var trygdeavgiftMottakerService: TrygdeavgiftMottakerService

    private lateinit var skattehendelserConsumer: SkattehendelserConsumer

    private val unleash = FakeUnleash().apply { enableAll() }


    @BeforeEach
    fun setUp() {
        skattehendelserConsumer = SkattehendelserConsumer(
            prosessinstansService,
            unleash,
            fagsakService,
            behandlingService,
            behandlingsresultatService,
            årsavregningService,
            trygdeavgiftMottakerService
        )
    }

    @Test
    fun `lag behandling ved skatteoppgjør hendelse når vi har fagsak behandlinger med trygdeavgift`() {
        val behandling = Behandling.forTest {
            status = Behandlingsstatus.AVSLUTTET
        }
        val fagsak = lagFagsak {
            leggTilBehandling(behandling)
        }
        val behandlingsresultat = Behandlingsresultat().apply {
            this.behandling = behandling
        }

        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every {
            årsavregningService.hentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag(
                FagsakTestFactory.SAKSNUMMER,
                GJELDER_ÅR
            )
        } returns behandlingsresultat
        every { prosessinstansService.opprettArsavregningsBehandlingProsessflyt(any(), any(), any()) } returns mockk<UUID>()
        every { trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) } returns true


        skattehendelserConsumer.lesSkattehendelser(
            ConsumerRecord(
                "topic", 1, 1, "key", Skattehendelse(
                    gjelderPeriode = GJELDER_ÅR.toString(),
                    identifikator = AKTØR_ID,
                    hendelsetype = "ny"
                )
            )
        )


        verify {
            prosessinstansService.opprettArsavregningsBehandlingProsessflyt(
                FagsakTestFactory.SAKSNUMMER,
                GJELDER_ÅR.toString(),
                Behandlingsaarsaktyper.MELDING_FRA_SKATT
            )
        }
    }

    @Test
    fun `oppdater behandling ved skatteoppgjør med endring i tidligere skatteoppgjør og ikke avsluttet ennå med overlapp`() {
        val behandling = Behandling.forTest {
            type = Behandlingstyper.ÅRSAVREGNING
            status = Behandlingsstatus.UNDER_BEHANDLING
            fagsak = lagFagsak()
        }

        val behandlingsresultat = Behandlingsresultat().apply {
            this.behandling = behandling
            id = 2
            type = Behandlingsresultattyper.IKKE_FASTSATT
            årsavregning = Årsavregning.forTest {
                aar = GJELDER_ÅR
            }
        }

        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(behandling.fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat

        val behandlingSlot = slot<Behandling>()
        every { behandlingService.lagre(capture(behandlingSlot)) } returns Unit
        every {
            årsavregningService.hentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag(
                FagsakTestFactory.SAKSNUMMER,
                GJELDER_ÅR
            )
        } returns behandlingsresultat
        every { trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) } returns true


        skattehendelserConsumer.lesSkattehendelser(
            ConsumerRecord(
                "topic", 1, 1, "key", Skattehendelse(
                    gjelderPeriode = GJELDER_ÅR.toString(),
                    identifikator = AKTØR_ID,
                    hendelsetype = "ny"
                )
            )
        )


        verify { prosessinstansService wasNot Called }
        verify { behandlingService.lagre(behandling) }
        behandlingSlot.captured.status shouldBe Behandlingsstatus.VURDER_DOKUMENT
    }

    @Test
    fun `ikke opprette ny behandling ved skatteoppgjør uten overlappende medlemskapsperiode`() {
        val fagsak = lagFagsak()

        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak
        every {
            årsavregningService.hentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag(
                fagsak.saksnummer,
                any()
            )
        } returns null


        skattehendelserConsumer.lesSkattehendelser(
            ConsumerRecord(
                "topic", 1, 1, "key", Skattehendelse(
                    gjelderPeriode = GJELDER_ÅR.toString(),
                    identifikator = AKTØR_ID,
                    hendelsetype = "ny"
                )
            )
        )


        verify { prosessinstansService wasNot Called }
        verify { behandlingService wasNot Called }
    }

    @Test
    fun `skal ikke opprette automatisk årsavregningoppgave dersom trygdeavgiften bare skal betales til Skatteetaten `() {

        val behandling = Behandling.forTest {
            status = Behandlingsstatus.AVSLUTTET
            id = 123
        }
        val fagsak = lagFagsak {
            this.leggTilBehandling(behandling)
        }

        val behandlingsresultat = Behandlingsresultat().apply {
            this.behandling = behandling
            id = 123
            type = Behandlingsresultattyper.FERDIGBEHANDLET
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat
        every { trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) } returns false
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)

        every {
            årsavregningService.hentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag(
                FagsakTestFactory.SAKSNUMMER,
                GJELDER_ÅR
            )
        } returns behandlingsresultat


        every { prosessinstansService.opprettArsavregningsBehandlingProsessflyt(any(), any(), any()) } returns mockk<UUID>()


        skattehendelserConsumer.lesSkattehendelser(
            ConsumerRecord(
                "topic", 1, 1, "key", Skattehendelse(
                    gjelderPeriode = GJELDER_ÅR.toString(),
                    identifikator = AKTØR_ID,
                    hendelsetype = "ny"
                )
            )
        )


        verify { prosessinstansService wasNot Called }
        verify { behandlingService wasNot Called }
    }


    private fun lagFagsak(block: Fagsak.() -> Unit = {}) = Fagsak.forTest {
        type = Sakstyper.EU_EOS
        tema = Sakstemaer.MEDLEMSKAP_LOVVALG
        status(Saksstatuser.OPPRETTET)
    }.apply {
        block()
    }

    companion object {
        const val AKTØR_ID = "456789123"
        const val GJELDER_ÅR = 2023
    }
}
