package no.nav.melosys.service.sak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakEndretAvSaksbehandler
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstemaer.*
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Sakstyper.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationEventPublisher
import java.util.*

@ExtendWith(MockKExtension::class)
internal class EndreSakServiceTest {
    @RelaxedMockK
    lateinit var lovligeKombinasjonerService: LovligeKombinasjonerService

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    lateinit var mottatteOpplysningerService: MottatteOpplysningerService

    @RelaxedMockK
    lateinit var oppfriskSaksopplysningerService: OppfriskSaksopplysningerService

    @RelaxedMockK
    lateinit var applicationEventPublisher: ApplicationEventPublisher

    private val unleash: FakeUnleash = FakeUnleash()

    private lateinit var endreSakService: EndreSakService

    @BeforeEach
    fun setUp() {
        unleash.resetAll()
        endreSakService = EndreSakService(
            lovligeKombinasjonerService,
            fagsakService,
            behandlingService,
            mottatteOpplysningerService,
            oppfriskSaksopplysningerService,
            applicationEventPublisher,
            unleash,
        )
    }

    @Test
    fun `endring av sak, ikke tom flyt - oppdater, opprett ny søknad og oppfrisk saksopplysninger`() {
        unleash.enable("melosys.tom_periode_og_land")
        val saksnummer = "MEL-123"
        val opprinneligFagsak = lagFagsak(saksnummer, TRYGDEAVTALE, TRYGDEAVGIFT)
        val mottatteOpplysningerData = MottatteOpplysningerData().apply {
            periode = Periode()
            soeknadsland = Soeknadsland()
        }
        opprinneligFagsak.behandlinger.add(SaksbehandlingDataFactory.lagBehandling(opprinneligFagsak, mottatteOpplysningerData))
        every { fagsakService.hentFagsak(saksnummer) } returns opprinneligFagsak
        every { mottatteOpplysningerService.finnMottatteOpplysninger(any()) } returns Optional.of(MottatteOpplysninger())


        endreSakService.endre(saksnummer, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, FØRSTEGANG, null, null)


        val aktivBehandling = opprinneligFagsak.hentAktivBehandling()
        verify { fagsakService.oppdaterSakstype(saksnummer, EU_EOS) }
        verify { fagsakService.oppdaterSakstema(saksnummer, MEDLEMSKAP_LOVVALG) }
        verify { behandlingService.endreBehandling(aktivBehandling.id, FØRSTEGANG, UTSENDT_ARBEIDSTAKER, null, null) }
        verify { mottatteOpplysningerService.slettOpplysninger(aktivBehandling.id) }
        verify { mottatteOpplysningerService.opprettSøknad(aktivBehandling, any(), any()) }
        verify { oppfriskSaksopplysningerService.oppfriskSaksopplysning(aktivBehandling.id, false) }
        // event for å oppdatere oppgave
        // event for å oppdatere oppgave
        val eventCapturingSlot = slot<FagsakEndretAvSaksbehandler>()
        verify { applicationEventPublisher.publishEvent(capture(eventCapturingSlot)) }
        eventCapturingSlot.captured.source shouldBe saksnummer
    }

    @Test
    fun `endring av sak, tom flyt - slett mottate opplysninger hvis finnes, opprett ikke nye`() {
        val saksnummer = "MEL-123"
        val fagsak = SaksbehandlingDataFactory.lagFagsak(saksnummer)
        val mottatteOpplysningerData = MottatteOpplysningerData().apply {
            periode = Periode()
            soeknadsland = Soeknadsland()
        }
        fagsak.behandlinger.add(SaksbehandlingDataFactory.lagBehandling(fagsak, mottatteOpplysningerData))
        every { fagsakService.hentFagsak(saksnummer) } returns fagsak
        every { mottatteOpplysningerService.finnMottatteOpplysninger(any()) } returns Optional.of(MottatteOpplysninger())


        endreSakService.endre(saksnummer, FTRL, TRYGDEAVGIFT, YRKESAKTIV, ANKE, null, null)


        verify { mottatteOpplysningerService.slettOpplysninger(fagsak.hentAktivBehandling().id) }
        verify(exactly = 0) { mottatteOpplysningerService.opprettSøknad(fagsak.hentAktivBehandling(), any(), any()) }
    }

    @Test
    fun `endring av sak, ingen endring av sak - oppdater bare behandling`() {
        val saksnummer = "MEL-123"
        val opprinneligFagsak = lagFagsak(saksnummer, EU_EOS, MEDLEMSKAP_LOVVALG)
        opprinneligFagsak.behandlinger.add(SaksbehandlingDataFactory.lagBehandling(opprinneligFagsak))
        every { fagsakService.hentFagsak(saksnummer) } returns opprinneligFagsak


        endreSakService.endre(saksnummer, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, FØRSTEGANG, null, null)


        verify(exactly = 0) { fagsakService.oppdaterSakstype(saksnummer, EU_EOS) }
        verify(exactly = 0) { fagsakService.oppdaterSakstema(saksnummer, MEDLEMSKAP_LOVVALG) }
        verify { behandlingService.endreBehandling(
            opprinneligFagsak.hentAktivBehandling().id,
            FØRSTEGANG,
            UTSENDT_ARBEIDSTAKER,
            null,
            null
        ) }
    }

    @Test
    fun `endring av sak til EØS, toggle av og sak mangler periode og land - feiler`() {
        unleash.disableAll()
        val saksnummer = "MEL-124"
        val opprinneligFagsak = lagFagsak(saksnummer, FTRL, UNNTAK)
        opprinneligFagsak.behandlinger.add(SaksbehandlingDataFactory.lagBehandling(opprinneligFagsak))
        every { fagsakService.hentFagsak(saksnummer) } returns opprinneligFagsak
        every { mottatteOpplysningerService.finnMottatteOpplysninger(any()) } returns Optional.of(MottatteOpplysninger())

        shouldThrow<FunksjonellException>
        {
            endreSakService.endre(
                saksnummer,
                EU_EOS,
                MEDLEMSKAP_LOVVALG,
                ANMODNING_OM_UNNTAK_HOVEDREGEL,
                FØRSTEGANG,
                null,
                null
            )
        }.message.shouldBe("Du må legge inn periode og land i flyten for å kunne bytte til sakstype EU/EØS")
    }

    private fun lagFagsak(saksnummer: String, sakstype: Sakstyper, sakstema: Sakstemaer) =
        Fagsak().apply {
            this.saksnummer = saksnummer
            this.type = sakstype
            this.tema = sakstema
            this.status = Saksstatuser.OPPRETTET
            this.aktører.add(SaksbehandlingDataFactory.lagBruker())
        }
}
