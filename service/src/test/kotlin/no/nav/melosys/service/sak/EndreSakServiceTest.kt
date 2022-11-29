package no.nav.melosys.service.sak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.BehandlingEndretAvSaksbehandlerEvent
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakEndretAvSaksbehandler
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstemaer.*
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Sakstyper.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import java.util.*

@ExtendWith(MockKExtension::class)
internal class EndreSakServiceTest {
    @RelaxedMockK
    lateinit var lovligeKombinasjonerService: LovligeKombinasjonerService

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

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
        val aktivBehandling = SaksbehandlingDataFactory.lagBehandling(opprinneligFagsak, mottatteOpplysningerData)
        opprinneligFagsak.behandlinger.add(aktivBehandling)
        every { fagsakService.hentFagsak(saksnummer) } returns opprinneligFagsak
        every { mottatteOpplysningerService.finnMottatteOpplysninger(any()) } returns Optional.of(MottatteOpplysninger())
        val applicationEvents = mutableListOf<ApplicationEvent>()
        every { applicationEventPublisher.publishEvent(capture(applicationEvents)) } just Runs


        endreSakService.endre(saksnummer, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, FØRSTEGANG, null, null)


        verify {
            fagsakService.oppdaterFagsakOgBehandling(
                saksnummer,
                EU_EOS,
                MEDLEMSKAP_LOVVALG,
                UTSENDT_ARBEIDSTAKER,
                FØRSTEGANG,
                null,
                null
            )
        }
        verify { mottatteOpplysningerService.slettOpplysninger(aktivBehandling.id) }
        verify { mottatteOpplysningerService.opprettSøknad(aktivBehandling, any(), any()) }
        verify { oppfriskSaksopplysningerService.oppfriskSaksopplysning(aktivBehandling.id, false) }

        applicationEvents.shouldHaveSize(2).forExactly(1) {
            it.shouldBeTypeOf<FagsakEndretAvSaksbehandler>()
            it.source shouldBe saksnummer
        }.forExactly(1) {
            it.shouldBeTypeOf<BehandlingEndretAvSaksbehandlerEvent>()
            it.source shouldBe aktivBehandling.id
        }
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

    @Test
    fun `ikke lov å endre behandlinger med status IVERKSETTER_VEDTAK`() {
        val saksnummer = "MEL-124"
        val opprinneligFagsak = lagFagsak(saksnummer, FTRL, UNNTAK)
        val behandling = SaksbehandlingDataFactory.lagInaktivBehandling(opprinneligFagsak).apply {
            status = Behandlingsstatus.IVERKSETTER_VEDTAK
        }
        opprinneligFagsak.behandlinger.add(behandling)
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
        }.message.shouldBe("Behandling 1 med status IVERKSETTER_VEDTAK kan ikke endres")
    }

    @Test
    fun `endring av kun mottaksdato eller behandlingsstatus skal ikke endre mottatte opplysninger eller registeropplysninger`() {
        val saksnummer = "MEL-124"
        val sak = lagFagsak(saksnummer, EU_EOS, UNNTAK)
        val behandling = SaksbehandlingDataFactory.lagBehandling(sak)
        sak.behandlinger.add(behandling)
        every { fagsakService.hentFagsak(saksnummer) } returns sak


        endreSakService.endre(sak.saksnummer, sak.type, sak.tema, behandling.tema, behandling.type, Behandlingsstatus.AVVENT_FAGLIG_AVKLARING, null)


        verify { mottatteOpplysningerService wasNot called}
        verify { oppfriskSaksopplysningerService wasNot called}
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
