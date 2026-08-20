package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.aarsavregning.GjeldendeBehandlingsresultaterForÅrsavregning
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Fastholder funnene fra kodereviewen 20.08 av 8045-kjøringen:
 *
 *  - C1: skarp kjøring må be om innhentingsbrev, slik Kafka-flyten den replayer gjør,
 *  - C2: én sak som feiler skal ikke kunne rulle tilbake de sakene som allerede er kjørt,
 *  - C3: dryrun-stien må være read-only.
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

        SkattepliktigeAarsavregningSkarpUtfoerer(prosessinstansService, behandlingService)
            .opprettProsessinstans("MEL-1", "2023")

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
    fun `side-effektene kjører i egen transaksjon slik at en feilet sak ikke ruller tilbake batchen`() {
        SkattepliktigeAarsavregningSkarpUtfoerer::class.java.declaredMethods
            .filter { it.name in setOf("opprettProsessinstans", "settStatusVurderDokument") }
            .also { it.size shouldBe 2 }
            .forEach { it.getAnnotation(Transactional::class.java).propagation shouldBe Propagation.REQUIRES_NEW }
    }

    @Test
    fun `dryrun-stien er read-only`() {
        SkattepliktigeAarsavregningDryrunService::class.java.declaredMethods
            .filter { it.name in setOf("prosesserSkattehendelser", "prosesserSkattehendelserAsynkront") }
            .mapNotNull { it.getAnnotation(Transactional::class.java) }
            .also { it.size shouldBe 2 }
            .forEach { it.readOnly shouldBe true }
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
    }
}
