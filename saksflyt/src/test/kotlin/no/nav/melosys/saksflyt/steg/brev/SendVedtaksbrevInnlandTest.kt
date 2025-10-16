package no.nav.melosys.saksflyt.steg.brev

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.brev.NorskMyndighet
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3A
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.brev.BrevData
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class SendVedtaksbrevInnlandTest {
    @MockK
    private lateinit var behandlingService: BehandlingService
    @MockK
    private lateinit var prosessinstansService: ProsessinstansService
    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService
    @MockK
    private lateinit var saksbehandlingRegler: SaksbehandlingRegler
    @MockK
    private lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    private lateinit var behandling: Behandling
    private val fakeUnleash = FakeUnleash()
    private lateinit var sendVedtaksbrevInnland: SendVedtaksbrevInnland
    private val doksysBrevbestillingSlot = slot<DoksysBrevbestilling>()

    @BeforeEach
    fun setUp() {
        behandling = lagBehandling()
        fakeUnleash.resetAll()
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLINGID) } returns behandling
        every { avklarteVirksomheterService.hentNorskeSelvstendigeForetak(any()) } returns emptyList()
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), any(), any()) } just Runs
        every { prosessinstansService.opprettProsessinstansSendBrev(any(), any(), any()) } just Runs
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any(), any()) } returns false

        sendVedtaksbrevInnland = SendVedtaksbrevInnland(
            behandlingService,
            behandlingsresultatService,
            prosessinstansService,
            saksbehandlingRegler,
            avklarteVirksomheterService,
            fakeUnleash
        )
    }

    @Test
    fun `utfør med flere lovvalgsperioder gir unntak`() {
        val lovvalgsperiode1 = lagLovvalgsperiodeArt16_1()
        val lovvalgsperiode2 = lagLovvalgsperiode(FO_883_2004_ART12_1, LocalDate.now().plusDays(30), Land_iso2.DK, false)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, setOf(lovvalgsperiode1, lovvalgsperiode2), null)
        val prosessinstans = lagProsessinstans()


        val exception = shouldThrow<UnsupportedOperationException> {
            sendVedtaksbrevInnland.utfør(prosessinstans)
        }
        exception.message shouldContain "Flere enn en lovvalgsperiode er ikke støttet"
    }

    @Test
    fun `utfør uten periode feiler`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultatUtenPerioder(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND)
        val prosessinstans = lagProsessinstans()


        val exception = shouldThrow<NoSuchElementException> {
            sendVedtaksbrevInnland.utfør(prosessinstans)
        }
        exception.message shouldContain "Ingen lovvalgsperiode finnes"
    }

    @Test
    fun `utfør innvilgelse 12_1 vedtak og kopi til skatt sendes ikke`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART12_1))
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), capture(doksysBrevbestillingSlot), any()) } just Runs


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        val mottakere = listOf(Mottaker.medRolle(Mottakerroller.BRUKER))
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any(), mottakere) }
        doksysBrevbestillingSlot.captured.produserbartdokument shouldBe INNVILGELSE_YRKESAKTIV
    }

    @Test
    fun `utfør innvilgelse Efta vedtak`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(KONV_EFTA_STORBRITANNIA_ART13_3A))
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), any(), any()) } just Runs


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        verify(exactly = 2) { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any(), any()) }
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, match { it.produserbartdokument == INNVILGELSE_EFTA_STORBRITANNIA }, any()) }
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, match { it.produserbartdokument == ATTEST_A1 }, any()) }
    }

    @Test
    fun `utfør innvilgelse 11_3 SokkelSkip vedtak`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultatMedAvklarteFakta(
                lagInnvilgetLovvalgsperiode(FO_883_2004_ART11_3A),
                setOf(lagAvklarteFakta(Avklartefaktatyper.YRKESGRUPPE, AvklartYrkesgruppeType.SOKKEL_ELLER_SKIP.name, ""))
            )
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), capture(doksysBrevbestillingSlot), any()) } just Runs


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        verify(exactly = 1) { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any(), any()) }
        doksysBrevbestillingSlot.captured.produserbartdokument shouldBe INNVILGELSE_YRKESAKTIV
    }

    @Test
    fun `utfør innvilgelse 11_3 Yrkesaktiv vedtak`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultatMedAvklarteFakta(
                lagInnvilgetLovvalgsperiode(FO_883_2004_ART11_3A),
                setOf(lagAvklarteFakta(Avklartefaktatyper.YRKESGRUPPE, AvklartYrkesgruppeType.ORDINAER.name, ""))
            )
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), any(), any()) } just Runs


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        verify(exactly = 2) { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any(), any()) }
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, match { it.produserbartdokument == INNVILGELSE_EFTA_STORBRITANNIA }, any()) }
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, match { it.produserbartdokument == ATTEST_A1 }, any()) }
    }

    @Test
    fun `utfør innvilgelse Efta vedtak sender ikke ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK når selvstendig lovvalgsbestemmelse`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_2))

        val prosessinstans = lagProsessinstans().apply {
            setData(ProsessDataKey.ARBEIDSGIVER_SKAL_HA_KOPI, true)
        }


        sendVedtaksbrevInnland.utfør(prosessinstans)


        verify(exactly = 2) { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any(), any()) }
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, match { it.produserbartdokument == INNVILGELSE_EFTA_STORBRITANNIA }, any()) }
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, match { it.produserbartdokument == ATTEST_A1 }, any()) }
    }

    @Test
    fun `utfør innvilgelse Efta vedtak sender ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4))
        every { prosessinstansService.opprettProsessinstansSendBrev(any(), capture(doksysBrevbestillingSlot), any()) } just Runs

        val prosessinstans = lagProsessinstans().apply {
            setData(ProsessDataKey.ARBEIDSGIVER_SKAL_HA_KOPI, true)
        }


        sendVedtaksbrevInnland.utfør(prosessinstans)


        verify(exactly = 1) { prosessinstansService.opprettProsessinstansSendBrev(behandling, any(), Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER)) }
        doksysBrevbestillingSlot.captured.produserbartdokument shouldBe ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK
    }

    @Test
    fun `utfør innvilgelse Efta vedtak sender ikke ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK når mottaker er selvstendig`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(KONV_EFTA_STORBRITANNIA_ART18_1))
        every { prosessinstansService.opprettProsessinstansSendBrev(any(), capture(doksysBrevbestillingSlot), any()) } just Runs

        val prosessinstans = lagProsessinstans().apply {
            setData(ProsessDataKey.ARBEIDSGIVER_SKAL_HA_KOPI, true)
        }


        sendVedtaksbrevInnland.utfør(prosessinstans)


        verify(exactly = 1) { prosessinstansService.opprettProsessinstansSendBrev(behandling, any(), Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER)) }
        doksysBrevbestillingSlot.captured.produserbartdokument shouldBe ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK
    }

    @Test
    fun `utfør avslag Efta 118 vedtak sender avslag efta storbritannia brev`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagLovvalgsperiode(KONV_EFTA_STORBRITANNIA_ART18_1, LocalDate.now(), Land_iso2.HR, false))
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), any(), any()) } just Runs
        every { prosessinstansService.opprettProsessinstansSendBrev(any(), any(), any()) } just Runs


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        val mottakere = listOf(Mottaker.medRolle(Mottakerroller.BRUKER), Mottaker.av(NorskMyndighet.HELFO), Mottaker.av(NorskMyndighet.SKATTEETATEN))

        verify(exactly = 1) { prosessinstansService.opprettProsessinstanserSendBrev(behandling, match { it.produserbartdokument == AVSLAG_EFTA_STORBRITANNIA }, mottakere) }
        verify(exactly = 1) { prosessinstansService.opprettProsessinstansSendBrev(behandling, match { it.produserbartdokument == ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK }, Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER)) }
    }

    @Test
    fun `utfør innvilgelse 11_4 sender ikke orientering til arbeidsgiver`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART11_4_2))
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), capture(doksysBrevbestillingSlot), any()) } just Runs


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        val mottakere = listOf(Mottaker.medRolle(Mottakerroller.BRUKER))
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any(), mottakere) }
        doksysBrevbestillingSlot.captured.produserbartdokument shouldBe INNVILGELSE_YRKESAKTIV
        verify(exactly = 0) { prosessinstansService.opprettProsessinstansSendBrev(behandling, any<DoksysBrevbestilling>(), Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER)) }
    }

    @Test
    fun `utfør innvilgelse 13_1A vedtak og kopi til skatt sendes`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART13_1A))
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), capture(doksysBrevbestillingSlot), any()) } just Runs


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        val mottakere = listOf(Mottaker.medRolle(Mottakerroller.BRUKER))
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any(), mottakere) }
        doksysBrevbestillingSlot.captured.produserbartdokument shouldBe INNVILGELSE_YRKESAKTIV_FLERE_LAND
    }

    @Test
    fun `utfør innvilgelse FO_883_2004_ART11_2 ikke yrkesaktiv vedtak sendes`() {
        behandling.tema = Behandlingstema.IKKE_YRKESAKTIV
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_2))
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(Sakstyper.EU_EOS, Behandlingstema.IKKE_YRKESAKTIV) } returns true
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), capture(doksysBrevbestillingSlot), any()) } just Runs


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        val mottakere = listOf(Mottaker.medRolle(Mottakerroller.BRUKER))
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any(), mottakere) }
        doksysBrevbestillingSlot.captured.produserbartdokument shouldBe IKKE_YRKESAKTIV_VEDTAKSBREV
    }

    @Test
    fun `utfør utpeking 13_1B1 sender orienteringsbrev`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagUtpekingsperiode(), lagLovvalgsperiode(FO_883_2004_ART13_1B1, LocalDate.now(), Land_iso2.SE, true))
        every { prosessinstansService.opprettProsessinstansSendBrev(any(), capture(doksysBrevbestillingSlot), any()) } just Runs


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        verify { prosessinstansService.opprettProsessinstansSendBrev(behandling, any(), Mottaker.medRolle(Mottakerroller.BRUKER)) }
        doksysBrevbestillingSlot.captured.produserbartdokument shouldBe ORIENTERING_UTPEKING_UTLAND
    }

    @Test
    fun `utfør innvilgelse 13_1A sender ikke innvilgelse til arbeidsgiver`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART13_1A))
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), any(), any()) } just Runs


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        verify(exactly = 0) { prosessinstansService.opprettProsessinstansSendBrev(behandling, any<DoksysBrevbestilling>(), Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER)) }
    }

    @Test
    fun `utfør innvilgelse 13_1A med utenlandsk foretak sender brev til statlig skatteoppkreving`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART13_1A))
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), capture(doksysBrevbestillingSlot), any()) } just Runs

        val arbeidsgiverUtland = ForetakUtland().apply {
            selvstendigNæringsvirksomhet = false
        }
        behandling.mottatteOpplysninger!!.mottatteOpplysningerData.foretakUtland.add(arbeidsgiverUtland)


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        val mottakere = listOf(Mottaker.medRolle(Mottakerroller.BRUKER), Mottaker.av(NorskMyndighet.SKATTEINNKREVER_UTLAND))
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any(), mottakere) }
        doksysBrevbestillingSlot.captured.produserbartdokument shouldBe INNVILGELSE_YRKESAKTIV_FLERE_LAND
    }

    @Test
    fun `utfør innvilgelse 11_3_A med selvstendig utenlandsk foretak sender ikke brev til statlig skatteoppkreving`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultatMedAvklarteFakta(
                lagInnvilgetLovvalgsperiode(FO_883_2004_ART11_3A),
                setOf(lagAvklarteFakta(Avklartefaktatyper.YRKESGRUPPE, AvklartYrkesgruppeType.ORDINAER.name, ""))
            )
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), any(), any()) } just Runs

        val arbeidsgiverUtland = ForetakUtland().apply {
            selvstendigNæringsvirksomhet = true
        }
        behandling.mottatteOpplysninger!!.mottatteOpplysningerData.foretakUtland.add(arbeidsgiverUtland)


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        val mottakere = listOf(Mottaker.medRolle(Mottakerroller.BRUKER))
        verify(exactly = 2) { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any(), mottakere) }
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, match { it.produserbartdokument == INNVILGELSE_EFTA_STORBRITANNIA }, any()) }
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, match { it.produserbartdokument == ATTEST_A1 }, any()) }
    }

    @Test
    fun `utfør innvilgelse 16_1 med utenlandsk selvstendig arbeid sender ikke brev til statlig skatteoppkreving`() {
        val behandlingsresultat = lagBehandlingsresultat(lagLovvalgsperiodeArt16_1()).apply {
            anmodningsperioder.add(lagAnmodningsperiodeMedSvar())
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns behandlingsresultat
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), any(), any()) } just Runs

        val utenlandskSelvstendigVirksomhet = ForetakUtland().apply {
            selvstendigNæringsvirksomhet = true
        }
        behandling.mottatteOpplysninger!!.mottatteOpplysningerData.foretakUtland.add(utenlandskSelvstendigVirksomhet)


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        val mottakere = listOf(Mottaker.medRolle(Mottakerroller.BRUKER))
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any<DoksysBrevbestilling>(), mottakere) }
    }

    @Test
    fun `utfør innvilgelse 16_1 saksbehandler ikke satt bruker saksbehandler som anmodet om unntak ved brevbestilling`() {
        val behandlingsresultat = lagBehandlingsresultat(lagLovvalgsperiodeArt16_1()).apply {
            anmodningsperioder.add(lagAnmodningsperiodeMedSvar())
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns behandlingsresultat
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), capture(doksysBrevbestillingSlot), any()) } just Runs

        val prosessinstans = lagProsessinstans()


        sendVedtaksbrevInnland.utfør(prosessinstans)


        prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER) shouldBe null
        val mottakere = listOf(Mottaker.medRolle(Mottakerroller.BRUKER))
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any(), mottakere) }
        doksysBrevbestillingSlot.captured.avsenderID shouldBe "Z111111"
    }

    @Test
    fun `utfør innvilgelse 12_1 sender ikke brev til statlig skatteoppkreving`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART12_1))
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), any(), any()) } just Runs

        behandling.mottatteOpplysninger!!.mottatteOpplysningerData.foretakUtland.add(ForetakUtland())


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        val mottakere = listOf(Mottaker.medRolle(Mottakerroller.BRUKER))
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any<DoksysBrevbestilling>(), mottakere) }
    }

    @Test
    fun `utfør innvilgelse 12_1 sender innvilgelse til arbeidsgiver`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART12_1))
        every { prosessinstansService.opprettProsessinstansSendBrev(any(), capture(doksysBrevbestillingSlot), any()) } just Runs


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        verify { prosessinstansService.opprettProsessinstansSendBrev(behandling, any(), Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER)) }
        doksysBrevbestillingSlot.captured.produserbartdokument shouldBe ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK
    }

    @Test
    fun `utfør avslag 12_1 sender til Helfo og Skatt`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagLovvalgsperiode(FO_883_2004_ART12_1, LocalDate.now(), Land_iso2.HR, false))
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), capture(doksysBrevbestillingSlot), any()) } just Runs


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        val mottakere = listOf(Mottaker.medRolle(Mottakerroller.BRUKER), Mottaker.av(NorskMyndighet.HELFO), Mottaker.av(NorskMyndighet.SKATTEETATEN))
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any(), mottakere) }
        doksysBrevbestillingSlot.captured.produserbartdokument shouldBe AVSLAG_YRKESAKTIV
    }

    @Test
    fun `utfør avslag 12_1 med arbeidsgiver sender til arbeidsgiver`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagLovvalgsperiode(FO_883_2004_ART12_1, LocalDate.now(), Land_iso2.HR, false))
        every { prosessinstansService.opprettProsessinstansSendBrev(any(), capture(doksysBrevbestillingSlot), any()) } just Runs

        val arbeidsgiver = Aktoer().apply {
            rolle = Aktoersroller.ARBEIDSGIVER
            orgnr = "123456789"
        }
        behandling.fagsak.leggTilAktør(arbeidsgiver)


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        verify { prosessinstansService.opprettProsessinstansSendBrev(behandling, any(), Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER)) }
        doksysBrevbestillingSlot.captured.produserbartdokument shouldBe ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK
    }

    @Test
    fun `utfør på innvilgelsesbrev bestemt av 12_2 sender brev til arbeidsgiver men ikke kopi til skatt`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART12_2))
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), any(), any()) } just Runs


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        val mottakere = listOf(Mottaker.medRolle(Mottakerroller.BRUKER))
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any<DoksysBrevbestilling>(), mottakere) }
    }

    @Test
    fun `utfør på innvilgelsesbrev bestemt ART13_3A sender brev til skatt og kopi til arbeidsgiver`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns
            lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(KONV_EFTA_STORBRITANNIA_ART13_3A))
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), any(), any()) } just Runs
        every { prosessinstansService.opprettProsessinstansSendBrev(any(), capture(doksysBrevbestillingSlot), any()) } just Runs


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        val mottakere = listOf(Mottaker.medRolle(Mottakerroller.BRUKER))
        verify(exactly = 2) { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any(), mottakere) }
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, match { it.produserbartdokument == INNVILGELSE_EFTA_STORBRITANNIA }, any()) }
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, match { it.produserbartdokument == ATTEST_A1 }, any()) }

        verify { prosessinstansService.opprettProsessinstansSendBrev(behandling, any(), Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER)) }
        doksysBrevbestillingSlot.captured.produserbartdokument shouldBe ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK
    }

    @Test
    fun `utfør på fastsatt lovvalg i Norge uten innvilget bestemmelse går til feilet maskinelt`() {
        val behandlingsresultat = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ANNET)).apply {
            type = Behandlingsresultattyper.IKKE_FASTSATT
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns behandlingsresultat


        val exception = shouldThrow<FunksjonellException> {
            sendVedtaksbrevInnland.utfør(lagProsessinstans())
        }
        exception.message!! shouldContain "Vedtaksbrev kan ikke sendes for behandling"
    }

    @Test
    fun `utfør på innvilgelsesbrev 12_1 med begrunnelseskode forkortet periode oppdaterer brevdata`() {
        val behandlingsresultat = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART12_1)).apply {
            avklartefakta.add(
                Avklartefakta(
                    this,
                    Avklartefaktatyper.AARSAK_ENDRING_PERIODE.kode,
                    Avklartefaktatyper.AARSAK_ENDRING_PERIODE,
                    null,
                    Endretperiode.ENDRINGER_ARBEIDSSITUASJON.kode
                )
            )
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID) } returns behandlingsresultat
        every { prosessinstansService.opprettProsessinstanserSendBrev(any(), capture(doksysBrevbestillingSlot), any()) } just Runs


        sendVedtaksbrevInnland.utfør(lagProsessinstans())


        val mottakere = listOf(Mottaker.medRolle(Mottakerroller.BRUKER))
        verify { prosessinstansService.opprettProsessinstanserSendBrev(behandling, any(), mottakere) }
        doksysBrevbestillingSlot.captured.begrunnelseKode shouldBe Endretperiode.ENDRINGER_ARBEIDSSITUASJON.kode
    }

    private fun lagProsessinstans() = Prosessinstans.forTest {
        type = ProsessType.IVERKSETT_VEDTAK_EOS
        status = ProsessStatus.KLAR
        behandling {
            id = BEHANDLINGID
            fagsak = lagFagsak()
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            type = Behandlingstyper.FØRSTEGANG
            mottatteOpplysninger = MottatteOpplysninger().apply {
                mottatteOpplysningerData = Soeknad()
            }
        }
        medData(ProsessDataKey.BREVDATA, BrevData())
    }

    private fun lagBehandling() = Behandling.forTest {
        id = BEHANDLINGID
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        mottatteOpplysninger = MottatteOpplysninger().apply {
            mottatteOpplysningerData = Soeknad()
        }
        fagsak {
            gsakSaksnummer = 123456789L
            leggTilAktør(Aktoer().apply {
                aktørId = "1"
                rolle = Aktoersroller.BRUKER
            })
            leggTilAktør(Aktoer().apply {
                aktørId = "2"
                rolle = Aktoersroller.TRYGDEMYNDIGHET
                institusjonID = "SE:sesese123"
            })
            leggTilAktør(Aktoer().apply {
                rolle = Aktoersroller.ARBEIDSGIVER
                orgnr = "123456789"
            })
        }
    }

    private fun lagFagsak() = Fagsak.forTest {
        gsakSaksnummer = 123456789L
        leggTilAktør(Aktoer().apply {
            aktørId = "1"
            rolle = Aktoersroller.BRUKER
        })
        leggTilAktør(Aktoer().apply {
            aktørId = "2"
            rolle = Aktoersroller.TRYGDEMYNDIGHET
            institusjonID = "SE:sesese123"
        })
        leggTilAktør(Aktoer().apply {
            rolle = Aktoersroller.ARBEIDSGIVER
            orgnr = "123456789"
        })
    }

    private fun lagAnmodningsperiodeMedSvar() = Anmodningsperiode().apply {
        anmodetAv = "Z111111"
        anmodningsperiodeSvar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
        }
    }

    private fun lagLovvalgsperiodeArt16_1() = lagInnvilgetLovvalgsperiode(FO_883_2004_ART16_1)

    private fun lagInnvilgetLovvalgsperiode(bestemmelse: LovvalgBestemmelse) =
        lagLovvalgsperiode(bestemmelse, LocalDate.now(), Land_iso2.NO, true)

    private fun lagLovvalgsperiode(bestemmelse: LovvalgBestemmelse, fom: LocalDate, land: Land_iso2, innvilget: Boolean) = Lovvalgsperiode().apply {
        this.fom = fom
        tom = fom.plusDays(1)
        lovvalgsland = land
        this.bestemmelse = bestemmelse
        innvilgelsesresultat = if (innvilget) {
            InnvilgelsesResultat.INNVILGET
        } else {
            InnvilgelsesResultat.AVSLAATT
        }
    }

    private fun lagUtpekingsperiode() = Utpekingsperiode().apply {
        fom = LocalDate.MIN
        tom = LocalDate.MIN.plusDays(1)
        lovvalgsland = Land_iso2.PL
        bestemmelse = FO_883_2004_ART13_1B1
    }

    private fun lagBehandlingsresultat(periode: Lovvalgsperiode) =
        lagBehandlingsresultat(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, setOf(periode), Land_iso2.NO)

    private fun lagBehandlingsresultat(utpekingsperiode: Utpekingsperiode, lovvalgsperiode: Lovvalgsperiode) = Behandlingsresultat().apply {
        utpekingsperioder = mutableSetOf(utpekingsperiode)
        lovvalgsperioder = mutableSetOf(lovvalgsperiode)
        type = Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND
        fastsattAvLand = Land_iso2.NO
    }

    private fun lagBehandlingsresultat(type: Behandlingsresultattyper, perioder: Set<Lovvalgsperiode>, land: Land_iso2?) = Behandlingsresultat().apply {
        lovvalgsperioder = perioder.toMutableSet()
        this.type = type
        fastsattAvLand = land
        vilkaarsresultater = mutableSetOf()
    }

    private fun lagBehandlingsresultatMedAvklarteFakta(periode: Lovvalgsperiode, avklartefakta: Set<Avklartefakta>) = Behandlingsresultat().apply {
        lovvalgsperioder = mutableSetOf(periode)
        type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        fastsattAvLand = Land_iso2.NO
        vilkaarsresultater = mutableSetOf()
        this.avklartefakta = avklartefakta.toMutableSet()
    }

    private fun lagBehandlingsresultatUtenPerioder(behandlingstype: Behandlingsresultattyper) =
        lagBehandlingsresultat(behandlingstype, emptySet(), Land_iso2.NO)

    private fun lagAvklarteFakta(type: Avklartefaktatyper, fakta: String, subjekt: String) = Avklartefakta().apply {
        this.subjekt = subjekt
        this.type = type
        this.fakta = fakta
    }

    companion object {
        private const val BEHANDLINGID = 1L
    }
}
