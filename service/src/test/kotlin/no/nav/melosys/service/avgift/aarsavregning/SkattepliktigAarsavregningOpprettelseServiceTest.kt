package no.nav.melosys.service.avgift.aarsavregning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.årsavregning
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SkattepliktigAarsavregningOpprettelseServiceTest {

    private val prosessinstansService = mockk<ProsessinstansService>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val behandlingsresultatService = mockk<BehandlingsresultatService>()
    private val årsavregningService = mockk<ÅrsavregningService>()
    private val trygdeavgiftMottakerService = mockk<TrygdeavgiftMottakerService>()

    private val service = SkattepliktigAarsavregningOpprettelseService(
        prosessinstansService,
        fagsakService,
        behandlingService,
        behandlingsresultatService,
        årsavregningService,
        trygdeavgiftMottakerService,
    )

    @Test
    fun `årløs aktiv årsavregning gir ingen årsmatch`() {
        val fagsak = lagFagsakMedÅrsavregning()

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns
            Behandlingsresultat.forTest { }

        service.finnAktivÅrsavregningBehandling(fagsak, GJELDER_ÅR) shouldBe null
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `finner årssatt behandling selv om en årløs behandling finnes`(årløsFørst: Boolean) {
        val fagsak = lagFagsakMedÅrsavregning()
        val årløs = fagsak.behandlinger.single()
        val årssatt = Behandling.forTest {
            id = BEHANDLING_ID + 1
            type = Behandlingstyper.ÅRSAVREGNING
            status = Behandlingsstatus.OPPRETTET
        }
        fagsak.behandlinger.clear()
        fagsak.behandlinger.addAll(if (årløsFørst) listOf(årløs, årssatt) else listOf(årssatt, årløs))
        every { behandlingsresultatService.hentBehandlingsresultat(årløs.id) } returns Behandlingsresultat.forTest { }
        every { behandlingsresultatService.hentBehandlingsresultat(årssatt.id) } returns
            Behandlingsresultat.forTest { årsavregning { aar = GJELDER_ÅR } }

        service.finnAktivÅrsavregningBehandling(fagsak, GJELDER_ÅR) shouldBe årssatt
    }

    @Test
    fun `årløs behandling skjuler ikke flere aktive årsmatcher`() {
        val fagsak = lagFagsakMedÅrsavregning()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns Behandlingsresultat.forTest { }
        (1L..2L).forEach { tillegg ->
            val behandling = Behandling.forTest {
                id = BEHANDLING_ID + tillegg
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.OPPRETTET
            }
            fagsak.behandlinger.add(behandling)
            every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns
                Behandlingsresultat.forTest { årsavregning { aar = GJELDER_ÅR } }
        }

        shouldThrow<TekniskException> {
            service.finnAktivÅrsavregningBehandling(fagsak, GJELDER_ÅR)
        }.message shouldBe "Flere aktive årsavregninger funnet for sak: $SAKSNUMMER og år: $GJELDER_ÅR"
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `annet år eller avsluttet behandling gir ingen årsmatch`(avsluttet: Boolean) {
        val fagsak = lagFagsakMedÅrsavregning()
        if (avsluttet) fagsak.behandlinger.single().status = Behandlingsstatus.AVSLUTTET
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns
            Behandlingsresultat.forTest { årsavregning { aar = if (avsluttet) GJELDER_ÅR else GJELDER_ÅR - 1 } }

        service.finnAktivÅrsavregningBehandling(fagsak, GJELDER_ÅR) shouldBe null
    }

    @Test
    fun `manglende behandlingsresultat videreføres som oppslagsfeil`() {
        val fagsak = lagFagsakMedÅrsavregning()
        val feil = IkkeFunnetException("Fant ikke behandlingsresultat for $BEHANDLING_ID")
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } throws feil

        shouldThrow<IkkeFunnetException> {
            service.finnAktivÅrsavregningBehandling(fagsak, GJELDER_ÅR)
        } shouldBe feil
    }

    @Test
    fun `feil ved henting av behandlingsresultat videreføres`() {
        val fagsak = lagFagsakMedÅrsavregning()

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } throws
            IllegalStateException("EntityManager is closed")

        val feil = shouldThrow<IllegalStateException> {
            service.finnAktivÅrsavregningBehandling(fagsak, GJELDER_ÅR)
        }

        feil.message!! shouldContain "EntityManager is closed"
    }

    /**
     * IVERKSETTER_VEDTAK er både aktiv og ulik OPPRETTET, så bare en sammenligning mot den
     * observerte statusen fanger at en saksbehandler har flyttet behandlingen videre.
     */
    @Test
    fun `status-bump hopper over behandling som er flyttet videre i mellomtiden`() {
        val behandling = Behandling.forTest { status = Behandlingsstatus.IVERKSETTER_VEDTAK }
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling

        val bump = service.settStatusVurderDokument(BEHANDLING_ID, Behandlingsstatus.VURDER_DOKUMENT)

        bump.oppdatert shouldBe false
        bump.faktiskStatus shouldBe Behandlingsstatus.IVERKSETTER_VEDTAK
        behandling.status shouldBe Behandlingsstatus.IVERKSETTER_VEDTAK
        verify(exactly = 0) { behandlingService.lagre(any()) }
    }

    @Test
    fun `status-bump skjer når behandlingen fortsatt står der den ble observert`() {
        // Startstatusen er ulik målstatusen, ellers ville testen overlevd at selve tilordningen forsvant.
        val behandling = Behandling.forTest { status = Behandlingsstatus.AVVENT_DOK_PART }
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { behandlingService.lagre(behandling) } returns Unit

        val bump = service.settStatusVurderDokument(BEHANDLING_ID, Behandlingsstatus.AVVENT_DOK_PART)

        bump.oppdatert shouldBe true
        bump.faktiskStatus shouldBe Behandlingsstatus.VURDER_DOKUMENT
        behandling.status shouldBe Behandlingsstatus.VURDER_DOKUMENT
        verify { behandlingService.lagre(behandling) }
    }

    private fun lagFagsakMedÅrsavregning() = Fagsak.forTest {
        saksnummer = SAKSNUMMER
        type = Sakstyper.EU_EOS
        tema = Sakstemaer.MEDLEMSKAP_LOVVALG
        status(Saksstatuser.OPPRETTET)
        behandling {
            id = BEHANDLING_ID
            type = Behandlingstyper.ÅRSAVREGNING
            status = Behandlingsstatus.VURDER_DOKUMENT
        }
    }

    companion object {
        const val SAKSNUMMER = "MEL-1"
        const val GJELDER_ÅR = 2023
        const val BEHANDLING_ID = 42L
    }
}
