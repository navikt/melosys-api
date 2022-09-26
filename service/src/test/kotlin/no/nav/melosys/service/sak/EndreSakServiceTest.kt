package no.nav.melosys.service.sak

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.Sakstemaer.TRYGDEAVGIFT
import no.nav.melosys.domain.kodeverk.Sakstyper.EU_EOS
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationEventPublisher

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
        fagsak.behandlinger.add(SaksbehandlingDataFactory.lagBehandling(fagsak))
        every { fagsakService.hentFagsak(saksnummer) } returns fagsak

        endreSakService.endre(saksnummer, EU_EOS, TRYGDEAVGIFT)

        verify { fagsakService.oppdaterSakstype(saksnummer, EU_EOS) }
        verify { fagsakService.oppdaterSakstema(saksnummer, TRYGDEAVGIFT) }
        verify { oppfriskSaksopplysningerService.oppfriskSaksopplysning(fagsak.behandlinger[0].id, false) }
        // event for å oppdatere oppgave
        verify { applicationEventPublisher.publishEvent(any()) }
    }
}
