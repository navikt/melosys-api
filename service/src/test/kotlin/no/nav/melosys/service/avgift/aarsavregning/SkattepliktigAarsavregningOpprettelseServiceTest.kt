package no.nav.melosys.service.avgift.aarsavregning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.Test

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

    /**
     * En aktiv ÅRSAVREGNING-behandling uten rad i `aarsavregning` får `hentÅrsavregning()` til å
     * kaste. Den rå meldingen sier ikke hvilken behandling det gjelder, og begge flytene som treffer
     * dette må stoppes av et menneske — kastet skal derfor navngi behandlingen som må lukkes.
     */
    @Test
    fun `årløs aktiv årsavregning gir en feilmelding som navngir behandlingen`() {
        val fagsak = lagFagsakMedÅrsavregning()

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns
            Behandlingsresultat.forTest { }

        val feil = shouldThrow<TekniskException> {
            service.finnAktivÅrsavregningBehandling(fagsak, GJELDER_ÅR)
        }

        feil.message!! shouldContain BEHANDLING_ID.toString()
        feil.message!! shouldContain SAKSNUMMER
        // Handlingsanvisningen, ikke bare ordet «årløs»: uten den er meldingen en diagnose
        // operatøren ikke kan gjøre noe med.
        feil.message!! shouldContain "lukk den årløse behandlingen"
    }

    /**
     * Bare manglende aarsavregning-rad er det årløse tilfellet. Enhver annen IllegalStateException
     * fra oppslaget — en lukket EntityManager, en tilstandssjekk lenger nede — skal ikke merkes som
     * årløs, for da sendes den som rydder til å lukke en behandling som ikke er problemet.
     */
    @Test
    fun `annen tilstandsfeil enn manglende årsavregning merkes ikke som årløs`() {
        val fagsak = lagFagsakMedÅrsavregning()

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } throws
            IllegalStateException("EntityManager is closed")

        val feil = shouldThrow<IllegalStateException> {
            service.finnAktivÅrsavregningBehandling(fagsak, GJELDER_ÅR)
        }

        feil.message!! shouldContain "EntityManager is closed"
    }

    /**
     * Statusen som ble observert da det ble bestemt at behandlingen skulle bumpes, kan ha endret
     * seg før skrivingen. IVERKSETTER_VEDTAK er både aktiv og ulik OPPRETTET, så bare en
     * sammenligning mot den observerte statusen fanger at en saksbehandler har flyttet
     * behandlingen videre i mellomtiden — å kaste den tilbake til VURDER_DOKUMENT ville tatt
     * saksbehandleren med seg.
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
