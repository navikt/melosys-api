package no.nav.melosys.saksflyt.steg.iv

import io.getunleash.FakeUnleash
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.eessi.BucInformasjon
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.saksflyt.steg.sed.SendVedtakUtland
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.brev.SedSomBrevService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.utpeking.UtpekingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class SendVedtakUtlandTest {
    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var eessiService: EessiService

    @MockK
    private lateinit var sedSomBrevService: SedSomBrevService

    @MockK
    private lateinit var utpekingService: UtpekingService

    @MockK
    private lateinit var prosessinstansService: ProsessinstansService

    private lateinit var sendVedtakUtland: SendVedtakUtland

    private lateinit var prosessinstans: Prosessinstans
    private lateinit var lovvalgsperiode: Lovvalgsperiode
    private lateinit var behandlingsresultat: Behandlingsresultat
    private lateinit var behandling: Behandling
    private lateinit var fagsak: Fagsak

    private val fakeUnleash = FakeUnleash()
    private val brevbestillingCaptor = slot<DoksysBrevbestilling>()

    @BeforeEach
    fun setUp() {
        behandling = Behandling.forTest {
            id = BEHANDLING_ID
            fagsak {
                gsakSaksnummer = 123456789L
            }
        }
        fagsak = behandling.fagsak

        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling {
                id = BEHANDLING_ID
                fagsak {
                    gsakSaksnummer = 123456789L
                }
            }
        }

        behandlingsresultat = lagBehandlingsresultat()
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultatMedAvklartefakta(any()) } returns behandlingsresultat

        every { eessiService.opprettOgSendSed(any(), any(), any(), any(), any()) } just Runs
        every { prosessinstansService.opprettProsessinstansSendBrev(any(), any(), any()) } just Runs
        every { utpekingService.oppdaterSendtUtland(any()) } just Runs
        every { eessiService.lukkBuc(any()) } just Runs
        every { eessiService.sendGodkjenningArbeidFlereLand(any(), any()) } just Runs

        sendVedtakUtland =
            SendVedtakUtland(eessiService, behandlingsresultatService, sedSomBrevService, utpekingService, prosessinstansService, fakeUnleash)
    }

    private fun lagBehandlingsresultat() = Behandlingsresultat().apply {
        id = BEHANDLING_ID
        lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            lovvalgsland = Land_iso2.NO
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        lovvalgsperioder = mutableSetOf(lovvalgsperiode)
        type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        behandling = this@SendVedtakUtlandTest.behandling
        vedtakMetadata = VedtakMetadata()
        avklartefakta = mutableSetOf(Avklartefakta())
    }

    @Test
    fun `utfør skal sende SED når artikkel 12 er suksessfull og status er oppdatert resultat`() {
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, listOf(MOTTAKER_INSTITUSJON))


        sendVedtakUtland.utfør(prosessinstans)


        verify { eessiService.opprettOgSendSed(any(), eq(listOf(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_04), eq(emptySet()), null) }
    }

    @Test
    fun `utfør skal sende brev når ingen institusjon og EESSI er klar`() {
        every { behandlingsresultatService.hentBehandlingsresultatMedAvklartefakta(BEHANDLING_ID) } returns lagBehandlingsresultat()


        sendVedtakUtland.utfør(prosessinstans)


        verify {
            prosessinstansService.opprettProsessinstansSendBrev(
                eq(behandling),
                capture(brevbestillingCaptor),
                eq(Mottaker.medRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET))
            )
        }
        brevbestillingCaptor.captured.produserbartdokument shouldBe Produserbaredokumenter.ATTEST_A1
    }

    @Test
    fun `utfør skal sende SED for artikkel 11 når suksessfull og status er oppdatert resultat`() {
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, listOf(MOTTAKER_INSTITUSJON))


        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B


        sendVedtakUtland.utfør(prosessinstans)


        verify { eessiService.opprettOgSendSed(any(), eq(listOf(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_05), eq(emptySet()), null) }
    }

    @Test
    fun `utfør skal hente mottakerinstitusjon fra tidligere BUC når uten oppgitt mottakerinstitusjon`() {
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, listOf(MOTTAKER_INSTITUSJON))
        behandling.fagsak = fagsak


        sendVedtakUtland.utfør(prosessinstans)


        verify { eessiService.opprettOgSendSed(any(), eq(listOf(MOTTAKER_INSTITUSJON)), any(), any(), null) }
    }

    @Test
    fun `utfør skal lage brev når utpekt annet land uten EESSI mottakere`() {
        behandling.tema = Behandlingstema.ARBEID_FLERE_LAND

        behandlingsresultat.apply {
            type = Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND
            utpekingsperioder.add(Utpekingsperiode())
            id = BEHANDLING_ID
        }
        lovvalgsperiode.apply {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2
            lovvalgsland = Land_iso2.AT
        }
        prosessinstans.id = UUID.randomUUID()
        prosessinstans.setData(ProsessDataKey.UTPEKT_LAND, Landkoder.AT)
        every {
            sedSomBrevService.lagJournalpostForSendingAvSedSomBrev(
                eq(SedType.A003),
                any(),
                any(),
                eq(prosessinstans.id.toString())
            )
        } returns "journalpostID"


        sendVedtakUtland.utfør(prosessinstans)


        verify {
            sedSomBrevService.lagJournalpostForSendingAvSedSomBrev(SedType.A003, Land_iso2.AT, behandling, prosessinstans.id.toString())
        }
    }

    @Test
    fun `utfør skal lukke BUC når vedtak etter artikkel 16 har tilknyttet LA BUC 01`() {
        val rinaSaksnummer = "5453"
        behandlingsresultat.hentLovvalgsperiode().bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
        val anmodningsperiode = Anmodningsperiode().apply {
            anmodningsperiodeSvar = AnmodningsperiodeSvar().apply {
                anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
            }
        }
        behandlingsresultat.anmodningsperioder.add(anmodningsperiode)

        every { eessiService.hentTilknyttedeBucer(fagsak.gsakSaksnummer!!, emptyList()) } returns listOf(
            BucInformasjon(rinaSaksnummer, true, BucType.LA_BUC_01.name, LocalDate.now(), emptySet(), emptyList())
        )


        sendVedtakUtland.utfør(prosessinstans)


        verify { eessiService.lukkBuc(rinaSaksnummer) }
    }

    @Test
    fun `utfør skal ikke sende A012 når Norge er utpekt og elektronisk BUC er åpen med standard behandlingsresultat`() {
        every { eessiService.hentTilknyttedeBucer(eq(fagsak.gsakSaksnummer!!), any()) } returns listOf(
            BucInformasjon("5453", true, BucType.LA_BUC_02.name, LocalDate.now(), emptySet(), emptyList())
        )
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE


        prosessinstans.setData(ProsessDataKey.YTTERLIGERE_INFO_SED, "Hei")


        sendVedtakUtland.utfør(prosessinstans)


        verify(exactly = 0) { eessiService.sendGodkjenningArbeidFlereLand(any(), any()) }
    }

    @Test
    fun `utfør skal ikke sende A012 når Norge er utpekt og elektronisk BUC er lukket`() {
        every { eessiService.hentTilknyttedeBucer(eq(fagsak.gsakSaksnummer!!), any()) } returns listOf(
            BucInformasjon("5453", false, BucType.LA_BUC_02.name, LocalDate.now(), emptySet(), emptyList())
        )
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE


        sendVedtakUtland.utfør(prosessinstans)


        verify(exactly = 0) { eessiService.sendGodkjenningArbeidFlereLand(any(), any()) }
    }

    companion object {
        private const val BEHANDLING_ID = 1L
        private const val MOTTAKER_INSTITUSJON = "SE:123"
    }
}
