package no.nav.melosys.service.dokument.brev.mapper

import io.getunleash.FakeUnleash
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.arkiv.Distribusjonstype
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.arkiv.SaksvedleggBestilling
import no.nav.melosys.domain.brev.*
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.aktoer.KontaktopplysningService
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.UtledMottaksdato
import no.nav.melosys.service.bruker.SaksbehandlerService
import no.nav.melosys.service.dokument.BrevmottakerService
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.dokument.DokgenTestData.FNR_BRUKER
import no.nav.melosys.service.dokument.DokgenTestData.lagBehandling
import no.nav.melosys.service.dokument.DokgenTestData.lagKontaktOpplysning
import no.nav.melosys.service.dokument.DokgenTestData.lagOrg
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.FritekstvedleggDto
import no.nav.melosys.service.dokument.brev.KopiMottakerDto
import no.nav.melosys.service.dokument.brev.SaksvedleggDto
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysninger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class DokgenServiceKtTest {

    companion object {
        const val FNR = "99887766554"
        const val ORGNR = "987654321"
        const val ANNEN_PERSON_IDENT = "21075114491"
    }

    @MockK
    private lateinit var mockDokgenConsumer: DokgenConsumer
    
    @MockK
    private lateinit var mockJoarkFasade: JoarkFasade
    
    @MockK
    private lateinit var mockKodeverkService: KodeverkService
    
    @MockK
    private lateinit var mockBehandlingsService: BehandlingService
    
    @MockK
    private lateinit var mockEregFasade: EregFasade
    
    @MockK
    private lateinit var mockPersondataFasade: PersondataFasade
    
    @MockK
    private lateinit var mockKontaktOpplysningService: KontaktopplysningService
    
    @MockK
    private lateinit var mockBehandlingsresultatService: BehandlingsresultatService
    
    @MockK
    private lateinit var mockBrevMottakerService: BrevmottakerService
    
    @MockK
    private lateinit var mockProsessinstansService: ProsessinstansService
    
    @MockK
    private lateinit var mockSaksbehandlerService: SaksbehandlerService
    
    @MockK
    private lateinit var mockUtenlandskMyndighetService: UtenlandskMyndighetService
    
    @MockK
    private lateinit var mockInnvilgelseFtrlMapper: InnvilgelseFtrlMapper
    
    @MockK
    private lateinit var mockTrygdeavtaleMapper: TrygdeavtaleMapper
    
    @MockK
    private lateinit var mockInnhentingAvInntektsopplysningerMapper: InnhentingAvInntektsopplysningerMapper
    
    @MockK
    private lateinit var mockInnvilgelseEftaStorbritanniaMapper: InnvilgelseEftaStorbritanniaMapper
    
    @MockK
    private lateinit var orienteringAnmodningUnntakMapper: OrienteringAnmodningUnntakMapper
    
    @MockK
    private lateinit var orienteringTilArbeidsgiverOmVedtakMapper: OrienteringTilArbeidsgiverOmVedtakMapper
    
    @MockK
    private lateinit var årsavregningVedtakMapper: ÅrsavregningVedtakMapper
    
    @MockK
    private lateinit var mockUtledMottaksdato: UtledMottaksdato
    
    @MockK
    private lateinit var avklarteVirksomheterService: AvklarteVirksomheterService
    
    @MockK
    private lateinit var informasjonTrygdeavgiftMapper: InformasjonTrygdeavgiftMapper

    private lateinit var dokgenService: DokgenService
    private val unleash = FakeUnleash()
    private val expectedPdf = "pdf".toByteArray()

    @BeforeEach
    fun init() {
        val dokgenMapperDatahenter = DokgenMapperDatahenter(
            mockBehandlingsresultatService, mockEregFasade, mockPersondataFasade, mockKodeverkService, avklarteVirksomheterService
        )

        dokgenService = DokgenService(
            mockDokgenConsumer, DokumentproduksjonsInfoMapper(), mockJoarkFasade,
            DokgenMalMapper(dokgenMapperDatahenter, mockInnvilgelseFtrlMapper, mockInnvilgelseEftaStorbritanniaMapper, 
                mockInnhentingAvInntektsopplysningerMapper, mockTrygdeavtaleMapper, orienteringAnmodningUnntakMapper, 
                orienteringTilArbeidsgiverOmVedtakMapper, årsavregningVedtakMapper, informasjonTrygdeavgiftMapper),
            mockBehandlingsService, mockEregFasade, mockKontaktOpplysningService,
            mockBrevMottakerService, mockProsessinstansService, mockSaksbehandlerService,
            mockUtenlandskMyndighetService, mockUtledMottaksdato, unleash
        )

        clearMocks(mockDokgenConsumer)
    }

    @Test
    fun `produserBrev feiler utilgjengelig mal`() {
        val brevbestilling = DokgenBrevbestilling.Builder<DokgenBrevbestilling>()
            .medProduserbartdokument(ATTEST_A1)
            .build()

        val exception = shouldThrow<FunksjonellException> {
            dokgenService.produserBrev(Mottaker(), brevbestilling)
        }
        
        exception.message shouldBe "ProduserbartDokument ATTEST_A1 er ikke støttet"
    }

    @Test
    fun `produserBrev til bruker ok`() {
        every { mockDokgenConsumer.lagPdf(any(), any(), eq(false), eq(false)) } returns expectedPdf
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns lagBehandling()
        every { mockPersondataFasade.hentPerson(any()) } returns lagPersonopplysninger()
        every { mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "0123") } returns "Aker"
        every { mockKodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NO") } returns "Norge"

        val mottaker = lagBruker()
        val brevbestilling = MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_BRUKER)
            .medBehandlingId(123)
            .build()

        val pdfResponse = dokgenService.produserBrev(mottaker, brevbestilling)

        pdfResponse.shouldNotBeNull()
        pdfResponse shouldBe expectedPdf

        verify { mockDokgenConsumer.lagPdf(any(), any(), eq(false), eq(false)) }
        verify(exactly = 0) { mockEregFasade.hentOrganisasjon(any()) }
        verify(exactly = 0) { mockKontaktOpplysningService.hentKontaktopplysning(any(), any()) }
    }

    @Test
    fun `produserBrev henter dato fra UtledMottaksdato`() {
        every { mockDokgenConsumer.lagPdf(any(), any(), eq(false), eq(false)) } returns expectedPdf
        val journalpost = lagJournalpost()
        every { mockJoarkFasade.hentJournalpost("journalpostId") } returns journalpost
        val behandling = lagBehandling().apply {
            initierendeJournalpostId = "journalpostId"
        }
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns behandling
        every { mockPersondataFasade.hentPerson(any()) } returns lagPersonopplysninger()
        every { mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "0123") } returns "Aker"
        every { mockKodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NO") } returns "Norge"
        every { mockUtledMottaksdato.getMottaksdato(any(), any()) } returns LocalDate.now()

        val mottaker = lagBruker()
        val brevbestilling = MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_BRUKER)
            .medBehandlingId(123)
            .build()

        val pdfResponse = dokgenService.produserBrev(mottaker, brevbestilling)

        pdfResponse.shouldNotBeNull()
        pdfResponse shouldBe expectedPdf

        verify { mockDokgenConsumer.lagPdf(any(), any(), eq(false), eq(false)) }
        verify { mockUtledMottaksdato.getMottaksdato(behandling, journalpost) }
        verify(exactly = 0) { mockEregFasade.hentOrganisasjon(any()) }
        verify(exactly = 0) { mockKontaktOpplysningService.hentKontaktopplysning(any(), any()) }
    }

    @Test
    fun `produserBrev til fullmektig organisasjon ok`() {
        every { mockDokgenConsumer.lagPdf(any(), any(), eq(false), eq(false)) } returns expectedPdf
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns lagBehandling()
        every { mockPersondataFasade.hentPerson(any()) } returns lagPersonopplysninger()
        every { mockEregFasade.hentOrganisasjon(any()) } returns lagSaksopplysning()
        every { mockKontaktOpplysningService.hentKontaktopplysning(any(), any()) } returns Optional.of(lagKontaktOpplysning())
        every { mockUtledMottaksdato.getMottaksdato(any(), any()) } returns LocalDate.now()
        every { mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "9990") } returns "Aker"
        every { mockKodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NO") } returns "Norge"
        
        val mottaker = lagFullmektig(ORGNR)
        val brevbestilling = DokgenBrevbestilling.Builder<DokgenBrevbestilling>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandlingId(123)
            .build()

        val pdfResponse = dokgenService.produserBrev(mottaker, brevbestilling)

        pdfResponse.shouldNotBeNull()
        pdfResponse shouldBe expectedPdf

        verify { mockDokgenConsumer.lagPdf(any(), any(), eq(false), eq(false)) }
        verify { mockEregFasade.hentOrganisasjon(any()) }
        verify { mockKontaktOpplysningService.hentKontaktopplysning(any(), any()) }
    }

    @Test
    fun `produserBrev til fullmektig person ok`() {
        every { mockDokgenConsumer.lagPdf(any(), any(), eq(false), eq(false)) } returns expectedPdf
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns lagBehandling()
        every { mockPersondataFasade.hentPerson(any()) } returns lagPersonopplysninger()
        every { mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "0123") } returns "Aker"
        every { mockKodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NO") } returns "Norge"
        every { mockUtledMottaksdato.getMottaksdato(any(), any()) } returns LocalDate.now()

        val brevbestilling = DokgenBrevbestilling.Builder<DokgenBrevbestilling>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandlingId(123)
            .build()

        val pdfResponse = dokgenService.produserBrev(lagFullmektig(FNR), brevbestilling)

        pdfResponse.shouldNotBeNull()
        pdfResponse shouldBe expectedPdf

        verify { mockDokgenConsumer.lagPdf(any(), any(), eq(false), eq(false)) }
        verify(exactly = 2) { mockPersondataFasade.hentPerson(any()) }
    }

    @Test
    fun `produserUtkast uten fullmektig for bruker ok`() {
        every { mockDokgenConsumer.lagPdf(any(), any(), eq(false), eq(true)) } returns expectedPdf
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns lagBehandling()
        every { mockPersondataFasade.hentPerson(any()) } returns lagPersonopplysninger()
        every { mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "0123") } returns "Aker"
        every { mockKodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NO") } returns "Norge"
        every { mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(true), eq(false)) } returns listOf(lagBruker())
        every { mockUtledMottaksdato.getMottaksdato(any(), any()) } returns LocalDate.now()
        every { mockSaksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler navn"

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD
            mottaker = Mottakerroller.BRUKER
            bestillersId = "Z123456"
        }

        val pdfResponse = dokgenService.produserUtkast(123L, brevbestillingDto)

        pdfResponse.shouldNotBeNull()
        pdfResponse shouldBe expectedPdf

        verify { mockDokgenConsumer.lagPdf(any(), any(), eq(false), eq(true)) }
        verify { mockSaksbehandlerService.hentNavnForIdent(any()) }
        verify(exactly = 0) { mockEregFasade.hentOrganisasjon(any()) }
        verify(exactly = 0) { mockKontaktOpplysningService.hentKontaktopplysning(any(), any()) }
    }

    @Test
    fun `produserUtkast til fullmektig for bruker ok`() {
        every { mockDokgenConsumer.lagPdf(any(), any(), eq(false), eq(true)) } returns expectedPdf
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns lagBehandling()
        every { mockPersondataFasade.hentPerson(any()) } returns lagPersonopplysninger()
        every { mockEregFasade.hentOrganisasjon(any()) } returns lagSaksopplysning()
        every { mockKontaktOpplysningService.hentKontaktopplysning(any(), any()) } returns Optional.of(lagKontaktOpplysning())
        every { mockUtledMottaksdato.getMottaksdato(any(), any()) } returns LocalDate.now()
        every { mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "9990") } returns "Aker"
        every { mockKodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NO") } returns "Norge"
        every { mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(true), eq(false)) } returns listOf(lagFullmektig(ORGNR))
        every { mockSaksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler navn"

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD
            mottaker = Mottakerroller.BRUKER
            bestillersId = "Z123456"
        }

        val pdfResponse = dokgenService.produserUtkast(123L, brevbestillingDto)

        pdfResponse.shouldNotBeNull()
        pdfResponse shouldBe expectedPdf

        verify { mockDokgenConsumer.lagPdf(any(), any(), eq(false), eq(true)) }
        verify { mockEregFasade.hentOrganisasjon(ORGNR) }
        verify { mockKontaktOpplysningService.hentKontaktopplysning(any(), any()) }
    }

    @Test
    fun `produserUtkast til fullmektig for arbeidsgiver ok`() {
        every { mockDokgenConsumer.lagPdf(any(), any(), eq(false), eq(true)) } returns expectedPdf
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns lagBehandling()
        every { mockPersondataFasade.hentPerson(any()) } returns lagPersonopplysninger()
        every { mockEregFasade.hentOrganisasjon(any()) } returns lagSaksopplysning()
        every { mockKontaktOpplysningService.hentKontaktopplysning(any(), any()) } returns Optional.of(lagKontaktOpplysning())
        every { mockUtledMottaksdato.getMottaksdato(any(), any()) } returns LocalDate.now()
        every { mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "9990") } returns "Aker"
        every { mockKodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NO") } returns "Norge"
        every { mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(true), eq(false)) } returns listOf(lagFullmektig(ORGNR))
        every { mockSaksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler navn"

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD
            mottaker = Mottakerroller.ARBEIDSGIVER
            bestillersId = "Z123456"
        }

        val pdfResponse = dokgenService.produserUtkast(123L, brevbestillingDto)

        pdfResponse.shouldNotBeNull()
        pdfResponse shouldBe expectedPdf

        verify { mockDokgenConsumer.lagPdf(any(), any(), eq(false), eq(true)) }
        verify { mockEregFasade.hentOrganisasjon(ORGNR) }
        verify { mockKontaktOpplysningService.hentKontaktopplysning(any(), any()) }
    }

    @Test
    fun `skal produsere og distribuere brev til bruker`() {
        val bruker = Mottaker.medRolle(Mottakerroller.BRUKER)

        every { mockSaksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler, Ole"
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns BehandlingTestFactory.builderWithDefaults().build()
        every { mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false)) } returns listOf(bruker)
        every { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just Runs

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MANGELBREV_BRUKER
            mottaker = Mottakerroller.BRUKER
            standardvedleggType = StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE
            bestillersId = "Z123456"
        }

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto)

        val brevbestillingSlot = slot<DokgenBrevbestilling>()
        verify { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any<Behandling>(), any<Mottaker>(), capture(brevbestillingSlot)) }
        verify { mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false)) }
        verify { mockSaksbehandlerService.hentNavnForIdent(any()) }

        val brevbestilling = brevbestillingSlot.captured as MangelbrevBrevbestilling
        brevbestilling.shouldNotBeNull()
        assertSoftly {
            brevbestilling.produserbartdokument shouldBe MANGELBREV_BRUKER
            brevbestilling.behandlingId shouldBe 123L
            brevbestilling.saksbehandlerNavn shouldBe "Saksbehandler, Ole"
            brevbestilling.standardvedleggType shouldBe StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE
        }
    }

    @Test
    fun `skal produsere og distribuere brev til orgnr uten kopi`() {
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns BehandlingTestFactory.builderWithDefaults().build()
        every { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just Runs

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD
            mottaker = Mottakerroller.ARBEIDSGIVER
            orgnr = ORGNR
            bestillersId = "Z123456"
        }

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto)

        val brevbestillingSlot = slot<DokgenBrevbestilling>()
        verify { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any<Behandling>(), any<Mottaker>(), capture(brevbestillingSlot)) }
        verify(exactly = 0) { mockBrevMottakerService.avklarMottakere(any(), any(), any(), any(), any()) }

        val brevbestilling = brevbestillingSlot.captured
        brevbestilling.shouldNotBeNull()
        assertSoftly {
            brevbestilling.produserbartdokument shouldBe MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD
            brevbestilling.behandlingId shouldBe 123L
        }
    }

    @Test
    fun `skal produsere og distribuere brev til annen organisasjon gir riktig mottaker`() {
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns BehandlingTestFactory.builderWithDefaults().build()
        every { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just Runs

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD
            mottaker = Mottakerroller.ANNEN_ORGANISASJON
            orgnr = ORGNR
            bestillersId = "Z123456"
        }

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto)

        val forventetMottaker = Mottaker.medRolle(Mottakerroller.ANNEN_ORGANISASJON).apply {
            orgnr = ORGNR
        }
        
        verify { 
            mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(
                any<Behandling>(), 
                eq(forventetMottaker),
                any<DokgenBrevbestilling>()
            ) 
        }
    }

    @Test
    fun `skal produsere og distribuere brev til annen person gir riktig mottaker`() {
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns BehandlingTestFactory.builderWithDefaults().build()
        every { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just Runs

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD
            mottaker = Mottakerroller.ANNEN_PERSON
            annenPersonMottakerIdent = ANNEN_PERSON_IDENT
            bestillersId = "Z123456"
        }

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto)

        val forventetMottaker = Mottaker.medRolle(Mottakerroller.ANNEN_PERSON).apply {
            personIdent = ANNEN_PERSON_IDENT
        }
        
        verify { 
            mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(
                any<Behandling>(), 
                eq(forventetMottaker),
                any<DokgenBrevbestilling>()
            ) 
        }
    }

    @Test
    fun `skal produsere og distribuere brev til orgnr med kopi`() {
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns BehandlingTestFactory.builderWithDefaults().build()
        every { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just Runs

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MANGELBREV_BRUKER
            mottaker = Mottakerroller.ARBEIDSGIVER
            orgnr = ORGNR
            manglerFritekst = "Mangler"
            bestillersId = "Z123456"
            kopiMottakere = listOf(KopiMottakerDto(Mottakerroller.BRUKER, null, "1223", null))
        }

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto)

        val brevbestillingSlot = slot<DokgenBrevbestilling>()
        verify(exactly = 2) { 
            mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(
                any<Behandling>(),
                any<Mottaker>(), 
                capture(brevbestillingSlot)
            ) 
        }
        verify(exactly = 0) { mockBrevMottakerService.avklarMottakere(any(), any(), any(), any(), any()) }

        val brevbestilling = brevbestillingSlot.captured as MangelbrevBrevbestilling
        brevbestilling.shouldNotBeNull()
        assertSoftly {
            brevbestilling.produserbartdokument shouldBe MANGELBREV_BRUKER
            brevbestilling.behandlingId shouldBe 123L
            brevbestilling.manglerInfoFritekst shouldBe "Mangler"
        }
    }

    @Test
    fun `produserOgDistribuerBrev skal distribuere brev med vedlegg når brevbestilling inneholder vedlegg`() {
        val bruker = Mottaker.medRolle(Mottakerroller.BRUKER)

        every { mockSaksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler, Ole"
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns BehandlingTestFactory.builderWithDefaults().build()
        every { mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false)) } returns listOf(bruker)
        every { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just Runs
        
        val saksvedleggDto = listOf(
            SaksvedleggDto("100", "200"),
            SaksvedleggDto("300", "400")
        )
        val fritekstvedleggDto = listOf(
            FritekstvedleggDto("tittel1", "fritekst1"),
            FritekstvedleggDto("tittel2", "fritekst2")
        )

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = GENERELT_FRITEKSTBREV_BRUKER
            saksVedlegg = saksvedleggDto
            fritekstvedlegg = fritekstvedleggDto
            bestillersId = "Z123456"
        }

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto)

        val brevbestillingSlot = slot<DokgenBrevbestilling>()
        verify { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any<Behandling>(), any<Mottaker>(), capture(brevbestillingSlot)) }
        verify { mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false)) }
        verify { mockSaksbehandlerService.hentNavnForIdent(any()) }

        val brevbestilling = brevbestillingSlot.captured as FritekstbrevBrevbestilling
        brevbestilling.saksvedleggBestilling shouldHaveSize 2
        brevbestilling.fritekstvedleggBestilling shouldHaveSize 2
    }

    @Test
    fun `produserOgDistribuerBrev skal distribuere brev med distribusjonstype når fritekstbrev`() {
        val bruker = Mottaker.medRolle(Mottakerroller.BRUKER)

        every { mockSaksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler, Ole"
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns BehandlingTestFactory.builderWithDefaults().build()
        every { mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false)) } returns listOf(bruker)
        every { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just Runs
        
        val saksvedleggDto = listOf(
            SaksvedleggDto("100", "200"),
            SaksvedleggDto("300", "400")
        )
        val fritekstvedleggDto = listOf(
            FritekstvedleggDto("tittel1", "fritekst1"),
            FritekstvedleggDto("tittel2", "fritekst2")
        )

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = GENERELT_FRITEKSTBREV_BRUKER
            distribusjonstype = Distribusjonstype.ANNET
            saksVedlegg = saksvedleggDto
            fritekstvedlegg = fritekstvedleggDto
            bestillersId = "Z123456"
        }

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto)

        val brevbestillingSlot = slot<DokgenBrevbestilling>()
        verify { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any<Behandling>(), any<Mottaker>(), capture(brevbestillingSlot)) }

        val brevbestilling = brevbestillingSlot.captured as FritekstbrevBrevbestilling
        brevbestilling.distribusjonstype shouldBe Distribusjonstype.ANNET
    }

    @Test
    fun `produserOgDistribuerBrev bruker skal ha kopi setter felt korrekt`() {
        val arbeidsgiver = Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER)
        every { mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false)) } returns listOf(arbeidsgiver)
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns BehandlingTestFactory.builderWithDefaults().build()
        every { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just Runs

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = GENERELT_FRITEKSTBREV_BRUKER
            kopiMottakere = listOf(KopiMottakerDto(Mottakerroller.BRUKER, null, null, null))
        }

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto)

        val brevbestillingSlot = slot<DokgenBrevbestilling>()
        verify(exactly = 2) { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any<Behandling>(), any<Mottaker>(), capture(brevbestillingSlot)) }
        
        val brevbestilling = brevbestillingSlot.captured as FritekstbrevBrevbestilling
        brevbestilling.isBrukerSkalHaKopi shouldBe true
    }

    @Test
    fun `produserOgDistribuerBrev utenlandsk trygdemyndighet oppretter prosessinstans med forventet mottaker`() {
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns BehandlingTestFactory.builderWithDefaults().build()
        every { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just Runs

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV
            institusjonID = "GB"
            mottaker = Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET
        }

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto)

        val mottakerSlot = slot<Mottaker>()
        verify(exactly = 1) { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any<Behandling>(), capture(mottakerSlot), any()) }
        
        val mottaker = mottakerSlot.captured
        mottaker.institusjonID shouldBe "GB"
        mottaker.rolle shouldBe Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET
    }

    @Test
    fun `produserOgDistribuerBrev bruker skal ikke ha kopi setter felt korrekt`() {
        val arbeidsgiver = Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER)
        every { mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false)) } returns listOf(arbeidsgiver)
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns BehandlingTestFactory.builderWithDefaults().build()
        every { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just Runs

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MANGELBREV_ARBEIDSGIVER
            distribusjonstype = Distribusjonstype.ANNET
        }

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto)

        val brevbestillingSlot = slot<DokgenBrevbestilling>()
        verify { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any<Behandling>(), any<Mottaker>(), capture(brevbestillingSlot)) }
        
        val brevbestilling = brevbestillingSlot.captured as MangelbrevBrevbestilling
        brevbestilling.isBrukerSkalHaKopi shouldBe false
    }

    @Test
    fun `skal produsere og distribuere brev til fullmektig privatperson med kopi`() {
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns BehandlingTestFactory.builderWithDefaults().build()
        every { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just Runs

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MANGELBREV_ARBEIDSGIVER
            mottaker = Mottakerroller.ARBEIDSGIVER
            orgnr = ORGNR
            manglerFritekst = "Mangler"
            bestillersId = "Z123456"
            kopiMottakere = listOf(KopiMottakerDto(Mottakerroller.FULLMEKTIG, null, null, null))
        }

        val mottaker = Mottaker(Mottakerroller.FULLMEKTIG, null, "12345678999", null, null, Land_iso2.NO)
        every { mockBrevMottakerService.avklarMottaker(any(), any(), any()) } returns mottaker

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto)

        val brevbestillingSlot = slot<DokgenBrevbestilling>()
        verify(exactly = 1) { 
            mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(
                any<Behandling>(),
                eq(mottaker), 
                capture(brevbestillingSlot)
            ) 
        }

        val brevbestilling = brevbestillingSlot.captured as MangelbrevBrevbestilling
        assertSoftly {
            brevbestilling.produserbartdokument shouldBe MANGELBREV_ARBEIDSGIVER
            brevbestilling.behandlingId shouldBe 123L
            brevbestilling.manglerInfoFritekst shouldBe "Mangler"
            brevbestilling.isBestillKopi shouldBe true
        }
    }

    @Test
    fun `er tilgjengelig dokgenmal`() {
        dokgenService.erTilgjengeligDokgenmal(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD) shouldBe true
        dokgenService.erTilgjengeligDokgenmal(ATTEST_A1) shouldBe false
    }

    @Test
    fun `skal hente dokument info`() {
        val dokumentproduksjonsInfo = dokgenService.hentDokumentInfo(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)

        dokumentproduksjonsInfo.dokgenMalnavn() shouldBe "saksbehandlingstid_soknad"
        dokumentproduksjonsInfo.dokumentKategoriKode() shouldBe "IB"
        dokumentproduksjonsInfo.journalføringsTittel() shouldBe "Melding om forventet saksbehandlingstid"
    }

    @Test
    fun `produserOgDistribuerBrev kopimottaker utenlandsk trygdemyndighet får null standardvedlegg`() {
        val behandling = lagBehandling()
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns behandling
        every { mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false)) } returns listOf(Mottaker.medRolle(Mottakerroller.BRUKER))
        every { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just Runs

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = TRYGDEAVTALE_GB
            mottaker = Mottakerroller.BRUKER
            bestillersId = "Z123456"
            standardvedleggType = StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE
            kopiMottakere = listOf(KopiMottakerDto(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET, "123456789", null, "institusjonID"))
        }

        unleash.enable(ToggleName.STANDARDVEDLEGG_EGET_VEDLEGG_AVTALELAND)

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto)

        val brevbestillingSlot = mutableListOf<DokgenBrevbestilling>()
        verify(exactly = 2) { 
            mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(
                any<Behandling>(), 
                any<Mottaker>(), 
                capture(brevbestillingSlot)
            ) 
        }

        brevbestillingSlot[0].standardvedleggType shouldBe StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE
        brevbestillingSlot[0].isBestillKopi shouldBe false

        brevbestillingSlot[1].standardvedleggType.shouldBeNull()
        brevbestillingSlot[1].isBestillKopi shouldBe true
    }

    @Test
    fun `produserOgDistribuerBrev fullmektig privatperson kopimottaker bruker avklarMottaker`() {
        val behandling = lagBehandling()
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns behandling
        every { mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false)) } returns listOf(Mottaker.medRolle(Mottakerroller.BRUKER))
        every { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just Runs

        val fullmektigMottaker = Mottaker(Mottakerroller.FULLMEKTIG, null, "12345678999", null, null, Land_iso2.NO)
        every { mockBrevMottakerService.avklarMottaker(any(), any(), any()) } returns fullmektigMottaker

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD
            mottaker = Mottakerroller.BRUKER
            kopiMottakere = listOf(KopiMottakerDto(Mottakerroller.FULLMEKTIG, null, null, null))
        }

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto)

        verify { 
            mockBrevMottakerService.avklarMottaker(
                eq(brevbestillingDto.produserbardokument),
                match { it.rolle == Mottakerroller.FULLMEKTIG },
                eq(behandling)
            )
        }

        val brevbestillingSlot = mutableListOf<DokgenBrevbestilling>()
        verify(exactly = 2) { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any<Behandling>(), any<Mottaker>(), capture(brevbestillingSlot)) }

        brevbestillingSlot[1].standardvedleggType.shouldBeNull()
        brevbestillingSlot[1].isBestillKopi shouldBe true
    }

    @Test
    fun `produserOgDistribuerBrev non fullmektig kopimottaker håndteres korrekt`() {
        val behandling = lagBehandling()
        every { mockBehandlingsService.hentBehandlingMedSaksopplysninger(any()) } returns behandling
        every { mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false)) } returns listOf(Mottaker.medRolle(Mottakerroller.BRUKER))
        every { mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just Runs

        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD
            mottaker = Mottakerroller.BRUKER
            kopiMottakere = listOf(
                KopiMottakerDto(Mottakerroller.ARBEIDSGIVER, "123456789", null, null)
            )
        }

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto)

        verify(exactly = 0) { mockBrevMottakerService.avklarMottaker(any(), any(), any()) }

        val mottakerSlot = mutableListOf<Mottaker>()
        val brevbestillingSlot = mutableListOf<DokgenBrevbestilling>()
        verify(exactly = 2) { 
            mockProsessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(
                any<Behandling>(), 
                capture(mottakerSlot), 
                capture(brevbestillingSlot)
            ) 
        }

        mottakerSlot[1].rolle shouldBe Mottakerroller.ARBEIDSGIVER
        mottakerSlot[1].orgnr shouldBe "123456789"

        brevbestillingSlot[1].standardvedleggType.shouldBeNull()
        brevbestillingSlot[1].isBestillKopi shouldBe true
    }

    private fun lagJournalpost(): Journalpost {
        return Journalpost("1234").apply {
            forsendelseMottatt = Instant.now()
            avsenderNavn = "Mr. Avsender"
            avsenderId = FNR
        }
    }

    private fun lagSaksopplysning(): Saksopplysning {
        return Saksopplysning().apply {
            dokument = lagOrg()
        }
    }

    private fun lagBruker(): Mottaker {
        return Mottaker.medRolle(Mottakerroller.BRUKER).apply {
            aktørId = FNR_BRUKER
        }
    }

    private fun lagFullmektig(mottakerID: String): Mottaker {
        return Mottaker.medRolle(Mottakerroller.FULLMEKTIG).apply {
            when (mottakerID) {
                ORGNR -> orgnr = ORGNR
                FNR -> personIdent = FNR
            }
        }
    }
}