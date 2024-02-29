package no.nav.melosys.service.sak

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Anmodningsperiode
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstemaer.*
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Sakstyper.*
import no.nav.melosys.domain.kodeverk.Trygdeavtale_myndighetsland
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.util.*

@ExtendWith(MockKExtension::class)
internal class EndreSakServiceTest {
    @RelaxedMockK
    lateinit var lovligeKombinasjonerSaksbehandlingService: LovligeKombinasjonerSaksbehandlingService

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var mottatteOpplysningerService: MottatteOpplysningerService

    @RelaxedMockK
    lateinit var oppfriskSaksopplysningerService: OppfriskSaksopplysningerService

    @RelaxedMockK
    lateinit var applicationEventPublisher: ApplicationEventPublisher

    @RelaxedMockK
    lateinit var saksbehandlingRegler: SaksbehandlingRegler

    private lateinit var endreSakService: EndreSakService

    private val SAKSNUMMER = "MEL-123"

    @BeforeEach
    fun setUp() {
        endreSakService = EndreSakService(
            lovligeKombinasjonerSaksbehandlingService,
            fagsakService,
            behandlingsresultatService,
            mottatteOpplysningerService,
            oppfriskSaksopplysningerService,
            applicationEventPublisher,
            saksbehandlingRegler,
            FakeUnleash().apply { enableAll() }
        )
    }

    @Test
    fun `endring av sak, ikke ingen flyt - oppdater, opprett ny søknad og oppfrisk saksopplysninger`() {
        val opprinneligFagsak = lagFagsak(TRYGDEAVTALE, TRYGDEAVGIFT)
        val mottatteOpplysningerData = MottatteOpplysningerData().apply {
            periode = Periode()
            soeknadsland = Soeknadsland()
        }
        val aktivBehandling = SaksbehandlingDataFactory.lagBehandling(opprinneligFagsak, mottatteOpplysningerData)
        aktivBehandling.sisteOpplysningerHentetDato = Instant.now()
        opprinneligFagsak.behandlinger.add(aktivBehandling)

        every { fagsakService.hentFagsak(SAKSNUMMER) } returns opprinneligFagsak
        every { mottatteOpplysningerService.finnMottatteOpplysninger(any()) } returns Optional.of(MottatteOpplysninger())


        endreSakService.endre(
            SAKSNUMMER,
            EU_EOS,
            MEDLEMSKAP_LOVVALG,
            UTSENDT_ARBEIDSTAKER,
            FØRSTEGANG,
            UNDER_BEHANDLING,
            null
        )


        verify {
            fagsakService.oppdaterFagsakOgBehandling(
                SAKSNUMMER,
                EU_EOS,
                MEDLEMSKAP_LOVVALG,
                UTSENDT_ARBEIDSTAKER,
                FØRSTEGANG,
                UNDER_BEHANDLING,
                null
            )
        }
        verify { mottatteOpplysningerService.slettOpplysninger(aktivBehandling.id) }
        verify { mottatteOpplysningerService.opprettSøknadEllerAnmodningEllerAttest(aktivBehandling, any(), any()) }
        verify { oppfriskSaksopplysningerService.oppfriskSaksopplysning(aktivBehandling.id, false) }
        verify { applicationEventPublisher.publishEvent(any()) }
    }

    @Test
    fun `endring av sak, ingen flyt - slett mottatte opplysninger hvis finnes, opprett ikke nye`() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        val mottatteOpplysningerData = MottatteOpplysningerData().apply {
            periode = Periode()
            soeknadsland = Soeknadsland()
        }
        fagsak.behandlinger.add(SaksbehandlingDataFactory.lagBehandling(fagsak, mottatteOpplysningerData))
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak
        every { mottatteOpplysningerService.finnMottatteOpplysninger(any()) } returns Optional.of(MottatteOpplysninger())
        every { saksbehandlingRegler.harIngenFlyt(any(), any(), any(), any()) } returns true


        endreSakService.endre(SAKSNUMMER, FTRL, TRYGDEAVGIFT, YRKESAKTIV, FØRSTEGANG, AVVENT_FAGLIG_AVKLARING, null)


        verify { mottatteOpplysningerService.slettOpplysninger(fagsak.hentAktivBehandling().id) }
        verify(exactly = 0) { mottatteOpplysningerService.opprettSøknadEllerAnmodningEllerAttest(fagsak.hentAktivBehandling(), any(), any()) }
    }

    @Test
    fun `endring av sak, land gyldig for både ny og gammel flyt - bruker eksisterende soeknadsland`() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        val mottatteOpplysningerData = MottatteOpplysningerData().apply {
            periode = Periode()
            soeknadsland = Soeknadsland().apply { landkoder.add(Trygdeavtale_myndighetsland.FR.kode) }
        }
        fagsak.type = TRYGDEAVTALE
        val behandling = SaksbehandlingDataFactory.lagBehandling(fagsak, mottatteOpplysningerData)
        fagsak.behandlinger.add(behandling)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak
        every { mottatteOpplysningerService.finnMottatteOpplysninger(any()) } returns Optional.of(behandling.mottatteOpplysninger)


        endreSakService.endre(
            SAKSNUMMER,
            EU_EOS,
            MEDLEMSKAP_LOVVALG,
            UTSENDT_ARBEIDSTAKER,
            FØRSTEGANG,
            UNDER_BEHANDLING,
            null
        )


        val soeknadslandSlot = slot<Soeknadsland>()
        verify { mottatteOpplysningerService.slettOpplysninger(fagsak.hentAktivBehandling().id) }
        verify {
            mottatteOpplysningerService.opprettSøknadEllerAnmodningEllerAttest(
                fagsak.hentAktivBehandling(),
                any(),
                capture(soeknadslandSlot)
            )
        }
        assertThat(soeknadslandSlot.captured.landkoder).isEqualTo(mottatteOpplysningerData.soeknadsland.landkoder)
    }

    @Test
    fun `endring av sak, land ikke gyldig for ny flyt - bruker tomt soeknadsland`() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        val mottatteOpplysningerData = MottatteOpplysningerData().apply {
            periode = Periode()
            soeknadsland = Soeknadsland().apply { landkoder.add(Trygdeavtale_myndighetsland.AU.kode) }
        }
        fagsak.type = TRYGDEAVTALE
        val behandling = SaksbehandlingDataFactory.lagBehandling(fagsak, mottatteOpplysningerData)
        fagsak.behandlinger.add(behandling)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak
        every { mottatteOpplysningerService.finnMottatteOpplysninger(any()) } returns Optional.of(behandling.mottatteOpplysninger)


        endreSakService.endre(
            SAKSNUMMER,
            EU_EOS,
            MEDLEMSKAP_LOVVALG,
            UTSENDT_ARBEIDSTAKER,
            FØRSTEGANG,
            UNDER_BEHANDLING,
            null
        )


        val soeknadslandSlot = slot<Soeknadsland>()
        verify { mottatteOpplysningerService.slettOpplysninger(fagsak.hentAktivBehandling().id) }
        verify {
            mottatteOpplysningerService.opprettSøknadEllerAnmodningEllerAttest(
                fagsak.hentAktivBehandling(),
                any(),
                capture(soeknadslandSlot)
            )
        }
        assertThat(soeknadslandSlot.captured.landkoder).isEmpty()
    }

    @Test
    fun `ikke lov å endre behandlinger med status IVERKSETTER_VEDTAK`() {
        val opprinneligFagsak = lagFagsak(FTRL, UNNTAK)
        val behandling = SaksbehandlingDataFactory.lagInaktivBehandling(opprinneligFagsak).apply {
            status = IVERKSETTER_VEDTAK
        }
        opprinneligFagsak.behandlinger.add(behandling)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns opprinneligFagsak
        every { mottatteOpplysningerService.finnMottatteOpplysninger(any()) } returns Optional.of(MottatteOpplysninger())


        shouldThrow<FunksjonellException>
        {
            endreSakService.endre(
                SAKSNUMMER,
                EU_EOS,
                MEDLEMSKAP_LOVVALG,
                ANMODNING_OM_UNNTAK_HOVEDREGEL,
                FØRSTEGANG,
                UNDER_BEHANDLING,
                null
            )
        }.message.shouldBe("Behandling 1 med status IVERKSETTER_VEDTAK kan ikke endres")
    }

    @Test
    fun `ikke lov å endre fagsak eller behandlinger med sendt anmodning om unntak`() {
        val opprinneligFagsak = lagFagsak(FTRL, UNNTAK)
        val behandling = SaksbehandlingDataFactory.lagBehandling(opprinneligFagsak)
        opprinneligFagsak.behandlinger.add(behandling)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns opprinneligFagsak
        val anmodningsperiode = Anmodningsperiode().apply {
            setSendtUtland(true)
        }
        val resultat = Behandlingsresultat().apply {
            anmodningsperioder.add(anmodningsperiode)
        }
        every { behandlingsresultatService.hentBehandlingsresultatMedAnmodningsperioder(behandling.id) } returns resultat


        shouldThrow<FunksjonellException>
        {
            endreSakService.endre(
                SAKSNUMMER,
                EU_EOS,
                MEDLEMSKAP_LOVVALG,
                UTSENDT_ARBEIDSTAKER,
                FØRSTEGANG,
                UNDER_BEHANDLING,
                null
            )
        }.message.shouldBe("Behandling 1 har sendt anmodning om unntak og kan ikke lenger endres")
    }

    @Test
    fun `greit å endre behandlingsstatus med sendt anmodning om unntak`() {
        val sak = lagFagsak(FTRL, UNNTAK)
        val behandling = SaksbehandlingDataFactory.lagBehandling(sak)
        sak.behandlinger.add(behandling)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns sak
        val anmodningsperiode = Anmodningsperiode().apply {
            setSendtUtland(true)
        }
        val resultat = Behandlingsresultat().apply {
            anmodningsperioder.add(anmodningsperiode)
        }
        every { behandlingsresultatService.hentBehandlingsresultatMedAnmodningsperioder(behandling.id) } returns resultat


        endreSakService.endre(
            sak.saksnummer,
            sak.type,
            sak.tema,
            behandling.tema,
            behandling.type,
            AVVENT_FAGLIG_AVKLARING,
            null
        )


        verify {
            fagsakService.oppdaterFagsakOgBehandling(
                sak.saksnummer,
                sak.type,
                sak.tema,
                behandling.tema,
                behandling.type,
                AVVENT_FAGLIG_AVKLARING,
                null
            )
        }
    }

    @Test
    fun `endring av kun mottaksdato eller behandlingsstatus skal ikke endre mottatte opplysninger eller registeropplysninger`() {
        val sak = lagFagsak(EU_EOS, UNNTAK)
        val behandling = SaksbehandlingDataFactory.lagBehandling(sak)
        sak.behandlinger.add(behandling)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns sak


        endreSakService.endre(
            sak.saksnummer,
            sak.type,
            sak.tema,
            behandling.tema,
            behandling.type,
            AVVENT_FAGLIG_AVKLARING,
            null
        )


        verify { mottatteOpplysningerService wasNot called }
        verify { oppfriskSaksopplysningerService wasNot called }
    }

    @Test
    fun `endring av sak, ikke ingen flyt - oppdater, opprett ny søknad, oppfrisker ikke når registeropplysninger ikke har blitt hentet før`() {
        val opprinneligFagsak = lagFagsak(TRYGDEAVTALE, TRYGDEAVGIFT)
        val mottatteOpplysningerData = MottatteOpplysningerData().apply {
            periode = Periode()
            soeknadsland = Soeknadsland()
        }
        val aktivBehandling = SaksbehandlingDataFactory.lagBehandling(opprinneligFagsak, mottatteOpplysningerData)
        opprinneligFagsak.behandlinger.add(aktivBehandling)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns opprinneligFagsak
        every { mottatteOpplysningerService.finnMottatteOpplysninger(any()) } returns Optional.of(MottatteOpplysninger())
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(EU_EOS, UNNTAK, A1_ANMODNING_OM_UNNTAK_PAPIR) } returns true


        endreSakService.endre(
            SAKSNUMMER,
            EU_EOS,
            UNNTAK,
            A1_ANMODNING_OM_UNNTAK_PAPIR,
            FØRSTEGANG,
            UNDER_BEHANDLING,
            null
        )


        verify {
            fagsakService.oppdaterFagsakOgBehandling(
                SAKSNUMMER,
                EU_EOS,
                UNNTAK,
                A1_ANMODNING_OM_UNNTAK_PAPIR,
                FØRSTEGANG,
                UNDER_BEHANDLING,
                null
            )
        }
        verify { mottatteOpplysningerService.slettOpplysninger(aktivBehandling.id) }
        verify { mottatteOpplysningerService.opprettSøknadEllerAnmodningEllerAttest(aktivBehandling, any(), any()) }
        verify(exactly = 0) { oppfriskSaksopplysningerService.oppfriskSaksopplysning(aktivBehandling.id, false) }

        verify { applicationEventPublisher.publishEvent(any()) }
    }

    @Test
    fun `endring av behandlingstype til ny vurdering, opprinneligBehandling er null - oppdater opprinneligBehandling`() {
        val gammelBehandling = Behandling().apply {
            id = 1L
            tema = YRKESAKTIV
            type = FØRSTEGANG
            status = AVSLUTTET
        }
        val aktivBehandling = Behandling().apply {
            id = 2L
            tema = YRKESAKTIV
            type = HENVENDELSE
            status = UNDER_BEHANDLING
        }
        val sak = lagFagsak(FTRL, MEDLEMSKAP_LOVVALG).apply { behandlinger.addAll(listOf(gammelBehandling, aktivBehandling)) }
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns sak
        every { mottatteOpplysningerService.finnMottatteOpplysninger(2L) } returns Optional.empty()
        every { saksbehandlingRegler.finnBehandlingSomKanReplikeres(sak) } returns gammelBehandling


        endreSakService.endre(
            SAKSNUMMER,
            FTRL,
            MEDLEMSKAP_LOVVALG,
            YRKESAKTIV,
            NY_VURDERING,
            UNDER_BEHANDLING,
            null
        )


        verify {
            fagsakService.oppdaterFagsakOgBehandling(
                SAKSNUMMER,
                FTRL,
                MEDLEMSKAP_LOVVALG,
                YRKESAKTIV,
                NY_VURDERING,
                UNDER_BEHANDLING,
                null
            )
        }
        verify { mottatteOpplysningerService.slettOpplysninger(aktivBehandling.id) }
        verify { mottatteOpplysningerService.opprettSøknadEllerAnmodningEllerAttest(aktivBehandling, any(), any()) }
        verify { applicationEventPublisher.publishEvent(any()) }
        aktivBehandling.opprinneligBehandling.shouldNotBeNull().shouldBe(gammelBehandling)
    }

    private fun lagFagsak(sakstype: Sakstyper, sakstema: Sakstemaer) =
        Fagsak().apply {
            this.saksnummer = SAKSNUMMER
            this.type = sakstype
            this.tema = sakstema
            this.status = Saksstatuser.OPPRETTET
            this.aktører.add(SaksbehandlingDataFactory.lagBruker())
        }
}
