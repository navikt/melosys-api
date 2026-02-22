package no.nav.melosys.saksflyt.steg.sed

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.arkiv.DokumentReferanse
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.saksflyt.brev.BrevBestiller
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.saksflytapi.domain.ProsessinstansTestFactory
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class SendAnmodningOmUnntakTest {

    private val behandlingService: BehandlingService = mockk()
    private val behandlingsresultatService: BehandlingsresultatService = mockk()
    private val eessiService: EessiService = mockk()
    private val brevBestiller: BrevBestiller = mockk()
    private val anmodningsperiodeService: AnmodningsperiodeService = mockk()

    private lateinit var sendAnmodningOmUnntak: SendAnmodningOmUnntak

    private val brevbestillingSlot = slot<DoksysBrevbestilling>()

    @BeforeEach
    fun setUp() {
        sendAnmodningOmUnntak = SendAnmodningOmUnntak(
            eessiService, brevBestiller, behandlingService,
            behandlingsresultatService, anmodningsperiodeService
        )

        every { behandlingService.lagre(any()) } returns mockk()
        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns mockk()
        every { anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(any()) } returns Unit
        every { eessiService.opprettOgSendSed(any<Long>(), any(), any(), any(), any(), any(), any()) } returns Unit
        every { brevBestiller.bestill(any()) } returns Unit
    }

    @Test
    fun `utfør artikkel16 skal sende sed med vedlegg`() {
        val dokumentReferanser = setOf(DokumentReferanse("", ""))
        val prosessinstans = lagProsessinstans {
            medData(ProsessDataKey.EESSI_MOTTAKERE, listOf(MOTTAKER_INSTITSJON))
            medData(ProsessDataKey.VEDLEGG_SED, dokumentReferanser)
        }
        val behandlingsresultat = lagBehandlingsresultat()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        sendAnmodningOmUnntak.utfør(prosessinstans)

        verify {
            eessiService.opprettOgSendSed(
                BEHANDLING_ID,
                listOf(MOTTAKER_INSTITSJON),
                BucType.LA_BUC_01,
                dokumentReferanser,
                null,
                null,
                null
            )
        }
        verify { anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(BEHANDLING_ID) }
    }

    @Test
    fun `utfør artikkel18_1 skal sende sed`() {
        val prosessinstans = lagProsessinstans {
            medData(ProsessDataKey.EESSI_MOTTAKERE, listOf(MOTTAKER_INSTITSJON))
        }
        val behandlingsresultat = lagBehandlingsresultatMedAnmodningsperiode {
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        sendAnmodningOmUnntak.utfør(prosessinstans)


        verify {
            eessiService.opprettOgSendSed(
                BEHANDLING_ID,
                listOf(MOTTAKER_INSTITSJON),
                BucType.LA_BUC_01,
                any(),
                null,
                null,
                null
            )
        }
        verify { anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(BEHANDLING_ID) }
    }

    @Test
    fun `utfør ingen institusjon eessi klar skal sende brev`() {
        val prosessinstans = lagProsessinstans {
            medData(ProsessDataKey.YTTERLIGERE_INFO_SED, "Mer info")
        }
        val behandlingsresultat = lagBehandlingsresultat()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        sendAnmodningOmUnntak.utfør(prosessinstans)


        verify { brevBestiller.bestill(capture(brevbestillingSlot)) }
        brevbestillingSlot.captured.run {
            mottakere!! shouldContain Mottaker.medRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET)
            produserbartdokument shouldBe Produserbaredokumenter.ANMODNING_UNNTAK
            ytterligereInformasjon shouldBe "Mer info"
        }
        verify { anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(BEHANDLING_ID) }
    }

    @Test
    fun `utfør ingen bestemmelse skal verifisere sed ikke sendt`() {
        val behandlingId = 2L
        val prosessinstans = lagProsessinstans(behandlingId) {
            medData(ProsessDataKey.EESSI_MOTTAKERE, listOf(MOTTAKER_INSTITSJON))
            medData(ProsessDataKey.YTTERLIGERE_INFO_SED, "fritekst")
        }
        val nå = prosessinstans.hentBehandling.dokumentasjonSvarfristDato

        val behandlingsresultat = Behandlingsresultat.forTest {
            id = behandlingId
            behandling {
                id = behandlingId
                fagsak { gsakSaksnummer = 123456789L }
                dokumentasjonSvarfristDato = Instant.now()
            }
            anmodningsperiode {
                bestemmelse = null
            }
            type = Behandlingsresultattyper.ANMODNING_OM_UNNTAK
        }
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat


        sendAnmodningOmUnntak.utfør(prosessinstans)


        prosessinstans.hentBehandling.dokumentasjonSvarfristDato!!.isAfter(nå!!) shouldBe true
        verify(exactly = 0) {
            eessiService.opprettOgSendSed(any<Long>(), any(), BucType.LA_BUC_01, null, "fritekst", any(), any())
        }
        verify { anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(prosessinstans.hentBehandling.id) }
    }

    private fun lagProsessinstans(
        behandlingId: Long = BEHANDLING_ID,
        init: ProsessinstansTestFactory.ProsessinstansTestBuilder.() -> Unit = {}
    ) = Prosessinstans.forTest {
        type = ProsessType.OPPRETT_SAK
        status = ProsessStatus.KLAR
        behandling {
            id = behandlingId
            fagsak {
                gsakSaksnummer = 123456789L
            }
            dokumentasjonSvarfristDato = Instant.now()
        }
        init()
    }

    private fun lagBehandlingsresultat() = lagBehandlingsresultatMedAnmodningsperiode()

    private fun lagBehandlingsresultatMedAnmodningsperiode(
        anmodningsperiodeInit: AnmodningsperiodeTestFactory.Builder.() -> Unit = {}
    ) = Behandlingsresultat.forTest {
        id = BEHANDLING_ID
        behandling {
            id = BEHANDLING_ID
            fagsak {
                gsakSaksnummer = 123456789L
            }
            dokumentasjonSvarfristDato = Instant.now()
        }
        anmodningsperiode {
            fom = LocalDate.now()
            tom = LocalDate.now()
            lovvalgsland = Land_iso2.NO
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2
            tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5
            unntakFraLovvalgsland = Land_iso2.NO
            unntakFraBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            dekning = Trygdedekninger.FULL_DEKNING_EOSFO
            anmodningsperiodeInit()
        }
        type = Behandlingsresultattyper.ANMODNING_OM_UNNTAK
    }

    companion object {
        private const val BEHANDLING_ID = 1L
        private const val MOTTAKER_INSTITSJON = "SE:123"
    }
}
