package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.årsavregning
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.JobMonitor
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.aarsavregning.GjeldendeBehandlingsresultaterForÅrsavregning
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.UUID

/**
 * Fastholder funnene fra kodereviewen 20.08 av 8045-kjøringen:
 *
 *  - C1: skarp kjøring må be om innhentingsbrev, slik Kafka-flyten den replayer gjør,
 *  - C2: én sak som feiler skal ikke kunne rulle tilbake de sakene som allerede er kjørt,
 *  - C3: dryrun-stien må være read-only.
 *
 * C2 og C3 er transaksjonsoppførsel og kan ikke bevises med mocks — det gjøres i
 * `SkattepliktigeAarsavregningSkarpIT`. Her dekkes brev-flagget, at løkka går videre etter en
 * feilet sak, og re-valideringen som REQUIRES_NEW gjorde nødvendig.
 */
class SkattepliktigeAarsavregningSkarpTest {

    private val prosessinstansService = mockk<ProsessinstansService>()
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val årsavregningService = mockk<ÅrsavregningService>()
    private val trygdeavgiftMottakerService = mockk<TrygdeavgiftMottakerService>()
    private val behandlingsresultatService = mockk<BehandlingsresultatService>()
    private val skarpUtfoerer = mockk<SkattepliktigeAarsavregningSkarpUtfoerer>()

    private val service = SkattepliktigeAarsavregningDryrunService(
        fagsakService,
        årsavregningService,
        trygdeavgiftMottakerService,
        behandlingsresultatService,
        skarpUtfoerer,
    )

    @Test
    fun `skarp opprettelse ber om innhentingsbrev, som SkattehendelserConsumer`() {
        every { prosessinstansService.opprettArsavregningsBehandlingProsessflyt(any(), any(), any(), any()) } returns UUID.randomUUID()

        utfoerer().opprettProsessinstans("MEL-1", "2023")

        verify {
            prosessinstansService.opprettArsavregningsBehandlingProsessflyt(
                "MEL-1",
                "2023",
                Behandlingsaarsaktyper.MELDING_FRA_SKATT,
                true,
            )
        }
    }

    @Test
    fun `status-bump hopper over behandling som er flyttet videre i mellomtiden`() {
        // Saksbehandleren flytter behandlingen etter at løkka leste den. IVERKSETTER_VEDTAK er både
        // aktiv og ulik OPPRETTET, så bare en compare-and-set mot observert status fanger dette.
        val behandling = Behandling.forTest { status = Behandlingsstatus.IVERKSETTER_VEDTAK }
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling

        val bump = utfoerer().settStatusVurderDokument(BEHANDLING_ID, Behandlingsstatus.VURDER_DOKUMENT)

        bump.oppdatert shouldBe false
        bump.faktiskStatus shouldBe Behandlingsstatus.IVERKSETTER_VEDTAK
        behandling.status shouldBe Behandlingsstatus.IVERKSETTER_VEDTAK
        verify(exactly = 0) { behandlingService.lagre(any()) }
    }

    @Test
    fun `status-bump hopper over behandling som er avsluttet i mellomtiden`() {
        val behandling = Behandling.forTest { status = Behandlingsstatus.AVSLUTTET }
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling

        val bump = utfoerer().settStatusVurderDokument(BEHANDLING_ID, Behandlingsstatus.AVVENT_DOK_PART)

        bump.oppdatert shouldBe false
        verify(exactly = 0) { behandlingService.lagre(any()) }
    }

    @Test
    fun `status-bump skjer når behandlingen fortsatt står der løkka så den`() {
        // Startstatus er ulik målstatus, ellers ville testen overlevd at selve tilordningen forsvant.
        val behandling = Behandling.forTest { status = Behandlingsstatus.AVVENT_DOK_PART }
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { behandlingService.lagre(behandling) } returns Unit

        val bump = utfoerer().settStatusVurderDokument(BEHANDLING_ID, Behandlingsstatus.AVVENT_DOK_PART)

        bump.oppdatert shouldBe true
        behandling.status shouldBe Behandlingsstatus.VURDER_DOKUMENT
        verify { behandlingService.lagre(behandling) }
    }

    @Test
    fun `en feilet sak stopper ikke resten av kjøringen`() {
        val fagsakSomFeiler = lagFagsak("MEL-1")
        val fagsakSomGaarBra = lagFagsak("MEL-2")
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns
            listOf(fagsakSomFeiler, fagsakSomGaarBra)

        val behandlingsresultat = Behandlingsresultat.forTest { }
        every { årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(any(), GJELDER_ÅR) } returns
            GjeldendeBehandlingsresultaterForÅrsavregning(
                behandlingsresultat,
                sisteBehandlingsresultatMedAvgift = behandlingsresultat,
                sisteÅrsavregning = behandlingsresultat,
            )
        every { trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) } returns true
        every { trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat) } returns
            Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV

        every { skarpUtfoerer.opprettProsessinstans("MEL-1", "2023") } throws RuntimeException("oppslag feilet")
        every { skarpUtfoerer.opprettProsessinstans("MEL-2", "2023") } returns UUID.randomUUID()

        service.prosesserSkattehendelser(
            listOf(SkattehendelseDryrunItem(gjelderPeriode = "2023", identifikator = AKTØR_ID)),
            skarp = true,
        )

        verify { skarpUtfoerer.opprettProsessinstans("MEL-2", "2023") }
        with(service.status()) {
            this["antallSakerFunnet"] shouldBe 2
            this["antallOpprettet"] shouldBe 1
            this["antallSkarpFeilet"] shouldBe 1
        }
        service.resultater.size shouldBe 2
    }

    @Test
    fun `hoppet over status-bump rapporteres som hoppet over, ikke som feil`() {
        // Driver status-grenen i løkka: uten dette har hele grenen null dekning, og en hardkodet
        // forventetStatus i kallet ville passert alle testene.
        val fagsak = Fagsak.forTest {
            saksnummer = "MEL-1"
            type = Sakstyper.EU_EOS
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            status(Saksstatuser.OPPRETTET)
            behandling {
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVVENT_DOK_PART
            }
        }
        val årsavregningBehandling = fagsak.behandlinger.first()
        val behandlingsresultat = Behandlingsresultat.forTest {
            this.behandling { id = årsavregningBehandling.id }
            årsavregning { aar = GJELDER_ÅR }
        }

        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(årsavregningBehandling.id) } returns behandlingsresultat
        every { årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(any(), GJELDER_ÅR) } returns
            GjeldendeBehandlingsresultaterForÅrsavregning(
                behandlingsresultat,
                sisteBehandlingsresultatMedAvgift = behandlingsresultat,
                sisteÅrsavregning = behandlingsresultat,
            )
        every { trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) } returns true
        every { trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat) } returns
            Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
        every { skarpUtfoerer.settStatusVurderDokument(any(), any()) } returns
            SkattepliktigeAarsavregningSkarpUtfoerer.StatusBumpResultat(
                oppdatert = false,
                faktiskStatus = Behandlingsstatus.IVERKSETTER_VEDTAK,
            )

        service.prosesserSkattehendelser(
            listOf(SkattehendelseDryrunItem(gjelderPeriode = "2023", identifikator = AKTØR_ID)),
            skarp = true,
        )

        // Statusen løkka observerte skal sendes med — det er hele poenget med compare-and-set.
        verify {
            skarpUtfoerer.settStatusVurderDokument(årsavregningBehandling.id, Behandlingsstatus.AVVENT_DOK_PART)
        }
        with(service.resultater.single()) {
            statusOppdatert shouldBe false
            hoppetOverAarsak shouldNotBe null
            feilmelding shouldBe null
        }
        with(service.status()) {
            this["antallStatusHoppetOver"] shouldBe 1
            this["antallStatusOppdatert"] shouldBe 0
            this["antallSkarpFeilet"] shouldBe 0
        }
    }

    @Test
    fun `flere aktive årsavregninger stopper saken i stedet for å bumpe en vilkårlig`() {
        val fagsak = Fagsak.forTest {
            saksnummer = "MEL-1"
            type = Sakstyper.EU_EOS
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            status(Saksstatuser.OPPRETTET)
            behandling {
                id = 1
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVVENT_DOK_PART
            }
            behandling {
                id = 2
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
        }
        val behandlingsresultat = Behandlingsresultat.forTest { årsavregning { aar = GJELDER_ÅR } }

        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        every { årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(any(), GJELDER_ÅR) } returns
            GjeldendeBehandlingsresultaterForÅrsavregning(
                behandlingsresultat,
                sisteBehandlingsresultatMedAvgift = behandlingsresultat,
                sisteÅrsavregning = behandlingsresultat,
            )
        every { trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) } returns true

        service.prosesserSkattehendelser(
            listOf(SkattehendelseDryrunItem(gjelderPeriode = "2023", identifikator = AKTØR_ID)),
            skarp = true,
        )

        verify(exactly = 0) { skarpUtfoerer.settStatusVurderDokument(any(), any()) }
        verify(exactly = 0) { skarpUtfoerer.opprettProsessinstans(any(), any()) }
        service.resultater.single().feilmelding shouldNotBe null
        service.status()["antallOppslagFeilet"] shouldBe 1
    }

    /**
     * Copilot-review 25.08: `maksAntall` er nullbar, og løkka håndhever taket kun når den ikke er null.
     * `{"skarp": true}` uten feltet startet dermed en prod-kjøring helt uten tak — i strid med at alle
     * skarpe skrivninger skal være kappet. Asserten er at requesten avvises *før* jobben startes.
     */
    @Test
    fun `skarp kjøring uten maksAntall avvises uten å starte jobben`() {
        val dryrunService = mockk<SkattepliktigeAarsavregningDryrunService>(relaxed = true)
        val controller = SkattepliktigeAarsavregningDryrunController(dryrunService, mockk(relaxed = true))

        val utenTak = controller.run(
            SkattehendelseRunRequest(
                skattehendelser = listOf(SkattehendelseDryrunItem("2024", AKTØR_ID)),
                skarp = true,
            )
        )
        val nullTak = controller.run(
            SkattehendelseRunRequest(
                skattehendelser = listOf(SkattehendelseDryrunItem("2024", AKTØR_ID)),
                skarp = true,
                maksAntall = 0,
            )
        )

        utenTak.statusCode shouldBe HttpStatus.BAD_REQUEST
        nullTak.statusCode shouldBe HttpStatus.BAD_REQUEST
        verify(exactly = 0) { dryrunService.prosesserSkattehendelserAsynkront(any(), any(), any()) }
    }

    @Test
    fun `dryrun uten maksAntall slipper gjennom, og skarp med tak starter jobben`() {
        val dryrunService = mockk<SkattepliktigeAarsavregningDryrunService>(relaxed = true)
        val controller = SkattepliktigeAarsavregningDryrunController(dryrunService, mockk(relaxed = true))
        val hendelser = listOf(SkattehendelseDryrunItem("2024", AKTØR_ID))

        controller.run(SkattehendelseRunRequest(hendelser, skarp = false)).statusCode shouldBe HttpStatus.OK
        controller.run(SkattehendelseRunRequest(hendelser, skarp = true, maksAntall = 1)).statusCode shouldBe HttpStatus.OK

        verify(exactly = 1) { dryrunService.prosesserSkattehendelserAsynkront(hendelser, false, null) }
        verify(exactly = 1) { dryrunService.prosesserSkattehendelserAsynkront(hendelser, true, 1) }
    }

    /**
     * Copilot-review 25.08: `/status` serialiserte den levende `JobMonitor.exceptions`-mappen mens den
     * asynkrone jobben skrev til den. Samme feilklasse som `/rapport` fikk fikset i runde 2 — og verre
     * her, siden `/status` er det ops poller nettopp mens feil registreres.
     */
    @Test
    fun `status returnerer et øyeblikksbilde av exceptions, ikke den levende mappen`() {
        val monitor = JobMonitor(jobName = "test", stats = TomStats())
        monitor.registerException(IllegalStateException("første"))

        @Suppress("UNCHECKED_CAST")
        val foer = monitor.status()["exceptions"] as Map<String, Int>
        monitor.registerException(IllegalStateException("andre"))

        foer.keys shouldBe setOf("første")
        @Suppress("UNCHECKED_CAST")
        (monitor.status()["exceptions"] as Map<String, Int>).keys shouldBe setOf("første", "andre")
    }

    private class TomStats : JobMonitor.Stats {
        override fun reset() = Unit
        override fun asMap(): Map<String, Any?> = emptyMap()
    }

    private fun utfoerer() = SkattepliktigeAarsavregningSkarpUtfoerer(prosessinstansService, behandlingService)

    private fun lagFagsak(saksnummer: String) = Fagsak.forTest {
        this.saksnummer = saksnummer
        type = Sakstyper.EU_EOS
        tema = Sakstemaer.MEDLEMSKAP_LOVVALG
        status(Saksstatuser.OPPRETTET)
        behandling { status = Behandlingsstatus.AVSLUTTET }
    }

    companion object {
        const val AKTØR_ID = FagsakTestFactory.BRUKER_AKTØR_ID
        const val GJELDER_ÅR = 2023
        const val BEHANDLING_ID = 42L
    }
}
