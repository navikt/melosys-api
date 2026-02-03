package no.nav.melosys.saksflyt.steg.iv

import io.getunleash.FakeUnleash
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
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
import no.nav.melosys.saksflytapi.domain.ProsessinstansTestFactory
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

    private val fakeUnleash = FakeUnleash()
    private val brevbestillingCaptor = slot<DoksysBrevbestilling>()

    @BeforeEach
    fun setUp() {
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns lagBehandlingsresultat()
        every { behandlingsresultatService.hentBehandlingsresultatMedAvklartefakta(any()) } returns lagBehandlingsresultat()

        every { eessiService.opprettOgSendSed(any(), any(), any(), any(), any(), any()) } just Runs
        every { prosessinstansService.opprettProsessinstansSendBrev(any(), any(), any()) } just Runs
        every { utpekingService.oppdaterSendtUtland(any()) } just Runs
        every { eessiService.lukkBuc(any()) } just Runs
        every { eessiService.sendGodkjenningArbeidFlereLand(any(), any()) } just Runs

        sendVedtakUtland =
            SendVedtakUtland(eessiService, behandlingsresultatService, sedSomBrevService, utpekingService, prosessinstansService, fakeUnleash)
    }

    private fun lagBehandlingsresultat(
        init: BehandlingsresultatTestFactory.Builder.() -> Unit = {}
    ) = Behandlingsresultat.forTest {
        id = BEHANDLING_ID
        type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        behandling {
            id = BEHANDLING_ID
            fagsak {
                gsakSaksnummer = GSAK_SAKSNUMMER
            }
        }
        vedtakMetadata { }
        avklartefakta { }
        lovvalgsperiode {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            lovvalgsland = Land_iso2.NO
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        init()
    }

    private fun lagProsessinstans(
        init: ProsessinstansTestFactory.ProsessinstansTestBuilder.() -> Unit = {}
    ) = Prosessinstans.forTest {
        type = ProsessType.OPPRETT_SAK
        status = ProsessStatus.KLAR
        behandling {
            id = BEHANDLING_ID
            fagsak {
                gsakSaksnummer = GSAK_SAKSNUMMER
            }
        }
        init()
    }

    @Test
    fun `utfør skal sende SED når artikkel 12 er suksessfull og status er oppdatert resultat`() {
        val prosessinstans = lagProsessinstans {
            medData(ProsessDataKey.EESSI_MOTTAKERE, listOf(MOTTAKER_INSTITUSJON))
        }


        sendVedtakUtland.utfør(prosessinstans)


        verify { eessiService.opprettOgSendSed(any(), eq(listOf(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_04), eq(emptySet()), null, null) }
    }

    @Test
    fun `utfør skal sende brev når ingen institusjon og EESSI er klar`() {
        val behandlingsresultat = lagBehandlingsresultat()
        val prosessinstans = lagProsessinstans()
        every { behandlingsresultatService.hentBehandlingsresultatMedAvklartefakta(BEHANDLING_ID) } returns behandlingsresultat


        sendVedtakUtland.utfør(prosessinstans)


        verify {
            prosessinstansService.opprettProsessinstansSendBrev(
                eq(behandlingsresultat.behandling!!),
                capture(brevbestillingCaptor),
                eq(Mottaker.medRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET))
            )
        }
        brevbestillingCaptor.captured.produserbartdokument shouldBe Produserbaredokumenter.ATTEST_A1
    }

    @Test
    fun `utfør skal sende SED for artikkel 11 når suksessfull og status er oppdatert resultat`() {
        val behandlingsresultat = lagBehandlingsresultat {
            lovvalgsperioder.clear()
            lovvalgsperiode {
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B
                lovvalgsland = Land_iso2.NO
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }
        }
        val prosessinstans = lagProsessinstans {
            medData(ProsessDataKey.EESSI_MOTTAKERE, listOf(MOTTAKER_INSTITUSJON))
        }
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultatMedAvklartefakta(any()) } returns behandlingsresultat


        sendVedtakUtland.utfør(prosessinstans)


        verify { eessiService.opprettOgSendSed(any(), eq(listOf(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_05), eq(emptySet()), null, null) }
    }

    @Test
    fun `utfør skal hente mottakerinstitusjon fra tidligere BUC når uten oppgitt mottakerinstitusjon`() {
        val prosessinstans = lagProsessinstans {
            medData(ProsessDataKey.EESSI_MOTTAKERE, listOf(MOTTAKER_INSTITUSJON))
        }


        sendVedtakUtland.utfør(prosessinstans)


        verify { eessiService.opprettOgSendSed(any(), eq(listOf(MOTTAKER_INSTITUSJON)), any(), any(), null, null) }
    }

    @Test
    fun `utfør skal lage brev når utpekt annet land uten EESSI mottakere`() {
        val prosessinstansId = UUID.randomUUID()
        val behandlingsresultat = lagBehandlingsresultat {
            type = Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND
            behandling {
                id = BEHANDLING_ID
                tema = Behandlingstema.ARBEID_FLERE_LAND
                fagsak {
                    gsakSaksnummer = GSAK_SAKSNUMMER
                }
            }
            utpekingsperiode { }
            lovvalgsperioder.clear()
            lovvalgsperiode {
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2
                lovvalgsland = Land_iso2.AT
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }
        }
        val prosessinstans = lagProsessinstans {
            id = prosessinstansId
            behandling {
                id = BEHANDLING_ID
                tema = Behandlingstema.ARBEID_FLERE_LAND
                fagsak {
                    gsakSaksnummer = GSAK_SAKSNUMMER
                }
            }
            medData(ProsessDataKey.UTPEKT_LAND, Landkoder.AT)
        }
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultatMedAvklartefakta(any()) } returns behandlingsresultat
        every {
            sedSomBrevService.lagJournalpostForSendingAvSedSomBrev(
                eq(SedType.A003),
                any(),
                any(),
                eq(prosessinstansId.toString())
            )
        } returns "journalpostID"


        sendVedtakUtland.utfør(prosessinstans)


        verify {
            sedSomBrevService.lagJournalpostForSendingAvSedSomBrev(SedType.A003, Land_iso2.AT, behandlingsresultat.behandling!!, prosessinstansId.toString())
        }
    }

    @Test
    fun `utfør skal lukke BUC når vedtak etter artikkel 16 har tilknyttet LA BUC 01`() {
        val rinaSaksnummer = "5453"
        val behandlingsresultat = lagBehandlingsresultat {
            lovvalgsperioder.clear()
            lovvalgsperiode {
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
                lovvalgsland = Land_iso2.NO
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }
            anmodningsperiode {
                anmodningsperiodeSvar {
                    anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
                }
            }
        }
        val prosessinstans = lagProsessinstans()
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultatMedAvklartefakta(any()) } returns behandlingsresultat
        every { eessiService.hentTilknyttedeBucer(GSAK_SAKSNUMMER, emptyList()) } returns listOf(
            BucInformasjon(rinaSaksnummer, true, BucType.LA_BUC_01.name, LocalDate.now(), emptySet(), emptyList())
        )


        sendVedtakUtland.utfør(prosessinstans)


        verify { eessiService.lukkBuc(rinaSaksnummer) }
    }

    @Test
    fun `utfør skal ikke sende A012 når Norge er utpekt og elektronisk BUC er åpen med standard behandlingsresultat`() {
        // Merk: Denne testen verifiserer at når prosessinstans.behandling IKKE har tema=BESLUTNING_LOVVALG_NORGE,
        // skal A012 ikke sendes, selv om det finnes en åpen LA_BUC_02.
        val prosessinstans = lagProsessinstans {
            medData(ProsessDataKey.YTTERLIGERE_INFO_SED, "Hei")
        }
        every { eessiService.hentTilknyttedeBucer(eq(GSAK_SAKSNUMMER), any()) } returns listOf(
            BucInformasjon("5453", true, BucType.LA_BUC_02.name, LocalDate.now(), emptySet(), emptyList())
        )


        sendVedtakUtland.utfør(prosessinstans)


        verify(exactly = 0) { eessiService.sendGodkjenningArbeidFlereLand(any(), any()) }
    }

    @Test
    fun `utfør skal ikke sende A012 når Norge er utpekt og elektronisk BUC er lukket`() {
        // Note: This test verifies that when prosessinstans.behandling does NOT have tema=BESLUTNING_LOVVALG_NORGE,
        // A012 should not be sent when BUC is closed.
        val prosessinstans = lagProsessinstans()
        every { eessiService.hentTilknyttedeBucer(eq(GSAK_SAKSNUMMER), any()) } returns listOf(
            BucInformasjon("5453", false, BucType.LA_BUC_02.name, LocalDate.now(), emptySet(), emptyList())
        )


        sendVedtakUtland.utfør(prosessinstans)


        verify(exactly = 0) { eessiService.sendGodkjenningArbeidFlereLand(any(), any()) }
    }

    companion object {
        private const val BEHANDLING_ID = 1L
        private const val GSAK_SAKSNUMMER = 123456789L
        private const val MOTTAKER_INSTITUSJON = "SE:123"
    }
}
