package no.nav.melosys.service.dokument.sed

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import io.mockk.impl.annotations.MockK
import no.nav.melosys.domain.*
import no.nav.melosys.domain.arkiv.ArkivDokument
import no.nav.melosys.domain.arkiv.DokumentReferanse
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.*
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis
import no.nav.melosys.domain.eessi.sed.SedDataDto
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IntegrasjonException
import no.nav.melosys.integrasjon.eessi.EessiConsumer
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.brev.SedPdfData
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlag
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagMedSoknad
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagUtenSoknad
import org.jeasy.random.EasyRandom
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class EessiServiceKtTest {

    @MockK
    private lateinit var sedDataBygger: SedDataBygger

    @MockK
    private lateinit var eessiConsumer: EessiConsumer

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var joarkFasade: JoarkFasade

    @MockK
    private lateinit var dokumentdataGrunnlagFactory: SedDataGrunnlagFactory

    private lateinit var eessiService: EessiService

    private val easyRandom = EasyRandom()
    private val unleash = FakeUnleash()
    private val mottakerBelgia1 = "BE:12222"
    private val mottakerBelgia2 = "BE:9999"
    private val mottakerBelgia3 = "BE:123131"
    private val mottakerTyskland1 = "DE:4444"
    private val mottakerTyskland2 = "DE:9999"

    private val institusjonBelgia1 = Institusjon(mottakerBelgia1, null, Landkoder.BE.kode)
    private val institusjonBelgia2 = Institusjon(mottakerBelgia2, null, Landkoder.BE.kode)
    private val institusjonBelgia3 = Institusjon(mottakerBelgia3, null, Landkoder.BE.kode)
    private val institusjonTyskland1 = Institusjon(mottakerTyskland1, null, Landkoder.DE.kode)
    private val institusjonTyskland2 = Institusjon(mottakerTyskland2, null, Landkoder.DE.kode)

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        eessiService = EessiService(
            behandlingService,
            behandlingsresultatService,
            eessiConsumer,
            joarkFasade,
            sedDataBygger,
            dokumentdataGrunnlagFactory,
            unleash
        )
    }

    private fun lagBehandling() = Behandling.forTest {
        id = BEHANDLING_ID
        fagsak {
            medGsakSaksnummer()
        }
    }

    private fun mockBehandling() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns lagBehandling()
    }

    private fun lagBehandlingsresultat() = Behandlingsresultat().apply {
        this.lovvalgsperioder = hashSetOf(Lovvalgsperiode().apply {
            this.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            this.lovvalgsland = Land_iso2.SK
        })
        this.anmodningsperioder = setOf(Anmodningsperiode().apply {
            this.anmodningsperiodeSvar = AnmodningsperiodeSvar().apply {
                this.anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
            }
        })

    }

    private fun mockBehandlingsresultat() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns lagBehandlingsresultat()
    }

    @Test
    fun `lagEessiVedlegg should create vedlegg from journalpost`() {
        val journalpost = lagJournalpost(listOf(lagArkivDokument("1"), lagArkivDokument("2")))
        val journalpostID = journalpost.journalpostId
        val dokumentReferanse = DokumentReferanse(journalpostID, "2")

        every { joarkFasade.hentJournalposterTilknyttetSak(any()) } returns listOf(journalpost)
        every { joarkFasade.hentDokument(any(), any()) } returns ByteArray(8)

        val fagsak = Fagsak.forTest {
            medGsakSaksnummer()
        }

        val vedlegg = eessiService.lagEessiVedlegg(fagsak, setOf(dokumentReferanse))

        vedlegg.first().run {
            innhold.size shouldBe 8
            tittel shouldBe "Tittel 2"
        }
    }

    @Test
    fun `opprettOgSendSed buc03 ingenMedlemsperiodeType`() {
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiConsumer.opprettBucOgSed(capture(sedDataDtoSlot), any(), any(), any(), any()) } returns OpprettSedDto()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_03, null, "fritekst")

        verify { sedDataBygger.lag(any<SedDataGrunnlag>(), eq(lagBehandlingsresultat()), eq(PeriodeType.INGEN)) }
        verify { eessiConsumer.opprettBucOgSed(any(), any(), eq(BucType.LA_BUC_03), eq(true), eq(true)) }
        sedDataDtoSlot.captured.ytterligereInformasjon shouldBe "fritekst"
    }

    @Test
    fun `opprettOgSendSed buc01 medlemsperiodeTypeAnmodningsperiode`() {
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_01, null, null)

        verify { sedDataBygger.lag(any<SedDataGrunnlag>(), eq(lagBehandlingsresultat()), eq(PeriodeType.ANMODNINGSPERIODE)) }
        verify { eessiConsumer.opprettBucOgSed(any<SedDataDto>(), any(), eq(BucType.LA_BUC_01), eq(true), eq(true)) }
    }

    @Test
    fun `opprettOgSendSed a001MedStorbritanniaKonv mapperKorrektYtterligereInformasjon`() {
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()

        val behandlingsresultat = lagBehandlingsresultat().apply {
            hentAnmodningsperiode().bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiConsumer.opprettBucOgSed(capture(sedDataDtoSlot), any(), any(), any(), any()) } returns OpprettSedDto()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_01, null, "fritekst")

        verify { sedDataBygger.lag(any<SedDataGrunnlag>(), eq(lagBehandlingsresultat()), eq(PeriodeType.ANMODNINGSPERIODE)) }
        verify { eessiConsumer.opprettBucOgSed(any(), any(), eq(BucType.LA_BUC_01), eq(true), eq(true)) }
        sedDataDtoSlot.captured.ytterligereInformasjon shouldBe "Issued under the EEA EFTA Convention. fritekst"
    }

    @Test
    fun `opprettOgSendSed a009MedStorbritanniaKonv mapperKorrektYtterligereInformasjon`() {
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()

        val behandlingsresultat = lagBehandlingsresultat().apply {
            hentLovvalgsperiode().bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiConsumer.opprettBucOgSed(capture(sedDataDtoSlot), any(), any(), any(), any()) } returns OpprettSedDto()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_04, null, "fritekst")

        verify { sedDataBygger.lag(any<SedDataGrunnlag>(), eq(lagBehandlingsresultat()), eq(PeriodeType.LOVVALGSPERIODE)) }
        verify { eessiConsumer.opprettBucOgSed(any(), any(), eq(BucType.LA_BUC_04), eq(true), eq(true)) }
        sedDataDtoSlot.captured.ytterligereInformasjon shouldBe "Issued under the EEA EFTA Convention. fritekst"
    }

    @Test
    fun `opprettOgSendSed buc02IngenUtpekingsperiode medlemsperiodeTypeLovvalgsperiode`() {
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_02, null, null)

        verify { sedDataBygger.lag(any<SedDataGrunnlag>(), eq(lagBehandlingsresultat()), eq(PeriodeType.LOVVALGSPERIODE)) }
        verify { eessiConsumer.opprettBucOgSed(any<SedDataDto>(), any(), eq(BucType.LA_BUC_02), eq(true), eq(true)) }
    }

    @Test
    fun `opprettOgSendSed buc02MedUtpekingsperiode medlemsperiodeTypeUtpekingsperiode`() {
        val behandlingsresultat = lagBehandlingsresultat().apply {
            utpekingsperioder.add(Utpekingsperiode())
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_02, null, null)

        verify { sedDataBygger.lag(any<SedDataGrunnlag>(), eq(behandlingsresultat), eq(PeriodeType.UTPEKINGSPERIODE)) }
        verify { eessiConsumer.opprettBucOgSed(any<SedDataDto>(), any(), eq(BucType.LA_BUC_02), eq(true), eq(true)) }
    }

    @Test
    fun `opprettOgSendSed buc04 medlemsperiodeTypeLovvalgsperiode`() {
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiConsumer.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_04, null, null)

        verify { sedDataBygger.lag(any<SedDataGrunnlag>(), eq(lagBehandlingsresultat()), eq(PeriodeType.LOVVALGSPERIODE)) }
        verify { eessiConsumer.opprettBucOgSed(any<SedDataDto>(), any(), eq(BucType.LA_BUC_04), eq(true), eq(true)) }
    }

    @Test
    fun `opprettBucOgSed verifiserKorrektSedType`() {
        val opprettSedDto = OpprettSedDto().apply {
            rinaUrl = "localhost:3000"
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns lagBehandling()
        every { eessiConsumer.opprettBucOgSed(any<SedDataDto>(), any(), any<BucType>(), any(), any()) } returns opprettSedDto
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { joarkFasade.validerDokumenterTilhørerSakOgHarTilgang(any(), any()) } returns Unit
        mockBehandlingsresultat()

        eessiService.opprettBucOgSed(BEHANDLING_ID, BucType.LA_BUC_01, listOf(mottakerBelgia1), emptyList())

        verify { eessiConsumer.opprettBucOgSed(any<SedDataDto>(), any(), eq(BucType.LA_BUC_01), eq(false), eq(false)) }
    }

    @Test
    fun `hentMottakerinstitusjoner forventListeMedRettType`() {
        every { eessiConsumer.hentMottakerinstitusjoner(any(), any<List<String>>()) } returns listOf(
            Institusjon("1", "Test1", "NO"),
            Institusjon("2", "Test2", "NO")
        )

        val mottakerinstitusjoner = eessiService.hentEessiMottakerinstitusjoner("LA_BUC_01", listOf("FR"))

        verify { eessiConsumer.hentMottakerinstitusjoner(any(), any<List<String>>()) }
        mottakerinstitusjoner shouldHaveSize 2
        mottakerinstitusjoner.forEach { it.shouldBeInstanceOf<Institusjon>() }
    }

    @Test
    fun `hentTilknyttedeBucer forventListeMedRettType`() {
        every { eessiConsumer.hentTilknyttedeBucer(any(), any<List<String>>()) } returns listOf(
            easyRandom.nextObject(BucInformasjon::class.java),
            easyRandom.nextObject(BucInformasjon::class.java),
            easyRandom.nextObject(BucInformasjon::class.java)
        )

        val tilknyttedeBucer = eessiService.hentTilknyttedeBucer(123L, listOf("utkast", "sendt"))

        verify { eessiConsumer.hentTilknyttedeBucer(any(), any<List<String>>()) }
        tilknyttedeBucer shouldHaveSize 3
        tilknyttedeBucer.forEach { it.shouldBeInstanceOf<BucInformasjon>() }
    }

    @Test
    fun `hentTilknyttedeBucer medFeilIConsumer forventException`() {
        every { eessiConsumer.hentTilknyttedeBucer(any(), any<List<String>>()) } throws IntegrasjonException("Error!")

        shouldThrow<IntegrasjonException> {
            eessiService.hentTilknyttedeBucer(123L, listOf("utkast"))
        }
    }

    @Test
    fun `støtterAutomatiskBehandling verifiserA001A003A009A010støtterAutomatiskBehandling`() {
        val sedTyperAutomatiskBehandling = listOf(
            SedType.A001.name,
            SedType.A009.name,
            SedType.A010.name
        )

        val melosysEessiMelding = MelosysEessiMelding().apply {
            lovvalgsland = Landkoder.DE.name
        }
        every { eessiConsumer.hentMelosysEessiMeldingFraJournalpostID("123") } returns melosysEessiMelding

        sedTyperAutomatiskBehandling.forEach { sedType ->
            melosysEessiMelding.sedType = sedType
            eessiService.støtterAutomatiskBehandling("123") shouldBe true
        }
    }

    @Test
    fun `støtterAutomatiskBehandling verifiserStøtterIkkeAutomatiskBehandling`() {
        val sedTyperIkkeAutomatiskBehandling = listOf(
            SedType.H001.name,
            SedType.A002.name,
            SedType.A004.name,
            SedType.A005.name,
            SedType.A006.name,
            SedType.A007.name,
            SedType.A008.name,
            SedType.A011.name,
            SedType.A012.name
        )

        val melosysEessiMelding = MelosysEessiMelding().apply {
            lovvalgsland = Landkoder.DE.name
        }
        every { eessiConsumer.hentMelosysEessiMeldingFraJournalpostID("123") } returns melosysEessiMelding

        sedTyperIkkeAutomatiskBehandling.forEach { sedType ->
            melosysEessiMelding.sedType = sedType
            eessiService.støtterAutomatiskBehandling("123") shouldBe false
        }
    }

    @Test
    fun `støtterAutomatiskBehandling nullVerdi forventFalse`() {
        val melosysEessiMelding = MelosysEessiMelding().apply {
            sedType = null
        }
        every { eessiConsumer.hentMelosysEessiMeldingFraJournalpostID("123") } returns melosysEessiMelding

        eessiService.støtterAutomatiskBehandling("123") shouldBe false
    }

    @Test
    fun `støtterAutomatiskBehandling a003ikkeUtpekt verifiserStøtterAutomatiskBehandling`() {
        val melosysEessiMelding = MelosysEessiMelding().apply {
            lovvalgsland = Landkoder.SE.name
            sedType = "A003"
        }
        every { eessiConsumer.hentMelosysEessiMeldingFraJournalpostID("123") } returns melosysEessiMelding

        eessiService.støtterAutomatiskBehandling("123") shouldBe true
    }

    @Test
    fun `støtterAutomatiskBehandling a003erUtpekt verifiserStøtterIkkeAutomatiskBehandling`() {
        val melosysEessiMelding = MelosysEessiMelding().apply {
            lovvalgsland = Landkoder.NO.name
            sedType = "A003"
        }
        every { eessiConsumer.hentMelosysEessiMeldingFraJournalpostID("123") } returns melosysEessiMelding

        eessiService.støtterAutomatiskBehandling("123") shouldBe true
    }

    @Test
    fun `hentSakForRinaSaksnummer forventOptionalIkkePresent`() {
        every { eessiConsumer.hentSakForRinasaksnummer(any()) } returns emptyList()

        val res = eessiService.finnSakForRinasaksnummer("123")

        res.isEmpty shouldBe true
    }

    @Test
    fun `hentSakForRinaSaksnummer forventOptionalPresent`() {
        every { eessiConsumer.hentSakForRinasaksnummer(any()) } returns listOf(SaksrelasjonDto(123L, "123", "123"))

        val res = eessiService.finnSakForRinasaksnummer("123")

        res.isPresent shouldBe true
    }

    @Test
    fun `lagreSaksrelasjon validerInput`() {
        every { eessiConsumer.lagreSaksrelasjon(any()) } returns Unit

        eessiService.lagreSaksrelasjon(123L, "123", "312")

        verify { eessiConsumer.lagreSaksrelasjon(any()) }
    }

    @Test
    fun `sendAnmodningUnntakSvar forventKall`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
        }

        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = SedDokument()
        }
        behandling.saksopplysninger = mutableSetOf(saksopplysning)

        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns behandling
        every { dokumentdataGrunnlagFactory.av(any(), any()) } returns mockk<SedDataGrunnlagUtenSoknad>()
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiConsumer.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), any<SedType>()) } returns Unit
        mockBehandlingsresultat()

        eessiService.sendAnmodningUnntakSvar(BEHANDLING_ID, null)

        verify { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) }
        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.ANMODNINGSPERIODE)) }
        verify { dokumentdataGrunnlagFactory.av(any(), any()) }
        verify { eessiConsumer.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), eq(SedType.A002)) }
    }

    @Test
    fun `sendGodkjenningArbeidFlereLand should work correctly`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            fagsak {
                medGsakSaksnummer()
            }
        }

        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = SedDokument()
        }
        behandling.saksopplysninger = mutableSetOf(saksopplysning)

        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns behandling
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { dokumentdataGrunnlagFactory.av(any(), any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiConsumer.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), any<SedType>()) } returns Unit
        mockBehandlingsresultat()

        eessiService.sendGodkjenningArbeidFlereLand(BEHANDLING_ID, null)

        verify { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) }
        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.LOVVALGSPERIODE)) }
        verify { dokumentdataGrunnlagFactory.av(any(), any()) }
        verify { eessiConsumer.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), eq(SedType.A012)) }
    }

    @Test
    fun `sendGodkjenningArbeidFlereLand feiler ikke når x008 utsending feiler`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                medGsakSaksnummer()
            }
        }

        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = SedDokument()
        }
        behandling.saksopplysninger = mutableSetOf(saksopplysning)

        val sedInformasjon = SedInformasjon("1", "2", LocalDate.now(), LocalDate.now(), "A012", "whatever", null)
        val bucInformasjon = BucInformasjon("1", true, "LA_BUC_02", LocalDate.now(), null, listOf(sedInformasjon))
        val bucInformasjonListe = listOf(bucInformasjon)

        every { eessiConsumer.hentTilknyttedeBucer(eq(FagsakTestFactory.GSAK_SAKSNUMMER), any()) } returns bucInformasjonListe
        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns behandling
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { dokumentdataGrunnlagFactory.av(any(), any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiConsumer.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), eq(SedType.A012)) } returns Unit
        every { eessiConsumer.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), eq(SedType.X008)) } throws RuntimeException()
        mockBehandlingsresultat()

        eessiService.sendGodkjenningArbeidFlereLand(BEHANDLING_ID, null)

        verify(exactly = 2) { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) }
        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.LOVVALGSPERIODE)) }
        verify(exactly = 2) { dokumentdataGrunnlagFactory.av(any(), any()) }
        verify { eessiConsumer.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), eq(SedType.A012)) }
    }

    @Test
    fun `sendAvslagUtpekingSvar feiler ikke når x008 utsending feiler`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                medGsakSaksnummer()
            }
            saksopplysninger = mutableSetOf(Saksopplysning().apply {
                this.type = SaksopplysningType.SEDOPPL
                this.dokument = SedDokument()
            })
        }

        val sedInformasjon = SedInformasjon("1", "2", LocalDate.now(), LocalDate.now(), "A012", "whatever", null)
        val bucInformasjon = BucInformasjon("1", true, "LA_BUC_02", LocalDate.now(), null, listOf(sedInformasjon))
        val bucInformasjonListe = listOf(bucInformasjon)

        every { eessiConsumer.hentTilknyttedeBucer(eq(FagsakTestFactory.GSAK_SAKSNUMMER), any()) } returns bucInformasjonListe
        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns behandling
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        every { dokumentdataGrunnlagFactory.av(any(), eq(SedType.X008)) } returns mockk<SedDataGrunnlagUtenSoknad>()
        every { sedDataBygger.lagUtkast(any(), any(), any()) } returns SedDataDto()
        every { eessiConsumer.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), eq(SedType.A004)) } returns Unit
        every { eessiConsumer.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), eq(SedType.X008)) } throws RuntimeException()
        mockBehandlingsresultat()

        val utpekingAvvis = UtpekingAvvis().apply {
            etterspørInformasjon = false
        }

        eessiService.sendAvslagUtpekingSvar(BEHANDLING_ID, utpekingAvvis)

        verify(exactly = 2) { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) }
        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.LOVVALGSPERIODE)) }
        verify { dokumentdataGrunnlagFactory.av(any(), any()) }
        verify { eessiConsumer.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), eq(SedType.A004)) }
    }

    @Test
    fun `genererSedPdf sedA001 medlemsperiodeTypeAnmodningsperiode`() {
        val pdf = "pdf".toByteArray()
        every { eessiConsumer.genererSedPdf(any(), any()) } returns pdf
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        val result = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A001)

        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.ANMODNINGSPERIODE)) }
        verify { eessiConsumer.genererSedPdf(any(), any()) }
        result shouldBe pdf
    }

    @Test
    fun `genererSedPdf sedA001 storbritanniaKonvFårTilpassetYtterligereInformasjon`() {
        val pdf = "pdf".toByteArray()
        every { eessiConsumer.genererSedPdf(any(), any()) } returns pdf
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()

        val sedPdfData = SedPdfData().apply {
            fritekst = "fritekst"
        }
        val behandlingsresultat = lagBehandlingsresultat().apply {
            hentAnmodningsperiode().bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiConsumer.genererSedPdf(capture(sedDataDtoSlot), any()) } returns pdf

        val result = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A001, sedPdfData)

        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.ANMODNINGSPERIODE)) }
        verify { eessiConsumer.genererSedPdf(any(), any()) }
        sedDataDtoSlot.captured.ytterligereInformasjon shouldBe "Issued under the EEA EFTA Convention. fritekst"
        result shouldBe pdf
    }

    @Test
    fun `genererSedPdf sedA003MedUtpekingsperiode medlemsperiodeTypeUtpekingsperiode`() {
        val pdf = "pdf".toByteArray()
        every { eessiConsumer.genererSedPdf(any(), any()) } returns pdf
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()

        val behandlingsresultat = lagBehandlingsresultat().apply {
            utpekingsperioder.add(Utpekingsperiode())
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val result = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A003)

        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.UTPEKINGSPERIODE)) }
        verify { eessiConsumer.genererSedPdf(any(), any()) }
        result shouldBe pdf
    }

    @Test
    fun `genererSedPdf sedA009 medlemsperiodeTypeLovvalgsperiode`() {
        val pdf = "pdf".toByteArray()
        every { eessiConsumer.genererSedPdf(any(), any()) } returns pdf
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()

        val behandlingsresultat = lagBehandlingsresultat().apply {
            utpekingsperioder.add(Utpekingsperiode())
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val result = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A009)

        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.LOVVALGSPERIODE)) }
        verify { eessiConsumer.genererSedPdf(any(), any()) }
        result shouldBe pdf
    }

    @Test
    fun `genererSedForhåndsvisning medSedPdfData verifiserSedDataDtoPreutfylt`() {
        val pdf = "pdf".toByteArray()
        every { eessiConsumer.genererSedPdf(any(), any()) } returns pdf
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        mockBehandlingsresultat()
        mockBehandling()

        val sedPdfData = SedPdfData().apply {
            nyttLovvalgsland = "SE"
        }

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiConsumer.genererSedPdf(capture(sedDataDtoSlot), any()) } returns pdf

        val result = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A001, sedPdfData)

        verify { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) }
        verify { dokumentdataGrunnlagFactory.av(any()) }
        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.ANMODNINGSPERIODE)) }
        verify { eessiConsumer.genererSedPdf(any(), any()) }
        result shouldBe pdf

        val sedDataDto = sedDataDtoSlot.captured
        sedDataDto.utpekingAvvis.shouldNotBeNull().nyttLovvalgsland shouldBe sedPdfData.nyttLovvalgsland
    }

    @Test
    fun `hentSedTypeForAnmodningUnntakSvar forventA002`() {
        val behandlingsresultat = lagBehandlingsresultat().apply {
            hentAnmodningsperiode().anmodningsperiodeSvar.anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val sedType = eessiService.hentSedTypeForAnmodningUnntakSvar(BEHANDLING_ID)

        sedType shouldBe SedType.A002
    }

    @Test
    fun `hentSedTypeForAnmodningUnntakSvar forventA011`() {
        val behandlingsresultat = lagBehandlingsresultat().apply {
            hentAnmodningsperiode().anmodningsperiodeSvar.anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val sedType = eessiService.hentSedTypeForAnmodningUnntakSvar(BEHANDLING_ID)

        sedType shouldBe SedType.A011
    }

    @Test
    fun `validerOgAvklarMottakerInstitusjonerForBuc toMottakereToMottakerLandMottakereKorrektSatt returnererMottakerInstitusjoner`() {
        val bucType = BucType.LA_BUC_02
        val mottakerLand = listOf(Land_iso2.BE, Land_iso2.DE)
        val valgteMottakerInstitusjoner = setOf(mottakerBelgia1, mottakerTyskland1)

        every {
            eessiConsumer.hentMottakerinstitusjoner(bucType.name, setOf(Land_iso2.BE.kode, Land_iso2.DE.kode))
        } returns listOf(institusjonBelgia1, institusjonBelgia2, institusjonTyskland1, institusjonTyskland2)

        val avklarteMottakerInstitusjoner =
            eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType)

        verify { eessiConsumer.hentMottakerinstitusjoner(eq(bucType.name), any<Set<String>>()) }
        avklarteMottakerInstitusjoner shouldBe valgteMottakerInstitusjoner
    }

    @Test
    fun `validerOgAvklarMottakerInstitusjonerForBuc toMottakereSisteErIkkeEessiReady returnererTomListe`() {
        val bucType = BucType.LA_BUC_02
        val mottakerLand = listOf(Land_iso2.BE, Land_iso2.DE)
        val valgteMottakerInstitusjoner = setOf(mottakerBelgia1, mottakerTyskland1)

        every {
            eessiConsumer.hentMottakerinstitusjoner(bucType.name, setOf(Land_iso2.BE.kode, Land_iso2.DE.kode))
        } returns listOf(institusjonBelgia1, institusjonBelgia2)

        val avklarteMottakerInstitusjoner =
            eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType)

        verify { eessiConsumer.hentMottakerinstitusjoner(eq(bucType.name), any<Set<String>>()) }
        avklarteMottakerInstitusjoner.shouldBeEmpty()
    }

    @Test
    fun `validerOgAvklarMottakerInstitusjonerForBuc toLandInstitusjonManglerForSiste kasterException`() {
        val bucType = BucType.LA_BUC_02
        val mottakerLand = listOf(Land_iso2.BE, Land_iso2.DE)
        val valgteMottakerInstitusjoner = setOf(mottakerBelgia1)

        every {
            eessiConsumer.hentMottakerinstitusjoner(bucType.name, setOf(Landkoder.BE.kode, Landkoder.DE.kode))
        } returns listOf(institusjonBelgia1, institusjonBelgia2, institusjonTyskland1, institusjonTyskland2)

        shouldThrow<FunksjonellException> {
            eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType)
        }.message shouldContain "Finner ingen gyldig mottakerinstitusjon for arbeidsland Tyskland"
    }

    @Test
    fun `validerOgAvklarMottakerInstitusjonerForBuc toLandInstitusjonManglerForSiste2 kasterException`() {
        val bucType = BucType.LA_BUC_02
        val mottakerLand = listOf(Land_iso2.BE, Land_iso2.DE)
        val valgteMottakerInstitusjoner = setOf(mottakerBelgia1, mottakerBelgia3, mottakerTyskland1)

        every {
            eessiConsumer.hentMottakerinstitusjoner(bucType.name, setOf(Landkoder.BE.kode, Landkoder.DE.kode))
        } returns listOf(institusjonBelgia1, institusjonBelgia3, institusjonBelgia2, institusjonTyskland1, institusjonTyskland2)

        shouldThrow<FunksjonellException> {
            eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType)
        }.message shouldContain "Kan kun velge en mottakerinstitusjon per land. Validerte mottakere:"
    }

    @Test
    fun `validerOgAvklarMottakerInstitusjonerForBuc toLandErPåkobletIngenInstitusjonValgt kasterException`() {
        val bucType = BucType.LA_BUC_02
        val mottakerLand = listOf(Land_iso2.BE, Land_iso2.DE)
        val valgteMottakerInstitusjoner = emptySet<String>()

        every {
            eessiConsumer.hentMottakerinstitusjoner(bucType.name, setOf(Landkoder.BE.kode, Landkoder.DE.kode))
        } returns listOf(institusjonBelgia2, institusjonTyskland2)

        shouldThrow<FunksjonellException> {
            eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType)
        }.message shouldContain (
            "Finner ingen gyldig mottakerinstitusjon for arbeidsland ${Landkoder.BE.beskrivelse}${System.lineSeparator()}" +
                "Finner ingen gyldig mottakerinstitusjon for arbeidsland ${Landkoder.DE.beskrivelse}"
            )
    }

    @Test
    fun `validerOgAvklarMottakerInstitusjonerForBuc toLandEnErIkkePåkobletIngenInstitusjonValgt returnererTomListe`() {
        val bucType = BucType.LA_BUC_02
        val mottakerLand = listOf(Land_iso2.BE, Land_iso2.DE)
        val valgteMottakerInstitusjoner = emptySet<String>()
        val institusjonBelgia = Institusjon("BE:44444", null, Landkoder.BE.kode)

        every {
            eessiConsumer.hentMottakerinstitusjoner(bucType.name, setOf(Landkoder.BE.kode, Landkoder.DE.kode))
        } returns listOf(institusjonBelgia)

        val avklarteMottakere = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType)

        avklarteMottakere.shouldBeEmpty()
    }

    @Test
    fun `landErEessiReady toLandEtErEessiReady forventFalse`() {
        val bucType = BucType.LA_BUC_01
        val land = listOf(Land_iso2.SE, Land_iso2.DK)

        every { eessiConsumer.hentMottakerinstitusjoner(bucType.name, setOf(Land_iso2.SE.kode)) } returns listOf(
            Institusjon("2", "", "")
        )
        every { eessiConsumer.hentMottakerinstitusjoner(bucType.name, setOf(Land_iso2.DK.kode)) } returns emptyList()

        eessiService.landErEessiReady(bucType.name, land) shouldBe false
    }

    @Test
    fun `landErEessiReady toLandAlleErEessiReady forventTrue`() {
        val bucType = BucType.LA_BUC_01
        val land = listOf(Land_iso2.SE, Land_iso2.DK)

        every { eessiConsumer.hentMottakerinstitusjoner(eq(bucType.name), any()) } returns listOf(
            Institusjon("2", "", "")
        )

        eessiService.landErEessiReady(bucType.name, land) shouldBe true
    }

    @Test
    fun `kanOppretteSedPåBuc fårCreateTilbake true`() {
        every { eessiConsumer.hentMuligeAksjoner("5566") } returns listOf("5fcffb7e6a9640acac5a09abc5a08ef6 A004 Create")

        eessiService.kanOppretteSedTyperPåBuc("5566", SedType.A004) shouldBe true
    }

    @Test
    fun `kanOppretteSedPåBuc fårCreateTilbakePåFeilSed false`() {
        every { eessiConsumer.hentMuligeAksjoner("5566") } returns listOf("5fcffb7e6a9640acac5a09abc5a08ef6 A004 Create")

        eessiService.kanOppretteSedTyperPåBuc("5566", SedType.A011) shouldBe false
    }

    @Test
    fun `kanOppretteSedPåBuc tomListe false`() {
        every { eessiConsumer.hentMuligeAksjoner("5566") } returns emptyList()

        eessiService.kanOppretteSedTyperPåBuc("5566", SedType.A011) shouldBe false
    }

    private fun lagJournalpost(dokumenter: List<ArkivDokument>) = Journalpost("jpID").apply {
        hoveddokument = dokumenter[0]
        vedleggListe.clear()
        vedleggListe.addAll(dokumenter.subList(1, dokumenter.size))
    }

    private fun lagArkivDokument(dokumentID: String) = ArkivDokument().apply {
        dokumentId = dokumentID
        tittel = "Tittel $dokumentID"
    }

    companion object {
        private const val BEHANDLING_ID = 1L

    }
}
