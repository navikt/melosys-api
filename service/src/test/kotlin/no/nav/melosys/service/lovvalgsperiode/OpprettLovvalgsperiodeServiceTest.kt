package no.nav.melosys.service.lovvalgsperiode

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.*
import no.nav.melosys.domain.mottatteopplysninger.AnmodningEllerAttest
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.LovvalgsperiodeRepository
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OpprettLovvalgsperiodeServiceTest {
    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var lovvalgsperiodeRepository: LovvalgsperiodeRepository

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var landvelgerService: LandvelgerService

    @MockK
    private lateinit var saksbehandlingRegler: SaksbehandlingRegler

    private val slotLovvalgsperiode = slot<Lovvalgsperiode>()

    private lateinit var opprettLovvalgsperiodeService: OpprettLovvalgsperiodeService

    @BeforeEach
    fun setup() {
        slotLovvalgsperiode.clear()
        opprettLovvalgsperiodeService = OpprettLovvalgsperiodeService(
            lovvalgsperiodeRepository,
            behandlingService,
            behandlingsresultatService,
            saksbehandlingRegler,
            landvelgerService
        )

        every { landvelgerService.hentArbeidsland(1L) } returns Land_iso2.NO
    }

    @Test
    fun opprettLovvalgsperiode_unntaksregistreringsflyt_lagrerKorrekt() {
        val request = requestForUnntaksregistreringFlyt(Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART9_3, null)
        val behandling = lagBehandling(Land_iso2.AU)
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns true
        mockHappyCase(behandling)

        opprettLovvalgsperiodeService.opprettLovvalgsperiode(1L, request)


        verify(exactly = 1) { lovvalgsperiodeRepository.save(capture(slotLovvalgsperiode)) }
        val lagretLovvalgsperiode = slotLovvalgsperiode.captured
        lagretLovvalgsperiode.shouldNotBeNull()
        lagretLovvalgsperiode.fom.shouldBe(request.fomDato)
        lagretLovvalgsperiode.tom.shouldBe(request.tomDato)
        lagretLovvalgsperiode.bestemmelse.shouldBe(request.lovvalgsbestemmelse)
        lagretLovvalgsperiode.innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
        lagretLovvalgsperiode.lovvalgsland.shouldBe(Land_iso2.AU)
        lagretLovvalgsperiode.medlemskapstype.shouldBe(Medlemskapstyper.UNNTATT)
        lagretLovvalgsperiode.dekning.shouldBe(Trygdedekninger.UTEN_DEKNING)
    }

    @Test
    fun opprettLovvalgsperiode_unntaksregistreringsflytQuebec_lagrerKorrekt() {
        val request = requestForUnntaksregistreringFlyt(Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE, null)
        val behandling = lagBehandling(Land_iso2.CA_QC)
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns true
        mockHappyCase(behandling)


        opprettLovvalgsperiodeService.opprettLovvalgsperiode(1L, request)


        verify(exactly = 1) { lovvalgsperiodeRepository.save(capture(slotLovvalgsperiode)) }
        slotLovvalgsperiode.captured.shouldNotBeNull()
        slotLovvalgsperiode.captured.lovvalgsland.shouldBe(Land_iso2.CA)
    }

    @Test
    fun opprettLovvalgsperiode_unntaksregistreringsflytCAN_ART7_lagrerKorrekt() {
        val request = requestForUnntaksregistreringFlyt(Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART7, null)
        val behandling = lagBehandling(Land_iso2.US)
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns true
        mockHappyCase(behandling)


        opprettLovvalgsperiodeService.opprettLovvalgsperiode(1L, request)


        verify(exactly = 1) { lovvalgsperiodeRepository.save(capture(slotLovvalgsperiode)) }
        slotLovvalgsperiode.captured.shouldNotBeNull()
        slotLovvalgsperiode.captured.medlemskapstype.shouldBe(Medlemskapstyper.DELVIS_UNNTATT)
        slotLovvalgsperiode.captured.dekning.shouldBe(Trygdedekninger.UNNTATT_CAN_7_5_B)
    }

    @Test
    fun opprettLovvalgsperiode_unntaksregistreringsflytCAN_ART11_UTEN_DEKNING_lagrerKorrekt() {
        val request = requestForUnntaksregistreringFlyt(Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART11, Trygdedekninger.UTEN_DEKNING)
        val behandling = lagBehandling(Land_iso2.US)
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns true
        mockHappyCase(behandling)


        opprettLovvalgsperiodeService.opprettLovvalgsperiode(1L, request)


        verify(exactly = 1) { lovvalgsperiodeRepository.save(capture(slotLovvalgsperiode)) }
        slotLovvalgsperiode.captured.shouldNotBeNull()
        slotLovvalgsperiode.captured.medlemskapstype.shouldBe(Medlemskapstyper.UNNTATT)
        slotLovvalgsperiode.captured.dekning.shouldBe(Trygdedekninger.UTEN_DEKNING)
    }

    @Test
    fun opprettLovvalgsperiode_unntaksregistreringsflytCAN_ART11_UNNTATT_CAN_7_5_B_lagrerKorrekt() {
        val request = requestForUnntaksregistreringFlyt(Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART11, Trygdedekninger.UNNTATT_CAN_7_5_B)
        val behandling = lagBehandling(Land_iso2.US)
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns true
        mockHappyCase(behandling)


        opprettLovvalgsperiodeService.opprettLovvalgsperiode(1L, request)


        verify(exactly = 1) { lovvalgsperiodeRepository.save(capture(slotLovvalgsperiode)) }
        slotLovvalgsperiode.captured.shouldNotBeNull()
        slotLovvalgsperiode.captured.medlemskapstype.shouldBe(Medlemskapstyper.DELVIS_UNNTATT)
        slotLovvalgsperiode.captured.dekning.shouldBe(Trygdedekninger.UNNTATT_CAN_7_5_B)
    }

    @Test
    fun opprettLovvalgsperiode_unntaksregistreringsflytCAN_ART11_uten_trygdedekning_kasterFeil() {
        val request = requestForUnntaksregistreringFlyt(Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART11, null)
        val behandling = lagBehandling(Land_iso2.US)
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns true
        mockHappyCase(behandling)


        shouldThrow<FunksjonellException> { opprettLovvalgsperiodeService.opprettLovvalgsperiode(1L, request) }
            .shouldHaveMessage(
                "Kan ikke opprette lovvalgsperiode for unntaksregistrering med lovvalgsbestemmelse: " +
                    Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART11.kode + " uten manuelt registrert trygdedekning"
            )
    }

    @Test
    fun opprettLovvalgsperiode_unntaksregistreringsflytUSA_ART5_2_lagrerKorrekt() {
        val request = requestForUnntaksregistreringFlyt(Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_2, null)
        val behandling = lagBehandling(Land_iso2.US)
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns true
        mockHappyCase(behandling)


        opprettLovvalgsperiodeService.opprettLovvalgsperiode(1L, request)


        verify(exactly = 1) { lovvalgsperiodeRepository.save(capture(slotLovvalgsperiode)) }
        slotLovvalgsperiode.captured.shouldNotBeNull()
        slotLovvalgsperiode.captured.medlemskapstype.shouldBe(Medlemskapstyper.DELVIS_UNNTATT)
        slotLovvalgsperiode.captured.dekning.shouldBe(Trygdedekninger.UNNTATT_USA_5_2_G)
    }

    @Test
    fun opprettLovvalgsperiode_unntaksregistreringsflytUSA_ART5_9_UNNTATT_USA_5_2_G_lagrerKorrekt() {
        val request = requestForUnntaksregistreringFlyt(Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_9, Trygdedekninger.UNNTATT_USA_5_2_G)
        val behandling = lagBehandling(Land_iso2.US)
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns true
        mockHappyCase(behandling)


        opprettLovvalgsperiodeService.opprettLovvalgsperiode(1L, request)


        verify(exactly = 1) { lovvalgsperiodeRepository.save(capture(slotLovvalgsperiode)) }
        slotLovvalgsperiode.captured.shouldNotBeNull()
        slotLovvalgsperiode.captured.medlemskapstype.shouldBe(Medlemskapstyper.DELVIS_UNNTATT)
        slotLovvalgsperiode.captured.dekning.shouldBe(Trygdedekninger.UNNTATT_USA_5_2_G)
    }

    @Test
    fun opprettLovvalgsperiode_unntaksregistreringsflytManglerFomDato_kasterFeil() {
        every { behandlingService.hentBehandling(any()) } returns lagBehandling(Land_iso2.NO)
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns true
        every { lovvalgsperiodeRepository.findByBehandlingsresultatId(any()) } returns emptyList()
        val request = OpprettLovvalgsperiodeRequest(null, LocalDate.now(), null, null, null)


        shouldThrow<FunksjonellException> { opprettLovvalgsperiodeService.opprettLovvalgsperiode(1L, request) }
            .shouldHaveMessage("Kan ikke opprette lovvalgsperiode for unntakregistrering uten fom-dato")
    }

    @Test
    fun opprettLovvalgsperiode_unntaksregistreringsflytTomDatoFørFomDato_kasterFeil() {
        every { behandlingService.hentBehandling(any()) } returns lagBehandling(Land_iso2.NO)
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns true
        every { lovvalgsperiodeRepository.findByBehandlingsresultatId(any()) } returns emptyList()
        val request = OpprettLovvalgsperiodeRequest(LocalDate.now(), LocalDate.now().minusMonths(2), null, null, null)


        shouldThrow<FunksjonellException> { opprettLovvalgsperiodeService.opprettLovvalgsperiode(1L, request) }
            .shouldHaveMessage("Fom-dato ${request.fomDato} er etter tom-dato ${request.tomDato}")
    }

    @Test
    fun opprettLovvalgsperiode_unntaksregistreringsflytManglerBestemmelse_kasterFeil() {
        every { behandlingService.hentBehandling(any()) } returns lagBehandling(Land_iso2.NO)
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns true
        every { lovvalgsperiodeRepository.findByBehandlingsresultatId(any()) } returns emptyList()
        val request = OpprettLovvalgsperiodeRequest(LocalDate.now(), null, null, null, null)


        shouldThrow<FunksjonellException> { opprettLovvalgsperiodeService.opprettLovvalgsperiode(1L, request) }
            .shouldHaveMessage("Kan ikke opprette lovvalgsperiode for unntakregistrering uten bestemmelse")
    }

    private fun requestForUnntaksregistreringFlyt(bestemmelse: LovvalgBestemmelse, trygdedekning: Trygdedekninger?): OpprettLovvalgsperiodeRequest =
        OpprettLovvalgsperiodeRequest(
            LocalDate.now(),
            LocalDate.now().plusMonths(6),
            bestemmelse,
            null,
            trygdedekning
        )

    @Test
    fun opprettLovvalgsperiode_ikkeYrkesaktivflyt_lagrerKorrekt() {
        val request =
            requestForIkkeYrkesaktivFlyt(InnvilgelsesResultat.INNVILGET, Lovvalgsbestemmelser_trygdeavtale_ba.BIH)
        val behandling = lagBehandling(Land_iso2.BA)
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(behandling) } returns true
        mockHappyCase(behandling)


        opprettLovvalgsperiodeService.opprettLovvalgsperiode(1L, request)


        verify(exactly = 1) { lovvalgsperiodeRepository.save(capture(slotLovvalgsperiode)) }
        val lagretLovvalgsperiode = slotLovvalgsperiode.captured
        lagretLovvalgsperiode.shouldNotBeNull()
        lagretLovvalgsperiode.fom.shouldBe(behandling.mottatteOpplysninger!!.mottatteOpplysningerData.periode.fom)
        lagretLovvalgsperiode.tom.shouldBe(behandling.mottatteOpplysninger!!.mottatteOpplysningerData.periode.tom)
        lagretLovvalgsperiode.bestemmelse.shouldBe(request.lovvalgsbestemmelse)
        lagretLovvalgsperiode.innvilgelsesresultat.shouldBe(request.innvilgelsesResultat)
        lagretLovvalgsperiode.lovvalgsland.shouldBe(Land_iso2.NO)
        lagretLovvalgsperiode.medlemskapstype.shouldBe(Medlemskapstyper.PLIKTIG)
        lagretLovvalgsperiode.dekning.shouldBe(Trygdedekninger.FULL_DEKNING)
    }

    @Test
    fun opprettLovvalgsperiode_ikkeYrkesaktivflytUtenBestemmelse_lagrerKorrekt() {
        val request = requestForIkkeYrkesaktivFlyt(InnvilgelsesResultat.AVSLAATT, null)
        val behandling = lagBehandling(Land_iso2.BA)
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(behandling) } returns true
        mockHappyCase(behandling)


        opprettLovvalgsperiodeService.opprettLovvalgsperiode(1L, request)


        verify(exactly = 1) { lovvalgsperiodeRepository.save(capture(slotLovvalgsperiode)) }
        slotLovvalgsperiode.captured.shouldNotBeNull()
        slotLovvalgsperiode.captured.bestemmelse.shouldBe(null)
    }

    @Test
    fun opprettLovvalgsperiode_ikkeYrkesaktivflytManglerInnvilgelsesresultat_kasterFeil() {
        every { behandlingService.hentBehandling(any()) } returns lagBehandling(Land_iso2.NO)
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) } returns true
        every { lovvalgsperiodeRepository.findByBehandlingsresultatId(any()) } returns emptyList()
        val request = OpprettLovvalgsperiodeRequest(null, null, null, null, null)


        shouldThrow<FunksjonellException> { opprettLovvalgsperiodeService.opprettLovvalgsperiode(1L, request) }
            .shouldHaveMessage("Kan ikke opprette lovvalgsperiode for ikke-yrkesaktive uten innvilgelsesresultat")
    }

    @Test
    fun `opprettLovvalgsperiode should create a new lovvalgsperiode if none exists`() {
        val behandlingId = 1L
        val request = OpprettLovvalgsperiodeRequest(
            lovvalgsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1,
            fomDato = LocalDate.now(),
            tomDato = LocalDate.now().plusMonths(1),
            trygdedekning = null,
            innvilgelsesResultat = null
        )
        every { lovvalgsperiodeRepository.findByBehandlingsresultatId(behandlingId) } returns emptyList()
        val behandling = Behandling.forTest {
            id = behandlingId
            fagsak {
                type = Sakstyper.EU_EOS
                status = Saksstatuser.OPPRETTET
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        }
        mockHappyCase(behandling)
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) } returns false
        every { saksbehandlingRegler.harUtsendtArbeidsTakerKunNorgeFlyt(true, Behandlingstema.UTSENDT_ARBEIDSTAKER, Land_iso2.NO) } returns true

        val lovvalgsperiodeSlot = slot<Lovvalgsperiode>()
        every { lovvalgsperiodeRepository.save(capture(lovvalgsperiodeSlot)) } answers { lovvalgsperiodeSlot.captured }

        val result = opprettLovvalgsperiodeService.opprettLovvalgsperiode(behandlingId, request)

        result.fom.shouldBe(request.fomDato)
        result.tom.shouldBe(request.tomDato)
        result.bestemmelse.shouldBe(request.lovvalgsbestemmelse)
        result.innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
        result.lovvalgsland.shouldBe(Land_iso2.NO)

        verify(exactly = 1) { lovvalgsperiodeRepository.save(capture(slotLovvalgsperiode)) }
        slotLovvalgsperiode.captured.shouldNotBeNull()
    }

    private fun requestForIkkeYrkesaktivFlyt(
        innvilgelsesResultat: InnvilgelsesResultat,
        bestemmelse: LovvalgBestemmelse?
    ): OpprettLovvalgsperiodeRequest =
        OpprettLovvalgsperiodeRequest(
            null,
            null,
            bestemmelse,
            innvilgelsesResultat,
            null
        )

    @Test
    fun opprettLovvalgsperiode_ikkeStoettetFlyt_kasterFeil() {
        every { behandlingService.hentBehandling(any()) } returns lagBehandling(Land_iso2.NO)
        every { lovvalgsperiodeRepository.findByBehandlingsresultatId(any()) } returns emptyList()
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) } returns false
        every { saksbehandlingRegler.harUtsendtArbeidsTakerKunNorgeFlyt(any(), any(), any()) } returns false

        val request = OpprettLovvalgsperiodeRequest(null, null, null, null, null)

        shouldThrow<FunksjonellException> { opprettLovvalgsperiodeService.opprettLovvalgsperiode(1L, request) }
            .shouldHaveMessage("Støtter ikke opprettelse av lovvalgsperiode for denne flyten")
    }

    @Test
    fun opprettLovvalgsperiode_flereEnnÉnLovvalgsperiode_kasterFeil() {
        every { lovvalgsperiodeRepository.findByBehandlingsresultatId(any()) } returns listOf(
            Lovvalgsperiode(),
            Lovvalgsperiode()
        )
        val request = OpprettLovvalgsperiodeRequest(null, null, null, null, null)


        shouldThrow<FunksjonellException> { opprettLovvalgsperiodeService.opprettLovvalgsperiode(1L, request) }
            .shouldHaveMessage("Fant 2 lovvalgsperioder. Forventer maks én lovvalgsperiode")
    }

    private fun mockHappyCase(behandling: Behandling) {
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { lovvalgsperiodeRepository.findByBehandlingsresultatId(behandling.id) } returns emptyList()
        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns Behandlingsresultat()
        every { lovvalgsperiodeRepository.save(any()) } returns Lovvalgsperiode()
    }

    private fun lagBehandling(land: Land_iso2): Behandling =
        Behandling.forTest {
            id = 1L
            fagsak { type = Sakstyper.TRYGDEAVTALE }
            mottatteOpplysninger = MottatteOpplysninger().apply {
                mottatteOpplysningerData = AnmodningEllerAttest().apply {
                    lovvalgsland = land
                    periode = Periode(LocalDate.now(), LocalDate.now().plusMonths(6))
                }
            }
            tema = Behandlingstema.YRKESAKTIV
        }
}
