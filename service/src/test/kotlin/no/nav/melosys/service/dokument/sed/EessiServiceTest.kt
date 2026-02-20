package no.nav.melosys.service.dokument.sed

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
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
import no.nav.melosys.domain.eessi.*
import no.nav.melosys.domain.eessi.A008Formaal
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis
import no.nav.melosys.domain.eessi.sed.OpprettBucOgSedDtoV2
import no.nav.melosys.domain.eessi.sed.SedDataDto
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IntegrasjonException
import no.nav.melosys.integrasjon.eessi.EessiClient
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
import no.nav.melosys.domain.eessi.sed.VedleggReferanse

class EessiServiceTest {

    @MockK
    private lateinit var sedDataBygger: SedDataBygger

    @MockK
    private lateinit var eessiClient: EessiClient

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
        unleash.disable(ToggleName.MELOSYS_BRUK_OPPRETT_BUC_OG_SED_V2)
        eessiService = EessiService(
            behandlingService,
            behandlingsresultatService,
            eessiClient,
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

    private fun lagBehandlingsresultat() = Behandlingsresultat.forTest {
        lovvalgsperiode {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            lovvalgsland = Land_iso2.SK
        }
        anmodningsperiode {
            anmodningsperiodeSvar {
                anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
            }
        }
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
            hentInnhold.size shouldBe 8
            tittel shouldBe "Tittel 2"
        }
    }

    @Test
    fun `opprettOgSendSed buc03 ingenMedlemsperiodeType`() {
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiClient.opprettBucOgSed(capture(sedDataDtoSlot), any(), any(), any(), any()) } returns OpprettSedDto()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_03, emptyList(), "fritekst", null, false)

        verify { sedDataBygger.lag(any<SedDataGrunnlag>(), eq(lagBehandlingsresultat()), eq(PeriodeType.INGEN)) }
        verify { eessiClient.opprettBucOgSed(any(), any(), eq(BucType.LA_BUC_03), eq(true), eq(true)) }
        sedDataDtoSlot.captured.ytterligereInformasjon shouldBe "fritekst"
    }

    @Test
    fun `opprettOgSendSed buc01 medlemsperiodeTypeAnmodningsperiode`() {
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_01, emptyList(), null, null, false)

        verify { sedDataBygger.lag(any<SedDataGrunnlag>(), eq(lagBehandlingsresultat()), eq(PeriodeType.ANMODNINGSPERIODE)) }
        verify { eessiClient.opprettBucOgSed(any<SedDataDto>(), any(), eq(BucType.LA_BUC_01), eq(true), eq(true)) }
    }

    @Test
    fun `opprettOgSendSed a001MedStorbritanniaKonv mapperKorrektYtterligereInformasjon`() {
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()

        val behandlingsresultat = lagBehandlingsresultatMedStorbritanniaAnmodning()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiClient.opprettBucOgSed(capture(sedDataDtoSlot), any(), any(), any(), any()) } returns OpprettSedDto()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_01, emptyList(), "fritekst", null, false)

        verify { sedDataBygger.lag(any<SedDataGrunnlag>(), eq(lagBehandlingsresultat()), eq(PeriodeType.ANMODNINGSPERIODE)) }
        verify { eessiClient.opprettBucOgSed(any(), any(), eq(BucType.LA_BUC_01), eq(true), eq(true)) }
        sedDataDtoSlot.captured.ytterligereInformasjon shouldBe "Issued under the EEA EFTA Convention. fritekst"
    }

    private fun lagBehandlingsresultatMedStorbritanniaAnmodning() = Behandlingsresultat.forTest {
        lovvalgsperiode {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            lovvalgsland = Land_iso2.SK
        }
        anmodningsperiode {
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
            anmodningsperiodeSvar {
                anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
            }
        }
    }

    @Test
    fun `opprettOgSendSed a009MedStorbritanniaKonv mapperKorrektYtterligereInformasjon`() {
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()

        val behandlingsresultat = lagBehandlingsresultatMedStorbritanniaLovvalg()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiClient.opprettBucOgSed(capture(sedDataDtoSlot), any(), any(), any(), any()) } returns OpprettSedDto()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_04, emptyList(), "fritekst", null, false)

        verify { sedDataBygger.lag(any<SedDataGrunnlag>(), eq(lagBehandlingsresultat()), eq(PeriodeType.LOVVALGSPERIODE)) }
        verify { eessiClient.opprettBucOgSed(any(), any(), eq(BucType.LA_BUC_04), eq(true), eq(true)) }
        sedDataDtoSlot.captured.ytterligereInformasjon shouldBe "Issued under the EEA EFTA Convention. fritekst"
    }

    private fun lagBehandlingsresultatMedStorbritanniaLovvalg() = Behandlingsresultat.forTest {
        lovvalgsperiode {
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
            lovvalgsland = Land_iso2.SK
        }
        anmodningsperiode {
            anmodningsperiodeSvar {
                anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
            }
        }
    }

    @Test
    fun `opprettOgSendSed buc02IngenUtpekingsperiode medlemsperiodeTypeLovvalgsperiode`() {
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_02, emptyList(), null, null, false)

        verify { sedDataBygger.lag(any<SedDataGrunnlag>(), eq(lagBehandlingsresultat()), eq(PeriodeType.LOVVALGSPERIODE)) }
        verify { eessiClient.opprettBucOgSed(any<SedDataDto>(), any(), eq(BucType.LA_BUC_02), eq(true), eq(true)) }
    }

    @Test
    fun `opprettOgSendSed buc02MedUtpekingsperiode medlemsperiodeTypeUtpekingsperiode`() {
        val behandlingsresultat = lagBehandlingsresultatMedUtpekingsperiode()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_02, emptyList(), null, null, false)

        verify { sedDataBygger.lag(any<SedDataGrunnlag>(), eq(behandlingsresultat), eq(PeriodeType.UTPEKINGSPERIODE)) }
        verify { eessiClient.opprettBucOgSed(any<SedDataDto>(), any(), eq(BucType.LA_BUC_02), eq(true), eq(true)) }
    }

    private fun lagBehandlingsresultatMedUtpekingsperiode() = Behandlingsresultat.forTest {
        lovvalgsperiode {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            lovvalgsland = Land_iso2.SK
        }
        anmodningsperiode {
            anmodningsperiodeSvar {
                anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
            }
        }
        utpekingsperiode { }
    }

    @Test
    fun `opprettOgSendSed buc04 medlemsperiodeTypeLovvalgsperiode`() {
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_04, emptyList(), null, null, false)

        verify { sedDataBygger.lag(any<SedDataGrunnlag>(), eq(lagBehandlingsresultat()), eq(PeriodeType.LOVVALGSPERIODE)) }
        verify { eessiClient.opprettBucOgSed(any<SedDataDto>(), any(), eq(BucType.LA_BUC_04), eq(true), eq(true)) }
    }

    @Test
    fun `opprettBucOgSed verifiserKorrektSedType`() {
        val opprettSedDto = OpprettSedDto().apply {
            rinaUrl = "localhost:3000"
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns lagBehandling()
        every { eessiClient.opprettBucOgSed(any<SedDataDto>(), any(), any<BucType>(), any(), any()) } returns opprettSedDto
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { joarkFasade.validerDokumenterTilhørerSakOgHarTilgang(any(), any()) } returns Unit
        mockBehandlingsresultat()

        eessiService.opprettBucOgSed(BEHANDLING_ID, BucType.LA_BUC_01, listOf(mottakerBelgia1), emptyList())

        verify { eessiClient.opprettBucOgSed(any<SedDataDto>(), any(), eq(BucType.LA_BUC_01), eq(false), eq(false)) }
    }

    @Test
    fun `opprettBucOgSed medFeatureToggle MELOSYS_BRUK_OPPRETT_BUC_OG_SED_V2 på kallerOpprettBucOgSedV2`() {
        unleash.enable(ToggleName.MELOSYS_BRUK_OPPRETT_BUC_OG_SED_V2)

        val opprettSedDto = OpprettSedDto().apply {
            rinaUrl = "localhost:3000"
        }
        val behandling = lagBehandling()
        val sedDataDto = SedDataDto()

        val journalpost = lagJournalpost(listOf(
            lagArkivDokument("hoved"),
            lagArkivDokument("dok1"),
            lagArkivDokument("dok2"))
        )

        val dokumentReferanser = journalpost.vedleggListe.map {
            DokumentReferanse(journalpost.journalpostId, it.dokumentId ?: error("dokumentId burde ikke være null her"))
        }

        val opprettBucOgSedDtoV2Slot = slot<OpprettBucOgSedDtoV2>()
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns behandling
        every { eessiClient.opprettBucOgSedV2(capture(opprettBucOgSedDtoV2Slot)) } returns opprettSedDto
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns sedDataDto
        every { joarkFasade.validerDokumenterTilhørerSakOgHarTilgang(any(), any()) } returns Unit
        every { joarkFasade.hentJournalposterTilknyttetSak(any()) } returns listOf(journalpost)
        mockBehandlingsresultat()

        eessiService.opprettBucOgSed(BEHANDLING_ID, BucType.LA_BUC_01, listOf(mottakerBelgia1), dokumentReferanser)

        verify(exactly = 1) { eessiClient.opprettBucOgSedV2(any<OpprettBucOgSedDtoV2>()) }
        verify(exactly = 0) { eessiClient.opprettBucOgSed(any<SedDataDto>(), any(), any<BucType>(), any(), any()) }

        val forventetOpprettBucOgSedDtoV2 = OpprettBucOgSedDtoV2(
            bucType = BucType.LA_BUC_01,
            sedDataDto = sedDataDto,
            vedlegg = journalpost.vedleggListe.map{
                VedleggReferanse(
                    journalpost.journalpostId,
                    it.dokumentId ?: error("dokumentId burde ikke være null her"),
                    it.tittel
                )
            },
            sendAutomatisk = false,
            oppdaterEksisterende = false
        )
        opprettBucOgSedDtoV2Slot.captured shouldBe forventetOpprettBucOgSedDtoV2
    }

    @Test
    fun `opprettOgSendSed medFeatureToggle MELOSYS_BRUK_OPPRETT_BUC_OG_SED_V2 på kallerOpprettBucOgSedV2`() {
        unleash.enable(ToggleName.MELOSYS_BRUK_OPPRETT_BUC_OG_SED_V2)

        val behandling = lagBehandling()
        val sedDataDto = SedDataDto()

        val journalpost = lagJournalpost(listOf(
            lagArkivDokument("hoved"),
            lagArkivDokument("dok1"),
            lagArkivDokument("dok2"))
        )

        val dokumentReferanser = journalpost.vedleggListe.map {
            DokumentReferanse(journalpost.journalpostId, it.dokumentId ?: error("dokumentId burde ikke være null her"))
        }

        val opprettBucOgSedDtoV2Slot = slot<OpprettBucOgSedDtoV2>()
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns behandling
        every { eessiClient.opprettBucOgSedV2(capture(opprettBucOgSedDtoV2Slot)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns sedDataDto
        every { joarkFasade.hentJournalposterTilknyttetSak(any()) } returns listOf(journalpost)
        mockBehandlingsresultat()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf(mottakerBelgia1), BucType.LA_BUC_01, dokumentReferanser, null, null, false)

        verify(exactly = 1) { eessiClient.opprettBucOgSedV2(any<OpprettBucOgSedDtoV2>()) }
        verify(exactly = 0) { eessiClient.opprettBucOgSed(any<SedDataDto>(), any(), any<BucType>(), any(), any()) }

        val forventetOpprettBucOgSedDtoV2 = OpprettBucOgSedDtoV2(
            bucType = BucType.LA_BUC_01,
            sedDataDto = sedDataDto,
            vedlegg = journalpost.vedleggListe.map{
                VedleggReferanse(
                    journalpost.journalpostId,
                    it.dokumentId ?: error("dokumentId burde ikke være null her"),
                    it.tittel
                )
            },
            sendAutomatisk = true,
            oppdaterEksisterende = true
        )
        opprettBucOgSedDtoV2Slot.captured shouldBe forventetOpprettBucOgSedDtoV2
    }

    @Test
    fun `hentMottakerinstitusjoner forventListeMedRettType`() {
        every { eessiClient.hentMottakerinstitusjoner(any(), any<List<String>>()) } returns listOf(
            Institusjon("1", "Test1", "NO"),
            Institusjon("2", "Test2", "NO")
        )

        val mottakerinstitusjoner = eessiService.hentEessiMottakerinstitusjoner("LA_BUC_01", listOf("FR"))

        verify { eessiClient.hentMottakerinstitusjoner(any(), any<List<String>>()) }
        mottakerinstitusjoner shouldHaveSize 2
        mottakerinstitusjoner.forEach { it.shouldBeInstanceOf<Institusjon>() }
    }

    @Test
    fun `hentTilknyttedeBucer forventListeMedRettType`() {
        every { eessiClient.hentTilknyttedeBucer(any(), any<List<String>>()) } returns listOf(
            easyRandom.nextObject(BucInformasjon::class.java),
            easyRandom.nextObject(BucInformasjon::class.java),
            easyRandom.nextObject(BucInformasjon::class.java)
        )

        val tilknyttedeBucer = eessiService.hentTilknyttedeBucer(123L, listOf("utkast", "sendt"))

        verify { eessiClient.hentTilknyttedeBucer(any(), any<List<String>>()) }
        tilknyttedeBucer shouldHaveSize 3
        tilknyttedeBucer.forEach { it.shouldBeInstanceOf<BucInformasjon>() }
    }

    @Test
    fun `hentTilknyttedeBucer medFeilIClient forventException`() {
        every { eessiClient.hentTilknyttedeBucer(any(), any<List<String>>()) } throws IntegrasjonException("Error!")

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
        every { eessiClient.hentMelosysEessiMeldingFraJournalpostID("123") } returns melosysEessiMelding

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
        every { eessiClient.hentMelosysEessiMeldingFraJournalpostID("123") } returns melosysEessiMelding

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
        every { eessiClient.hentMelosysEessiMeldingFraJournalpostID("123") } returns melosysEessiMelding

        eessiService.støtterAutomatiskBehandling("123") shouldBe false
    }

    @Test
    fun `støtterAutomatiskBehandling a003ikkeUtpekt verifiserStøtterAutomatiskBehandling`() {
        val melosysEessiMelding = MelosysEessiMelding().apply {
            lovvalgsland = Landkoder.SE.name
            sedType = "A003"
        }
        every { eessiClient.hentMelosysEessiMeldingFraJournalpostID("123") } returns melosysEessiMelding

        eessiService.støtterAutomatiskBehandling("123") shouldBe true
    }

    @Test
    fun `støtterAutomatiskBehandling a003erUtpekt verifiserStøtterIkkeAutomatiskBehandling`() {
        val melosysEessiMelding = MelosysEessiMelding().apply {
            lovvalgsland = Landkoder.NO.name
            sedType = "A003"
        }
        every { eessiClient.hentMelosysEessiMeldingFraJournalpostID("123") } returns melosysEessiMelding

        eessiService.støtterAutomatiskBehandling("123") shouldBe true
    }

    @Test
    fun `hentSakForRinaSaksnummer forventOptionalIkkePresent`() {
        every { eessiClient.hentSakForRinasaksnummer(any()) } returns emptyList()

        val res = eessiService.finnSakForRinasaksnummer("123")

        res.isEmpty shouldBe true
    }

    @Test
    fun `hentSakForRinaSaksnummer forventOptionalPresent`() {
        every { eessiClient.hentSakForRinasaksnummer(any()) } returns listOf(SaksrelasjonDto(123L, "123", "123"))

        val res = eessiService.finnSakForRinasaksnummer("123")

        res.isPresent shouldBe true
    }

    @Test
    fun `lagreSaksrelasjon validerInput`() {
        every { eessiClient.lagreSaksrelasjon(any()) } returns Unit

        eessiService.lagreSaksrelasjon(123L, "123", "312")

        verify { eessiClient.lagreSaksrelasjon(any()) }
    }

    @Test
    fun `sendAnmodningUnntakSvar forventKall`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            saksopplysning {
                type = SaksopplysningType.SEDOPPL
                sedDokument { }
            }
        }

        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns behandling
        every { dokumentdataGrunnlagFactory.av(any(), any()) } returns mockk<SedDataGrunnlagUtenSoknad>()
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), any<SedType>()) } returns Unit
        mockBehandlingsresultat()

        eessiService.sendAnmodningUnntakSvar(BEHANDLING_ID, null)

        verify { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) }
        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.ANMODNINGSPERIODE)) }
        verify { dokumentdataGrunnlagFactory.av(any(), any()) }
        verify { eessiClient.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), eq(SedType.A002)) }
    }

    @Test
    fun `sendGodkjenningArbeidFlereLand should work correctly`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            fagsak {
                medGsakSaksnummer()
            }
            saksopplysning {
                type = SaksopplysningType.SEDOPPL
                sedDokument { }
            }
        }

        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns behandling
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { dokumentdataGrunnlagFactory.av(any(), any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), any<SedType>()) } returns Unit
        mockBehandlingsresultat()

        eessiService.sendGodkjenningArbeidFlereLand(BEHANDLING_ID, null)

        verify { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) }
        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.LOVVALGSPERIODE)) }
        verify { dokumentdataGrunnlagFactory.av(any(), any()) }
        verify { eessiClient.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), eq(SedType.A012)) }
    }

    @Test
    fun `sendGodkjenningArbeidFlereLand feiler ikke når x008 utsending feiler`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                medGsakSaksnummer()
            }
            saksopplysning {
                type = SaksopplysningType.SEDOPPL
                sedDokument { }
            }
        }

        val sedInformasjon = SedInformasjon("1", "2", LocalDate.now(), LocalDate.now(), "A012", "whatever", null)
        val bucInformasjon = BucInformasjon("1", true, "LA_BUC_02", LocalDate.now(), null, listOf(sedInformasjon))
        val bucInformasjonListe = listOf(bucInformasjon)

        every { eessiClient.hentTilknyttedeBucer(eq(FagsakTestFactory.GSAK_SAKSNUMMER), any()) } returns bucInformasjonListe
        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns behandling
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { dokumentdataGrunnlagFactory.av(any(), any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), eq(SedType.A012)) } returns Unit
        every { eessiClient.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), eq(SedType.X008)) } throws RuntimeException()
        mockBehandlingsresultat()

        eessiService.sendGodkjenningArbeidFlereLand(BEHANDLING_ID, null)

        verify(exactly = 2) { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) }
        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.LOVVALGSPERIODE)) }
        verify(exactly = 2) { dokumentdataGrunnlagFactory.av(any(), any()) }
        verify { eessiClient.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), eq(SedType.A012)) }
    }

    @Test
    fun `sendAvslagUtpekingSvar feiler ikke når x008 utsending feiler`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                medGsakSaksnummer()
            }
            saksopplysning {
                type = SaksopplysningType.SEDOPPL
                sedDokument { }
            }
        }

        val sedInformasjon = SedInformasjon("1", "2", LocalDate.now(), LocalDate.now(), "A012", "whatever", null)
        val bucInformasjon = BucInformasjon("1", true, "LA_BUC_02", LocalDate.now(), null, listOf(sedInformasjon))
        val bucInformasjonListe = listOf(bucInformasjon)

        every { eessiClient.hentTilknyttedeBucer(eq(FagsakTestFactory.GSAK_SAKSNUMMER), any()) } returns bucInformasjonListe
        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns behandling
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        every { dokumentdataGrunnlagFactory.av(any(), eq(SedType.X008)) } returns mockk<SedDataGrunnlagUtenSoknad>()
        every { sedDataBygger.lagUtkast(any(), any(), any()) } returns SedDataDto()
        every { eessiClient.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), eq(SedType.A004)) } returns Unit
        every { eessiClient.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), eq(SedType.X008)) } throws RuntimeException()
        mockBehandlingsresultat()

        val utpekingAvvis = UtpekingAvvis().apply {
            etterspørInformasjon = false
        }

        eessiService.sendAvslagUtpekingSvar(BEHANDLING_ID, utpekingAvvis)

        verify(exactly = 2) { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) }
        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.LOVVALGSPERIODE)) }
        verify { dokumentdataGrunnlagFactory.av(any(), any()) }
        verify { eessiClient.sendSedPåEksisterendeBuc(any<SedDataDto>(), any(), eq(SedType.A004)) }
    }

    @Test
    fun `genererSedPdf sedA001 medlemsperiodeTypeAnmodningsperiode`() {
        val pdf = "pdf".toByteArray()
        every { eessiClient.genererSedPdf(any(), any()) } returns pdf
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        val result = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A001)

        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.ANMODNINGSPERIODE)) }
        verify { eessiClient.genererSedPdf(any(), any()) }
        result shouldBe pdf
    }

    @Test
    fun `genererSedPdf sedA001 storbritanniaKonvFårTilpassetYtterligereInformasjon`() {
        val pdf = "pdf".toByteArray()
        every { eessiClient.genererSedPdf(any(), any()) } returns pdf
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()

        val sedPdfData = SedPdfData().apply {
            fritekst = "fritekst"
        }
        val behandlingsresultat = lagBehandlingsresultatMedStorbritanniaAnmodning()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiClient.genererSedPdf(capture(sedDataDtoSlot), any()) } returns pdf

        val result = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A001, sedPdfData)

        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.ANMODNINGSPERIODE)) }
        verify { eessiClient.genererSedPdf(any(), any()) }
        sedDataDtoSlot.captured.ytterligereInformasjon shouldBe "Issued under the EEA EFTA Convention. fritekst"
        result shouldBe pdf
    }

    @Test
    fun `genererSedPdf sedA003MedUtpekingsperiode medlemsperiodeTypeUtpekingsperiode`() {
        val pdf = "pdf".toByteArray()
        every { eessiClient.genererSedPdf(any(), any()) } returns pdf
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()

        val behandlingsresultat = lagBehandlingsresultatMedUtpekingsperiode()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val result = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A003)

        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.UTPEKINGSPERIODE)) }
        verify { eessiClient.genererSedPdf(any(), any()) }
        result shouldBe pdf
    }

    @Test
    fun `genererSedPdf sedA009 medlemsperiodeTypeLovvalgsperiode`() {
        val pdf = "pdf".toByteArray()
        every { eessiClient.genererSedPdf(any(), any()) } returns pdf
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()

        val behandlingsresultat = lagBehandlingsresultatMedUtpekingsperiode()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val result = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A009)

        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.LOVVALGSPERIODE)) }
        verify { eessiClient.genererSedPdf(any(), any()) }
        result shouldBe pdf
    }

    @Test
    fun `genererSedForhåndsvisning medSedPdfData verifiserSedDataDtoPreutfylt`() {
        val pdf = "pdf".toByteArray()
        every { eessiClient.genererSedPdf(any(), any()) } returns pdf
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        every { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        mockBehandlingsresultat()
        mockBehandling()

        val sedPdfData = SedPdfData().apply {
            nyttLovvalgsland = "SE"
        }

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiClient.genererSedPdf(capture(sedDataDtoSlot), any()) } returns pdf

        val result = eessiService.genererSedPdf(BEHANDLING_ID, SedType.A001, sedPdfData)

        verify { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) }
        verify { dokumentdataGrunnlagFactory.av(any()) }
        verify { sedDataBygger.lagUtkast(any<SedDataGrunnlag>(), any(), eq(PeriodeType.ANMODNINGSPERIODE)) }
        verify { eessiClient.genererSedPdf(any(), any()) }
        result shouldBe pdf

        val sedDataDto = sedDataDtoSlot.captured
        sedDataDto.utpekingAvvis.shouldNotBeNull().nyttLovvalgsland shouldBe sedPdfData.nyttLovvalgsland
    }

    @Test
    fun `hentSedTypeForAnmodningUnntakSvar forventA002`() {
        // lagBehandlingsresultat() already has AVSLAG as default
        val behandlingsresultat = lagBehandlingsresultat()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val sedType = eessiService.hentSedTypeForAnmodningUnntakSvar(BEHANDLING_ID)

        sedType shouldBe SedType.A002
    }

    @Test
    fun `hentSedTypeForAnmodningUnntakSvar forventA011`() {
        val behandlingsresultat = lagBehandlingsresultatMedInnvilgelse()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val sedType = eessiService.hentSedTypeForAnmodningUnntakSvar(BEHANDLING_ID)

        sedType shouldBe SedType.A011
    }

    private fun lagBehandlingsresultatMedInnvilgelse() = Behandlingsresultat.forTest {
        lovvalgsperiode {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            lovvalgsland = Land_iso2.SK
        }
        anmodningsperiode {
            anmodningsperiodeSvar {
                anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
            }
        }
    }

    @Test
    fun `validerOgAvklarMottakerInstitusjonerForBuc toMottakereToMottakerLandMottakereKorrektSatt returnererMottakerInstitusjoner`() {
        val bucType = BucType.LA_BUC_02
        val mottakerLand = listOf(Land_iso2.BE, Land_iso2.DE)
        val valgteMottakerInstitusjoner = setOf(mottakerBelgia1, mottakerTyskland1)

        every {
            eessiClient.hentMottakerinstitusjoner(bucType.name, setOf(Land_iso2.BE.kode, Land_iso2.DE.kode))
        } returns listOf(institusjonBelgia1, institusjonBelgia2, institusjonTyskland1, institusjonTyskland2)

        val avklarteMottakerInstitusjoner =
            eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType)

        verify { eessiClient.hentMottakerinstitusjoner(eq(bucType.name), any<Set<String>>()) }
        avklarteMottakerInstitusjoner shouldBe valgteMottakerInstitusjoner
    }

    @Test
    fun `validerOgAvklarMottakerInstitusjonerForBuc toMottakereSisteErIkkeEessiReady returnererTomListe`() {
        val bucType = BucType.LA_BUC_02
        val mottakerLand = listOf(Land_iso2.BE, Land_iso2.DE)
        val valgteMottakerInstitusjoner = setOf(mottakerBelgia1, mottakerTyskland1)

        every {
            eessiClient.hentMottakerinstitusjoner(bucType.name, setOf(Land_iso2.BE.kode, Land_iso2.DE.kode))
        } returns listOf(institusjonBelgia1, institusjonBelgia2)

        val avklarteMottakerInstitusjoner =
            eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType)

        verify { eessiClient.hentMottakerinstitusjoner(eq(bucType.name), any<Set<String>>()) }
        avklarteMottakerInstitusjoner.shouldBeEmpty()
    }

    @Test
    fun `validerOgAvklarMottakerInstitusjonerForBuc toLandInstitusjonManglerForSiste kasterException`() {
        val bucType = BucType.LA_BUC_02
        val mottakerLand = listOf(Land_iso2.BE, Land_iso2.DE)
        val valgteMottakerInstitusjoner = setOf(mottakerBelgia1)

        every {
            eessiClient.hentMottakerinstitusjoner(bucType.name, setOf(Landkoder.BE.kode, Landkoder.DE.kode))
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
            eessiClient.hentMottakerinstitusjoner(bucType.name, setOf(Landkoder.BE.kode, Landkoder.DE.kode))
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
            eessiClient.hentMottakerinstitusjoner(bucType.name, setOf(Landkoder.BE.kode, Landkoder.DE.kode))
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
            eessiClient.hentMottakerinstitusjoner(bucType.name, setOf(Landkoder.BE.kode, Landkoder.DE.kode))
        } returns listOf(institusjonBelgia)

        val avklarteMottakere = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(valgteMottakerInstitusjoner, mottakerLand, bucType)

        avklarteMottakere.shouldBeEmpty()
    }

    @Test
    fun `landErEessiReady toLandEtErEessiReady forventFalse`() {
        val bucType = BucType.LA_BUC_01
        val land = listOf(Land_iso2.SE, Land_iso2.DK)

        every { eessiClient.hentMottakerinstitusjoner(bucType.name, setOf(Land_iso2.SE.kode)) } returns listOf(
            Institusjon("2", "", "")
        )
        every { eessiClient.hentMottakerinstitusjoner(bucType.name, setOf(Land_iso2.DK.kode)) } returns emptyList()

        eessiService.landErEessiReady(bucType.name, land) shouldBe false
    }

    @Test
    fun `landErEessiReady toLandAlleErEessiReady forventTrue`() {
        val bucType = BucType.LA_BUC_01
        val land = listOf(Land_iso2.SE, Land_iso2.DK)

        every { eessiClient.hentMottakerinstitusjoner(eq(bucType.name), any()) } returns listOf(
            Institusjon("2", "", "")
        )

        eessiService.landErEessiReady(bucType.name, land) shouldBe true
    }

    @Test
    fun `kanOppretteSedPåBuc fårCreateTilbake true`() {
        every { eessiClient.hentMuligeAksjoner("5566") } returns listOf("5fcffb7e6a9640acac5a09abc5a08ef6 A004 Create")

        eessiService.kanOppretteSedTyperPåBuc("5566", SedType.A004) shouldBe true
    }

    @Test
    fun `kanOppretteSedPåBuc fårCreateTilbakePåFeilSed false`() {
        every { eessiClient.hentMuligeAksjoner("5566") } returns listOf("5fcffb7e6a9640acac5a09abc5a08ef6 A004 Create")

        eessiService.kanOppretteSedTyperPåBuc("5566", SedType.A011) shouldBe false
    }

    @Test
    fun `kanOppretteSedPåBuc tomListe false`() {
        every { eessiClient.hentMuligeAksjoner("5566") } returns emptyList()

        eessiService.kanOppretteSedTyperPåBuc("5566", SedType.A011) shouldBe false
    }

    // CDM 4.4 toggle tests
    @Test
    fun `opprettOgSendSed med toggle CDM_4_4 på setter a008Formaal på sedData`() {
        unleash.enable(ToggleName.MELOSYS_CDM_4_4)
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiClient.opprettBucOgSed(capture(sedDataDtoSlot), any(), any(), any(), any()) } returns OpprettSedDto()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_03, emptyList(), null, "arbeid_flere_land", false)

        sedDataDtoSlot.captured.a008Formaal shouldBe A008Formaal.ARBEID_FLERE_LAND
    }

    @Test
    fun `opprettOgSendSed med toggle CDM_4_4 av setter ikke a008Formaal på sedData`() {
        unleash.disable(ToggleName.MELOSYS_CDM_4_4)
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiClient.opprettBucOgSed(capture(sedDataDtoSlot), any(), any(), any(), any()) } returns OpprettSedDto()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_03, emptyList(), null, "arbeid_flere_land", false)

        sedDataDtoSlot.captured.a008Formaal shouldBe null
    }

    @Test
    fun `opprettOgSendSed med gyldig a008Formaal endringsmelding setter enum`() {
        unleash.enable(ToggleName.MELOSYS_CDM_4_4)
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiClient.opprettBucOgSed(capture(sedDataDtoSlot), any(), any(), any(), any()) } returns OpprettSedDto()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_03, emptyList(), null, "endringsmelding", false)

        sedDataDtoSlot.captured.a008Formaal shouldBe A008Formaal.ENDRINGSMELDING
    }

    @Test
    fun `opprettOgSendSed med null a008Formaal bruker default ARBEID_FLERE_LAND`() {
        unleash.enable(ToggleName.MELOSYS_CDM_4_4)
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiClient.opprettBucOgSed(capture(sedDataDtoSlot), any(), any(), any(), any()) } returns OpprettSedDto()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_03, emptyList(), null, null, false)

        // null bruker default ARBEID_FLERE_LAND
        sedDataDtoSlot.captured.a008Formaal shouldBe A008Formaal.ARBEID_FLERE_LAND
    }

    @Test
    fun `opprettOgSendSed med ugyldig a008Formaal bruker default ARBEID_FLERE_LAND`() {
        unleash.enable(ToggleName.MELOSYS_CDM_4_4)
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiClient.opprettBucOgSed(capture(sedDataDtoSlot), any(), any(), any(), any()) } returns OpprettSedDto()

        // Ugyldig verdi faller tilbake til default
        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_03, emptyList(), null, "ugyldig_formaal", false)

        sedDataDtoSlot.captured.a008Formaal shouldBe A008Formaal.ARBEID_FLERE_LAND
    }

    @Test
    fun `opprettOgSendSed med case-sensitiv feil a008Formaal bruker default ARBEID_FLERE_LAND`() {
        unleash.enable(ToggleName.MELOSYS_CDM_4_4)
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiClient.opprettBucOgSed(capture(sedDataDtoSlot), any(), any(), any(), any()) } returns OpprettSedDto()

        // Store bokstaver er ugyldig, faller tilbake til default
        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_03, emptyList(), null, "ARBEID_FLERE_LAND", false)

        sedDataDtoSlot.captured.a008Formaal shouldBe A008Formaal.ARBEID_FLERE_LAND
    }

    @Test
    fun `opprettOgSendSed med ugyldig a008Formaal setter ikke når toggle er av`() {
        unleash.disable(ToggleName.MELOSYS_CDM_4_4)
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiClient.opprettBucOgSed(capture(sedDataDtoSlot), any(), any(), any(), any()) } returns OpprettSedDto()

        // Når toggle er av, settes ikke a008Formaal uansett input
        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_03, emptyList(), null, "ugyldig_formaal", false)

        sedDataDtoSlot.captured.a008Formaal.shouldBeNull()
    }

    @Test
    fun `opprettOgSendSed med toggle CDM_4_4 på setter erFjernarbeidTWFA på sedData`() {
        unleash.enable(ToggleName.MELOSYS_CDM_4_4)
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiClient.opprettBucOgSed(capture(sedDataDtoSlot), any(), any(), any(), any()) } returns OpprettSedDto()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_01, emptyList(), null, null, true)

        sedDataDtoSlot.captured.erFjernarbeidTWFA shouldBe true
    }

    @Test
    fun `opprettOgSendSed med toggle CDM_4_4 av setter ikke erFjernarbeidTWFA på sedData`() {
        unleash.disable(ToggleName.MELOSYS_CDM_4_4)
        every { sedDataBygger.lag(any<SedDataGrunnlag>(), any<Behandlingsresultat>(), any<PeriodeType>()) } returns SedDataDto()
        every { eessiClient.opprettBucOgSed(any(), any(), any(), eq(true), eq(true)) } returns OpprettSedDto()
        every { dokumentdataGrunnlagFactory.av(any()) } returns mockk<SedDataGrunnlagMedSoknad>()
        mockBehandling()
        mockBehandlingsresultat()

        val sedDataDtoSlot = slot<SedDataDto>()
        every { eessiClient.opprettBucOgSed(capture(sedDataDtoSlot), any(), any(), any(), any()) } returns OpprettSedDto()

        eessiService.opprettOgSendSed(BEHANDLING_ID, listOf("SE:123"), BucType.LA_BUC_01, emptyList(), null, null, true)

        sedDataDtoSlot.captured.erFjernarbeidTWFA.shouldBeNull()
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
