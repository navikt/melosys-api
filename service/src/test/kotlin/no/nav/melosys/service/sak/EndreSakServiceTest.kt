package no.nav.melosys.service.sak

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.FagsakEndretAvSaksbehandler
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode
import no.nav.melosys.domain.behandlingsgrunnlag.data.Soeknadsland
import no.nav.melosys.domain.kodeverk.Sakstemaer.MEDLEMSKAP_LOVVALG
import no.nav.melosys.domain.kodeverk.Sakstemaer.TRYGDEAVGIFT
import no.nav.melosys.domain.kodeverk.Sakstyper.EU_EOS
import no.nav.melosys.domain.kodeverk.Sakstyper.FTRL
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationEventPublisher
import kotlin.test.assertFailsWith

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EndreSakServiceTest {
    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var behandlingsgrunnlagService: BehandlingsgrunnlagService

    @RelaxedMockK
    lateinit var oppfriskSaksopplysningerService: OppfriskSaksopplysningerService

    @RelaxedMockK
    lateinit var applicationEventPublisher: ApplicationEventPublisher

    lateinit var endreSakService: EndreSakService

    @BeforeEach
    fun setUp() {
        endreSakService = EndreSakService(fagsakService, behandlingsgrunnlagService, oppfriskSaksopplysningerService, applicationEventPublisher)
    }

    @Test
    fun `endring av sak - oppdater type og tema, opprett ny søknad og oppfrisk saksopplysninger`() {
        val saksnummer = "MEL-123"
        val fagsak = SaksbehandlingDataFactory.lagFagsak(saksnummer)
        val behandlingsgrunnlagData = BehandlingsgrunnlagData().apply {
            periode = Periode()
            soeknadsland = Soeknadsland()
        }
        fagsak.behandlinger.add(SaksbehandlingDataFactory.lagBehandling(fagsak, behandlingsgrunnlagData))
        every { fagsakService.hentFagsak(saksnummer) } returns fagsak

        endreSakService.endre(saksnummer, FTRL, MEDLEMSKAP_LOVVALG)

        verify { behandlingsgrunnlagService.slettGrunnlag(fagsak.hentAktivBehandling().id) }
        verify { behandlingsgrunnlagService.opprettSøknad(fagsak.hentAktivBehandling(), any(), any()) }
        verify { fagsakService.oppdaterSakstype(saksnummer, FTRL) }
        verify { fagsakService.oppdaterSakstema(saksnummer, MEDLEMSKAP_LOVVALG) }
        verify { oppfriskSaksopplysningerService.oppfriskSaksopplysning(fagsak.hentAktivBehandling().id, false) }
        // event for å oppdatere oppgave
        val eventCapturingSlot = slot<FagsakEndretAvSaksbehandler>()
        verify { applicationEventPublisher.publishEvent(capture(eventCapturingSlot)) }
        eventCapturingSlot.captured.source shouldBe fagsak
    }

    @Test
    fun `endring av sak til sakstype EØS - sak mangler periode og land - feiler`() {
        val saksnummer = "MEL-123"
        val fagsak = SaksbehandlingDataFactory.lagFagsak(saksnummer)
        fagsak.behandlinger.add(SaksbehandlingDataFactory.lagBehandling(fagsak))
        every { fagsakService.hentFagsak(saksnummer) } returns fagsak

        assertFailsWith<FunksjonellException>(
            block = { endreSakService.endre(saksnummer, EU_EOS, TRYGDEAVGIFT) },
            message = "Du må legge inn periode og land i flyten for å kunne bytte til sakstype EU/EØS"
        )
    }
}
