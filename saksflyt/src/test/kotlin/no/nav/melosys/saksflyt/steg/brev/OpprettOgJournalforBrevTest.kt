package no.nav.melosys.saksflyt.steg.brev

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import no.nav.melosys.domain.*
import no.nav.melosys.domain.arkiv.*
import no.nav.melosys.domain.brev.*
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.dokgen.dto.standardvedlegg.InnvilgelseRettigheterPlikterStandardvedlegg
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.saksflyt.TestdataFactory
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.brev.DokumentNavnService
import no.nav.melosys.service.dokument.BrevmottakerService
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.dokument.DokumentHentingService
import no.nav.melosys.service.dokument.brev.mapper.DokumentproduksjonsInfoMapper
import no.nav.melosys.service.dokument.brev.mapper.standardvedlegg.RettigheterOgPlikterStandardvedleggMapper
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.oppgave.OppgaveFactory
import no.nav.melosys.service.persondata.PersondataFasade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpprettOgJournalforBrevTest {

    private lateinit var mockBehandlingService: BehandlingService
    private lateinit var mockDokgenService: DokgenService
    private lateinit var mockUtenlandskMyndighetService: UtenlandskMyndighetService
    private lateinit var mockJoarkFasade: JoarkFasade
    private lateinit var mockEregFasade: EregFasade
    private lateinit var mockPersondataFasade: PersondataFasade
    private lateinit var mockDokumentHentingService: DokumentHentingService
    private lateinit var mockLovvalgsperiodeService: LovvalgsperiodeService
    private lateinit var mockRettigheterOgPlikterStandardvedleggMapper: RettigheterOgPlikterStandardvedleggMapper

    private lateinit var dokumentNavnService: DokumentNavnService
    private lateinit var opprettJournalforBrev: OpprettOgJournalforBrev

    private val oppgaveFactory = OppgaveFactory()
    private val prosessinstansUuid = UUID.randomUUID()
    private val opprettJournalpostCaptor = slot<OpprettJournalpost>()

    @BeforeEach
    fun init() {
        clearAllMocks()
        mockBehandlingService = mockk(relaxed = true)
        mockDokgenService = mockk(relaxed = true)
        mockUtenlandskMyndighetService = mockk(relaxed = true)
        mockJoarkFasade = mockk(relaxed = true)
        mockEregFasade = mockk(relaxed = true)
        mockPersondataFasade = mockk(relaxed = true)
        mockDokumentHentingService = mockk(relaxed = true)
        mockLovvalgsperiodeService = mockk(relaxed = true)
        mockRettigheterOgPlikterStandardvedleggMapper = mockk(relaxed = true)

        dokumentNavnService = DokumentNavnService(
            mockk<BrevmottakerService>(relaxed = true),
            mockk<DokgenService>(relaxed = true),
            mockLovvalgsperiodeService,
            mockk<MedlemskapsperiodeService>(relaxed = true)
        )
        opprettJournalforBrev = OpprettOgJournalforBrev(
            mockBehandlingService,
            mockDokgenService,
            mockRettigheterOgPlikterStandardvedleggMapper,
            mockUtenlandskMyndighetService,
            mockJoarkFasade,
            mockPersondataFasade,
            mockEregFasade,
            dokumentNavnService,
            mockDokumentHentingService,
            oppgaveFactory
        )
    }

    @Test
    fun `utfør skal feile ved manglende behandling`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.AKTØR_ID, "12345678901")
        }


        val exception = shouldThrow<FunksjonellException> {
            opprettJournalforBrev.utfør(prosessinstans)
        }


        exception.message shouldBe "Prosessinstans mangler behandling"
    }

    @Test
    fun `utfør skal feile ved manglende mottaker`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling {
                fagsak = TestdataFactory.lagBehandling().fagsak
                type = Behandlingstyper.FØRSTEGANG
            }
        }


        val exception = shouldThrow<FunksjonellException> {
            opprettJournalforBrev.utfør(prosessinstans)
        }


        exception.message shouldBe "Mangler mottaker"
    }

    @Test
    fun `utfør skal opprette og journalføre brev til bruker`() {
        val behandling = TestdataFactory.lagBehandling()
        every { mockBehandlingService.hentBehandling(any()) } returns behandling
        every { mockJoarkFasade.opprettJournalpost(any(), any()) } returns "12234"
        every { mockDokgenService.hentDokumentInfo(any()) } returns TestdataFactory.lagDokumentInfo()

        val brevbestilling = DokgenBrevbestilling.Builder()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .build()
        val prosessinstans = lagProsessinstans(behandling, brevbestilling)


        opprettJournalforBrev.utfør(prosessinstans)


        verify(exactly = 2) { mockPersondataFasade.hentFolkeregisterident(any()) }
        verify { mockBehandlingService.hentBehandling(any()) }
        verify { mockDokgenService.produserBrev(any<Mottaker>(), any<DokgenBrevbestilling>()) }
        verify { mockJoarkFasade.opprettJournalpost(any(), any()) }
    }

    @Test
    fun `utfør skal opprette og journalføre brev til virksomhet`() {
        val virksomhet = Aktoer().apply {
            orgnr = "orgnr"
            rolle = Aktoersroller.VIRKSOMHET
        }
        val behandling = Behandling.forTest {
            fagsak {
                aktører.add(virksomhet)
            }
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.YRKESAKTIV
            id = 1L
        }
        every { mockBehandlingService.hentBehandling(behandling.id) } returns behandling
        every { mockJoarkFasade.opprettJournalpost(capture(opprettJournalpostCaptor), any()) } returns "12234"
        every { mockDokgenService.hentDokumentInfo(any()) } returns TestdataFactory.lagDokumentInfo()
        every { mockEregFasade.hentOrganisasjonNavn(virksomhet.orgnr) } returns "organisasjonsnavn"

        val brevbestilling = FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_VIRKSOMHET)
            .medFritekstTittel("Tittel")
            .build()
        val prosessinstans = lagProsessinstansMedMottaker(behandling, Mottaker.av(virksomhet), brevbestilling)


        opprettJournalforBrev.utfør(prosessinstans)


        verify(exactly = 0) { mockPersondataFasade.hentFolkeregisterident(any()) }
        verify { mockBehandlingService.hentBehandling(any()) }
        verify { mockDokgenService.produserBrev(any<Mottaker>(), any<DokgenBrevbestilling>()) }
        verify { mockJoarkFasade.opprettJournalpost(any(), any()) }

        opprettJournalpostCaptor.captured.run {
            brukerId shouldBe virksomhet.orgnr
            brukerIdType shouldBe BrukerIdType.ORGNR
            korrespondansepartId shouldBe virksomhet.orgnr
            korrespondansepartNavn shouldBe "organisasjonsnavn"
            korrespondansepartIdType shouldBe OpprettJournalpost.KorrespondansepartIdType.ORGNR.kode
            eksternReferanseId shouldBe prosessinstansUuid.toString()
        }
    }

    @Test
    fun `utfør skal opprette og journalføre brev til fullmektig`() {
        val behandling = TestdataFactory.lagBehandling()
        every { mockBehandlingService.hentBehandling(any()) } returns behandling
        every { mockDokgenService.hentDokumentInfo(any()) } returns TestdataFactory.lagDokumentInfo()
        every { mockJoarkFasade.opprettJournalpost(any(), any()) } returns "12234"
        every { mockEregFasade.hentOrganisasjonNavn(any()) } returns "Advokatene AS"

        val mottaker = Mottaker.medRolle(Mottakerroller.FULLMEKTIG).apply {
            orgnr = "987654321"
        }
        val brevbestilling = DokgenBrevbestilling.Builder()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .build()
        val prosessinstans = lagProsessinstansMedOrgnr(behandling, mottaker, brevbestilling)


        opprettJournalforBrev.utfør(prosessinstans)


        verify { mockBehandlingService.hentBehandling(any()) }
        verify { mockDokgenService.produserBrev(any<Mottaker>(), any<DokgenBrevbestilling>()) }
        verify { mockJoarkFasade.opprettJournalpost(any(), any()) }
    }

    @Test
    fun `utfør skal ha korrekt tittel for Storbritannia innvilgelse til bruker`() {
        val behandling = TestdataFactory.lagBehandling()
        every { mockBehandlingService.hentBehandling(any()) } returns behandling
        every { mockJoarkFasade.opprettJournalpost(capture(opprettJournalpostCaptor), any()) } returns "12234"
        val dokumentproduksjonsInfoMapper = DokumentproduksjonsInfoMapper()
        every { mockDokgenService.hentDokumentInfo(any()) } returns dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(TRYGDEAVTALE_GB)
        every { mockLovvalgsperiodeService.hentLovvalgsperiode(any()) } returns Lovvalgsperiode()
        val mottaker = lagMottaker("12234")

        val brevbestilling = InnvilgelseBrevbestilling.Builder()
            .medBehandlingId(1L)
            .medProduserbartdokument(TRYGDEAVTALE_GB)
            .build()
        val prosessinstans = lagProsessinstansMedMottaker(behandling, mottaker, brevbestilling)


        opprettJournalforBrev.utfør(prosessinstans)


        verify { mockJoarkFasade.opprettJournalpost(any(), any()) }

        opprettJournalpostCaptor.captured.hoveddokument.tittel shouldBe "Vedtak om medlemskap, Attest for medlemskap i folketrygden"
    }

    @Test
    fun `utfør skal opprette og journalføre mangelbrev til bruker`() {
        val behandling = TestdataFactory.lagBehandling()
        every { mockBehandlingService.hentBehandling(any()) } returns behandling
        every { mockJoarkFasade.opprettJournalpost(any(), any()) } returns "12234"
        every { mockDokgenService.hentDokumentInfo(any()) } returns TestdataFactory.lagDokumentInfo()

        val brevbestilling = MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_BRUKER)
            .medManglerInfoFritekst("Mangler")
            .build()
        val prosessinstans = lagProsessinstans(behandling, brevbestilling)


        opprettJournalforBrev.utfør(prosessinstans)


        verify { mockBehandlingService.hentBehandling(any()) }
        verify { mockDokgenService.produserBrev(any<Mottaker>(), any<MangelbrevBrevbestilling>()) }
        verify { mockJoarkFasade.opprettJournalpost(any(), any()) }
    }

    @Test
    fun `utfør skal opprette og journalføre mangelbrev med kopi til bruker`() {
        val behandling = TestdataFactory.lagBehandling()
        every { mockBehandlingService.hentBehandling(any()) } returns behandling
        every { mockJoarkFasade.opprettJournalpost(any(), any()) } returns "12234"
        every { mockDokgenService.hentDokumentInfo(any()) } returns TestdataFactory.lagDokumentInfo()

        val brevbestilling = MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_BRUKER)
            .medManglerInfoFritekst("Mangler")
            .medBestillKopi(true)
            .build()
        val prosessinstans = lagProsessinstans(behandling, brevbestilling)


        opprettJournalforBrev.utfør(prosessinstans)


        verify { mockBehandlingService.hentBehandling(any()) }
        verify { mockDokgenService.produserBrev(any<Mottaker>(), any<MangelbrevBrevbestilling>()) }
        verify { mockJoarkFasade.opprettJournalpost(any(), any()) }
    }

    @Test
    fun `utfør skal feile for fritekstbrev uten tittel`() {
        val behandling = TestdataFactory.lagBehandling()
        every { mockBehandlingService.hentBehandling(any()) } returns behandling
        every { mockDokgenService.hentDokumentInfo(any()) } returns TestdataFactory.lagDokumentInfo()

        val brevbestilling = FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_BRUKER)
            .build()
        val prosessinstans = lagProsessinstans(behandling, brevbestilling)


        val exception = shouldThrow<FunksjonellException> {
            opprettJournalforBrev.utfør(prosessinstans)
        }


        exception.message shouldNotBe null
        exception.message!!.contains("Tittel til fritekstbrev mangler") shouldBe true
    }

    @Test
    fun `utfør skal opprette og journalføre fritekstbrev`() {
        val behandling = TestdataFactory.lagBehandling()
        every { mockBehandlingService.hentBehandling(any()) } returns behandling
        every { mockJoarkFasade.opprettJournalpost(any(), any()) } returns "12234"
        every { mockDokgenService.hentDokumentInfo(any()) } returns TestdataFactory.lagDokumentInfo()

        val brevbestilling = FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_BRUKER)
            .medFritekstTittel("Tittel")
            .medFritekst("Innhold")
            .build()
        val prosessinstans = lagProsessinstans(behandling, brevbestilling)


        opprettJournalforBrev.utfør(prosessinstans)


        verify { mockBehandlingService.hentBehandling(any()) }
        verify { mockDokgenService.produserBrev(any<Mottaker>(), any<FritekstbrevBrevbestilling>()) }
        verify { mockJoarkFasade.opprettJournalpost(any(), any()) }
    }

    @Test
    fun `utfør skal feile med IkkeFunnetException når journalpost ikke finnes for vedleggsdokument`() {
        val behandling = TestdataFactory.lagBehandling()
        every { mockBehandlingService.hentBehandling(any()) } returns behandling

        val saksvedleggBestillingList = listOf(SaksvedleggBestilling("1", "2"))
        val brevbestilling = FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_BRUKER)
            .medFritekstTittel("Tittel")
            .medSaksvedleggBestilling(saksvedleggBestillingList)
            .build()
        val prosessinstans = lagProsessinstans(behandling, brevbestilling)


        val exception = shouldThrow<IkkeFunnetException> {
            opprettJournalforBrev.utfør(prosessinstans)
        }


        exception.message shouldBe "Finner ikke journalpost 1 for saken saksnummer"
    }

    @Test
    fun `utfør skal hente vedleggsdokumenter fra Joark`() {
        val behandling = TestdataFactory.lagBehandling()
        every { mockBehandlingService.hentBehandling(any()) } returns behandling
        every { mockJoarkFasade.opprettJournalpost(capture(opprettJournalpostCaptor), any()) } returns "12234"
        every { mockDokgenService.hentDokumentInfo(any()) } returns TestdataFactory.lagDokumentInfo()
        every { mockDokumentHentingService.hentJournalposter("MEL-test") } returns listOf(
            lagJournalpost("1", "2", "tittel 1"),
            lagJournalpost("3", "4", "tittel 2")
        )
        every { mockJoarkFasade.hentDokument("1", "2") } returns byteArrayOf(1, 2)
        every { mockJoarkFasade.hentDokument("3", "4") } returns byteArrayOf(3, 4)

        val saksvedleggBestillingList = listOf(
            SaksvedleggBestilling("1", "2"),
            SaksvedleggBestilling("3", "4")
        )
        val brevbestilling = FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_BRUKER)
            .medFritekstTittel("Tittel")
            .medSaksvedleggBestilling(saksvedleggBestillingList)
            .medFritekst("Innhold")
            .build()
        val prosessinstans = lagProsessinstans(behandling, brevbestilling)


        opprettJournalforBrev.utfør(prosessinstans)


        verify { mockDokumentHentingService.hentJournalposter("MEL-test") }
        verify { mockJoarkFasade.hentDokument("1", "2") }
        verify { mockJoarkFasade.hentDokument("3", "4") }
        verify { mockJoarkFasade.opprettJournalpost(any(), any()) }

        opprettJournalpostCaptor.captured.run {
            eksternReferanseId shouldBe prosessinstansUuid.toString()
            hoveddokument.tittel shouldBe "Tittel"
            vedlegg.run {
                shouldHaveSize(2)
                get(0).run {
                    tittel shouldBe "tittel 1"
                    dokumentVarianter.firstOrNull()?.data?.contentEquals(byteArrayOf(1, 2)) shouldBe true
                }
                get(1).run {
                    tittel shouldBe "tittel 2"
                    dokumentVarianter.firstOrNull()?.data?.contentEquals(byteArrayOf(3, 4)) shouldBe true
                }
            }
        }
    }

    @Test
    fun `utfør skal opprette og journalføre brev med sakstema medlemskap lovvalg`() {
        val captor = slot<OpprettJournalpost>()
        val behandling = TestdataFactory.lagBehandling().apply {
            fagsak.tema = Sakstemaer.MEDLEMSKAP_LOVVALG
        }
        every { mockBehandlingService.hentBehandling(any()) } returns behandling
        every { mockJoarkFasade.opprettJournalpost(capture(captor), any()) } returns "12234"
        every { mockDokgenService.hentDokumentInfo(any()) } returns TestdataFactory.lagDokumentInfo()

        val brevbestilling = DokgenBrevbestilling.Builder()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .build()
        val prosessinstans = lagProsessinstans(behandling, brevbestilling)


        opprettJournalforBrev.utfør(prosessinstans)


        verify { mockJoarkFasade.opprettJournalpost(any(), any()) }
        captor.captured shouldNotBe null
        captor.captured.tema shouldBe Tema.MED.kode
    }

    @Test
    fun `utfør skal opprette og journalføre brev med sakstema trygdeavgift`() {
        val captor = slot<OpprettJournalpost>()
        val behandling = TestdataFactory.lagBehandling().apply {
            fagsak.tema = Sakstemaer.TRYGDEAVGIFT
        }
        every { mockBehandlingService.hentBehandling(any()) } returns behandling
        every { mockJoarkFasade.opprettJournalpost(capture(captor), any()) } returns "12234"
        every { mockDokgenService.hentDokumentInfo(any()) } returns TestdataFactory.lagDokumentInfo()

        val brevbestilling = DokgenBrevbestilling.Builder()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .build()
        val prosessinstans = lagProsessinstans(behandling, brevbestilling)


        opprettJournalforBrev.utfør(prosessinstans)


        verify { mockJoarkFasade.opprettJournalpost(any(), any()) }
        captor.captured shouldNotBe null
        captor.captured.tema shouldBe Tema.TRY.kode
    }

    @Test
    fun `utfør skal opprette og journalføre brev med sakstema unntak`() {
        val captor = slot<OpprettJournalpost>()
        val behandling = TestdataFactory.lagBehandling().apply {
            fagsak.tema = Sakstemaer.UNNTAK
        }
        every { mockBehandlingService.hentBehandling(any()) } returns behandling
        every { mockJoarkFasade.opprettJournalpost(capture(captor), any()) } returns "12234"
        every { mockDokgenService.hentDokumentInfo(any()) } returns TestdataFactory.lagDokumentInfo()

        val brevbestilling = DokgenBrevbestilling.Builder()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .build()
        val prosessinstans = lagProsessinstans(behandling, brevbestilling)


        opprettJournalforBrev.utfør(prosessinstans)


        verify { mockJoarkFasade.opprettJournalpost(any(), any()) }
        captor.captured shouldNotBe null
        captor.captured.tema shouldBe Tema.UFM.kode
    }

    @ParameterizedTest
    @MethodSource("hentProduserbaredokumenter")
    fun `utfør skal produsere fritekstbrev`(
        produserbaredokumenter: Produserbaredokumenter,
        tittel: String,
        dokumentTittel: String?,
        journalforingstittel: String
    ) {
        val behandling = TestdataFactory.lagBehandling()
        every { mockBehandlingService.hentBehandling(any()) } returns behandling
        every { mockJoarkFasade.opprettJournalpost(capture(opprettJournalpostCaptor), any()) } returns "12234"
        every { mockDokgenService.hentDokumentInfo(any()) } returns TestdataFactory.lagDokumentInfo()
        every { mockDokumentHentingService.hentJournalposter("MEL-test") } returns listOf(
            lagJournalpost("1", "2", "tittel 1"),
            lagJournalpost("3", "4", "tittel 2")
        )
        every { mockJoarkFasade.hentDokument("1", "2") } returns byteArrayOf(1, 2)
        every { mockJoarkFasade.hentDokument("3", "4") } returns byteArrayOf(3, 4)

        val saksvedleggBestillingList = listOf(
            SaksvedleggBestilling("1", "2"),
            SaksvedleggBestilling("3", "4")
        )
        val brevbestilling = FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(produserbaredokumenter)
            .medFritekstTittel(tittel)
            .medSaksvedleggBestilling(saksvedleggBestillingList)
            .medFritekst("Innhold")
            .medDokumentTittel(dokumentTittel)
            .build()
        val prosessinstans = lagProsessinstans(behandling, brevbestilling)


        opprettJournalforBrev.utfør(prosessinstans)


        verify { mockDokumentHentingService.hentJournalposter("MEL-test") }
        verify { mockJoarkFasade.hentDokument("1", "2") }
        verify { mockJoarkFasade.hentDokument("3", "4") }
        verify { mockJoarkFasade.opprettJournalpost(any(), any()) }

        opprettJournalpostCaptor.captured.run {
            hoveddokument.tittel shouldBe journalforingstittel
            val vedleggTitler = vedlegg.map { it.tittel }
            vedleggTitler shouldContainExactly listOf("tittel 1", "tittel 2")

            val vedleggData = vedlegg.map { it.dokumentVarianter.firstOrNull().shouldNotBeNull().data }
            vedleggData[0].contentEquals(byteArrayOf(1, 2)) shouldBe true
            vedleggData[1].contentEquals(byteArrayOf(3, 4)) shouldBe true
        }
    }

    @Test
    fun `utfør skal kombinere vedlegg med standardvedlegg og fritekstvedlegg`() {
        val behandling = TestdataFactory.lagBehandling()
        every { mockBehandlingService.hentBehandling(any()) } returns behandling
        every { mockJoarkFasade.opprettJournalpost(capture(opprettJournalpostCaptor), any()) } returns "12234"
        every { mockDokgenService.hentDokumentInfo(any()) } returns TestdataFactory.lagDokumentInfo()
        every { mockRettigheterOgPlikterStandardvedleggMapper.mapInnvilgelse(any(), any()) } returns InnvilgelseRettigheterPlikterStandardvedlegg()
        val standardvedleggPdf = byteArrayOf(5, 6)
        every { mockDokgenService.produserStandardvedlegg(any(), any()) } returns standardvedleggPdf

        val fritekstvedleggBestilling = listOf(
            FritekstvedleggBestilling("Tittel 1", "Tekst 1")
        )
        val brevbestilling = FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_BRUKER)
            .medFritekstTittel("Hovedtittel")
            .medFritekst("Innhold")
            .medFritekstvedleggBestilling(fritekstvedleggBestilling)
            .medStandardvedleggBestilling(StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE)
            .medBehandlingId(1L)
            .build()
        val prosessinstans = lagProsessinstans(behandling, brevbestilling)


        opprettJournalforBrev.utfør(prosessinstans)


        verify { mockJoarkFasade.opprettJournalpost(any(), any()) }

        opprettJournalpostCaptor.captured.vedlegg.run {
            shouldHaveSize(2)
            map { it.tittel } shouldContainExactly listOf("Tittel 1", "Viktig informasjon om rettigheter og plikter")
        }
    }

    @Test
    fun `utfør skal ikke ha standardvedlegg når standardvedleggType er null`() {
        val behandling = TestdataFactory.lagBehandling()
        every { mockBehandlingService.hentBehandling(any()) } returns behandling
        every { mockJoarkFasade.opprettJournalpost(capture(opprettJournalpostCaptor), any()) } returns "12234"
        every { mockDokgenService.hentDokumentInfo(any()) } returns TestdataFactory.lagDokumentInfo()

        val brevbestilling = FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_BRUKER)
            .medFritekstTittel("Hovedtittel")
            .medFritekst("Innhold")
            .medStandardvedleggBestilling(null)
            .build()
        val prosessinstans = lagProsessinstans(behandling, brevbestilling)


        opprettJournalforBrev.utfør(prosessinstans)


        verify { mockJoarkFasade.opprettJournalpost(any(), any()) }

        opprettJournalpostCaptor.captured.vedlegg shouldHaveSize 0
    }

    @Test
    fun `utfør skal returnere avslag vedlegg for avslag standardvedlegg`() {
        val behandling = TestdataFactory.lagBehandling()
        every { mockBehandlingService.hentBehandling(any()) } returns behandling
        every { mockJoarkFasade.opprettJournalpost(capture(opprettJournalpostCaptor), any()) } returns "12234"
        every { mockDokgenService.hentDokumentInfo(any()) } returns TestdataFactory.lagDokumentInfo()

        val brevbestilling = FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_BRUKER)
            .medFritekstTittel("Hovedtittel")
            .medFritekst("Innhold")
            .medStandardvedleggBestilling(StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_AVSLAG)
            .build()
        val prosessinstans = lagProsessinstans(behandling, brevbestilling)


        opprettJournalforBrev.utfør(prosessinstans)


        verify { mockJoarkFasade.opprettJournalpost(any(), any()) }

        opprettJournalpostCaptor.captured.vedlegg.run {
            shouldHaveSize(1)
            map { it.tittel } shouldContainExactly listOf("Viktig informasjon om rettigheter")
        }
    }

    @Test
    fun `utfør skal kombinere alle vedleggstyper`() {
        val behandling = TestdataFactory.lagBehandling()
        every { mockBehandlingService.hentBehandling(any()) } returns behandling
        every { mockJoarkFasade.opprettJournalpost(capture(opprettJournalpostCaptor), any()) } returns "12234"
        every { mockDokgenService.hentDokumentInfo(any()) } returns TestdataFactory.lagDokumentInfo()
        every { mockRettigheterOgPlikterStandardvedleggMapper.mapInnvilgelse(any(), any()) } returns InnvilgelseRettigheterPlikterStandardvedlegg()
        val standardvedleggPdf = byteArrayOf(5, 6)
        every { mockDokgenService.produserStandardvedlegg(any(), any()) } returns standardvedleggPdf
        every { mockDokumentHentingService.hentJournalposter("MEL-test") } returns listOf(
            lagJournalpost("1", "2", "Joark tittel")
        )
        every { mockJoarkFasade.hentDokument("1", "2") } returns byteArrayOf(1, 2)

        val fritekstvedleggBestilling = listOf(
            FritekstvedleggBestilling("Fritekst tittel", "Tekst")
        )
        val saksvedleggBestilling = listOf(
            SaksvedleggBestilling("1", "2")
        )
        val brevbestilling = FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_BRUKER)
            .medFritekstTittel("Hovedtittel")
            .medFritekst("Innhold")
            .medFritekstvedleggBestilling(fritekstvedleggBestilling)
            .medSaksvedleggBestilling(saksvedleggBestilling)
            .medStandardvedleggBestilling(StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE)
            .medBehandlingId(1L)
            .build()
        val prosessinstans = lagProsessinstans(behandling, brevbestilling)


        opprettJournalforBrev.utfør(prosessinstans)


        verify { mockJoarkFasade.opprettJournalpost(any(), any()) }

        opprettJournalpostCaptor.captured.vedlegg.run {
            shouldHaveSize(3)
            map { it.tittel } shouldContainExactly listOf(
                "Fritekst tittel",
                "Viktig informasjon om rettigheter og plikter",
                "Joark tittel"
            )
        }
    }

    private fun lagProsessinstans(behandling: Behandling, brevbestilling: DokgenBrevbestilling): Prosessinstans {
        val mottaker = lagMottaker("1234")

        return Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            id = prosessinstansUuid
            this.behandling = behandling
            medData(ProsessDataKey.BREVBESTILLING, brevbestilling)
            medData(ProsessDataKey.MOTTAKER, mottaker.rolle)
            medData(ProsessDataKey.AKTØR_ID, mottaker.aktørId)
        }
    }

    private fun lagProsessinstansMedMottaker(behandling: Behandling, mottaker: Mottaker, brevbestilling: DokgenBrevbestilling) =
        Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            id = prosessinstansUuid
            this.behandling = behandling
            medData(ProsessDataKey.BREVBESTILLING, brevbestilling)
            medData(ProsessDataKey.MOTTAKER, mottaker.rolle)
            mottaker.aktørId?.let { medData(ProsessDataKey.AKTØR_ID, it) }
            mottaker.orgnr?.let { medData(ProsessDataKey.ORGNR, it) }
            mottaker.institusjonID?.let { medData(ProsessDataKey.INSTITUSJON_ID, it) }
        }

    private fun lagProsessinstansMedOrgnr(behandling: Behandling, mottaker: Mottaker, brevbestilling: DokgenBrevbestilling) =
        Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            id = prosessinstansUuid
            this.behandling = behandling
            medData(ProsessDataKey.BREVBESTILLING, brevbestilling)
            medData(ProsessDataKey.MOTTAKER, mottaker.rolle)
            medData(ProsessDataKey.ORGNR, mottaker.orgnr)
        }

    private fun lagMottaker(aktørID: String) = Mottaker.medRolle(Mottakerroller.BRUKER).apply {
        aktørId = aktørID
        orgnr = null
        institusjonID = null
    }

    private fun lagJournalpost(journalpostId: String, dokumentId: String, tittel: String) =
        Journalpost(journalpostId).apply {
            journalposttype = Journalposttype.UT
            avsenderId = "NAVAT:07"
            korrespondansepartNavn = "Test12345"
            hoveddokument = ArkivDokument().apply {
                this.dokumentId = dokumentId
                this.tittel = tittel
            }
        }

    private fun hentProduserbaredokumenter() = listOf(
        Arguments.of(
            UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV,
            "Request to remain subject to Norwegian legislation",
            null,
            "Søknad om unntak"
        ),
        Arguments.of(
            GENERELT_FRITEKSTBREV_BRUKER,
            "Tittel",
            "Overstyrt tittel",
            "Overstyrt tittel"
        ),
        Arguments.of(
            GENERELT_FRITEKSTBREV_ARBEIDSGIVER,
            "Tittel",
            null,
            "Tittel"
        ),
        Arguments.of(
            GENERELT_FRITEKSTBREV_VIRKSOMHET,
            "Tittel",
            null,
            "Tittel"
        ),
        Arguments.of(
            GENERELT_FRITEKSTVEDLEGG,
            "Tittel",
            null,
            "Tittel"
        )
    )
}
