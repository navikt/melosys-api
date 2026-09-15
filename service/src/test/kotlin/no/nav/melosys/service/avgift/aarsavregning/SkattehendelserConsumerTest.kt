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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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
        // Den delte servicen bygges ekte, ikke mocket: testene under er skrevet mot consumerens
        // opprinnelige oppførsel, og de holder derfor bare hvis flyttingen dit var tapsfri.
        skattehendelserConsumer = SkattehendelserConsumer(
            unleash,
            SkattepliktigAarsavregningOpprettelseService(
                prosessinstansService,
                fagsakService,
                behandlingService,
                behandlingsresultatService,
                årsavregningService,
                trygdeavgiftMottakerService
            )
        )
    }

    @Test
    fun `lag behandling ved skatteoppgjør hendelse når vi har fagsak behandlinger med trygdeavgift`() {
        val fagsak = lagFagsak {
            behandling {
                status = Behandlingsstatus.AVSLUTTET
            }
        }
        val behandling = fagsak.behandlinger.first()
        val behandlingsresultat = Behandlingsresultat.forTest {
            this.behandling { id = behandling.id }
        }

        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every {
            årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(
                FagsakTestFactory.SAKSNUMMER,
                GJELDER_ÅR
            )
        } returns GjeldendeBehandlingsresultaterForÅrsavregning(
            behandlingsresultat,
            sisteBehandlingsresultatMedAvgift = behandlingsresultat,
            sisteÅrsavregning = behandlingsresultat
        )
        every { prosessinstansService.opprettArsavregningsBehandlingProsessflyt(any(), any(), any(), any()) } returns mockk<UUID>()
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
                Behandlingsaarsaktyper.MELDING_FRA_SKATT,
                true
            )
        }
    }

    @Test
    fun `oppdater behandling ved skatteoppgjør med endring i tidligere skatteoppgjør og ikke avsluttet ennå med overlapp`() {
        val fagsak = lagFagsak {
            behandling {
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
        }
        val behandling = fagsak.behandlinger.first()

        val behandlingsresultat = Behandlingsresultat.forTest {
            this.behandling { id = behandling.id }
            id = 2
            type = Behandlingsresultattyper.IKKE_FASTSATT
            årsavregning {
                aar = GJELDER_ÅR
            }
        }

        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(behandling.fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat
        // Consumeren leser på nytt i samme transaksjon, så oppslaget gir samme entitet.
        every { behandlingService.hentBehandling(behandling.id) } returns behandling

        val behandlingSlot = slot<Behandling>()
        every { behandlingService.lagre(capture(behandlingSlot)) } returns Unit
        every {
            årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(
                FagsakTestFactory.SAKSNUMMER,
                GJELDER_ÅR
            )
        } returns GjeldendeBehandlingsresultaterForÅrsavregning(
            behandlingsresultat,
            sisteBehandlingsresultatMedAvgift = behandlingsresultat,
            sisteÅrsavregning = behandlingsresultat
        )
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

    /** Consumeren skal sende statusen den observerte, ikke en hardkodet verdi. */
    @Test
    fun `status oppdateres ikke når behandlingen er flyttet videre etter oppslaget`() {
        val fagsak = lagFagsak {
            behandling {
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
        }
        val behandling = fagsak.behandlinger.first()
        val behandlingsresultat = Behandlingsresultat.forTest {
            this.behandling { id = behandling.id }
            id = 2
            type = Behandlingsresultattyper.IKKE_FASTSATT
            årsavregning {
                aar = GJELDER_ÅR
            }
        }

        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat
        every {
            årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(
                FagsakTestFactory.SAKSNUMMER,
                GJELDER_ÅR
            )
        } returns GjeldendeBehandlingsresultaterForÅrsavregning(
            behandlingsresultat,
            sisteBehandlingsresultatMedAvgift = behandlingsresultat,
            sisteÅrsavregning = behandlingsresultat
        )
        every { trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) } returns true
        // Saksbehandler har flyttet behandlingen videre siden oppslaget.
        val flyttetBehandling = Behandling.forTest { status = Behandlingsstatus.IVERKSETTER_VEDTAK }
        every { behandlingService.hentBehandling(behandling.id) } returns flyttetBehandling


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
        verify(exactly = 0) { behandlingService.lagre(any()) }
        flyttetBehandling.status shouldBe Behandlingsstatus.IVERKSETTER_VEDTAK
    }

    @Test
    fun `ikke opprette ny behandling ved skatteoppgjør uten overlappende medlemskapsperiode`() {
        val fagsak = lagFagsak()

        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak
        every {
            årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(
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
        val fagsak = lagFagsak {
            behandling {
                id = 123
                status = Behandlingsstatus.AVSLUTTET
            }
        }
        val behandling = fagsak.behandlinger.first()

        val behandlingsresultat = Behandlingsresultat.forTest {
            this.behandling { id = behandling.id }
            id = 123
            type = Behandlingsresultattyper.FERDIGBEHANDLET
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat
        every { trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) } returns false
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)

        every {
            årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(
                FagsakTestFactory.SAKSNUMMER,
                GJELDER_ÅR
            )
        } returns GjeldendeBehandlingsresultaterForÅrsavregning(null, behandlingsresultat)


        every { prosessinstansService.opprettArsavregningsBehandlingProsessflyt(any(), any(), any(), any()) } returns mockk<UUID>()


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


    @ParameterizedTest
    @EnumSource(Behandlingsstatus::class, names = ["OPPRETTET", "AVVENT_DOK_PART"])
    fun `årløs behandling tillater opprettelse og gjentatt hendelse bruker årssatt behandling`(status: Behandlingsstatus) {
        val fagsak = lagFagsak {
            behandling {
                id = 41
                type = Behandlingstyper.ÅRSAVREGNING
                this.status = Behandlingsstatus.UNDER_BEHANDLING
            }
        }
        val årløs = fagsak.behandlinger.single()
        val grunnlag = Behandlingsresultat.forTest { }
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every { årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(fagsak.saksnummer, GJELDER_ÅR) } returns
            GjeldendeBehandlingsresultaterForÅrsavregning(grunnlag, sisteBehandlingsresultatMedAvgift = grunnlag)
        every { trygdeavgiftMottakerService.skalBetalesTilNav(grunnlag) } returns true
        every { behandlingsresultatService.hentBehandlingsresultat(årløs.id) } returns Behandlingsresultat.forTest { }
        every { prosessinstansService.opprettArsavregningsBehandlingProsessflyt(any(), any(), any(), any()) } returns UUID.randomUUID()
        val hendelse = ConsumerRecord("topic", 1, 1, "key", Skattehendelse(GJELDER_ÅR.toString(), AKTØR_ID, "ny"))

        skattehendelserConsumer.lesSkattehendelser(hendelse)

        val årssatt = Behandling.forTest {
            id = 42
            type = Behandlingstyper.ÅRSAVREGNING
            this.status = status
        }
        fagsak.behandlinger.add(årssatt)
        every { behandlingsresultatService.hentBehandlingsresultat(årssatt.id) } returns
            Behandlingsresultat.forTest { årsavregning { aar = GJELDER_ÅR } }
        every { behandlingService.hentBehandling(årssatt.id) } returns årssatt
        every { behandlingService.lagre(årssatt) } just Runs

        skattehendelserConsumer.lesSkattehendelser(hendelse)

        verify(exactly = 1) {
            prosessinstansService.opprettArsavregningsBehandlingProsessflyt(
                fagsak.saksnummer, GJELDER_ÅR.toString(), Behandlingsaarsaktyper.MELDING_FRA_SKATT, true
            )
        }
        verify(exactly = if (status == Behandlingsstatus.OPPRETTET) 0 else 1) { behandlingService.lagre(årssatt) }
        årssatt.status shouldBe if (status == Behandlingsstatus.OPPRETTET) status else Behandlingsstatus.VURDER_DOKUMENT
        årløs.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
    }

    private fun lagFagsak(init: FagsakTestFactory.Builder.() -> Unit = {}) = Fagsak.forTest {
        type = Sakstyper.EU_EOS
        tema = Sakstemaer.MEDLEMSKAP_LOVVALG
        status(Saksstatuser.OPPRETTET)
        init()
    }

    companion object {
        const val AKTØR_ID = "456789123"
        const val GJELDER_ÅR = 2023
    }
}
