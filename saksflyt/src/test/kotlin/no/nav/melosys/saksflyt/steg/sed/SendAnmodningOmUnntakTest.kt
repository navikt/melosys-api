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
import no.nav.melosys.domain.arkiv.Vedlegg
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

    private lateinit var prosessinstans: Prosessinstans
    private val brevbestillingSlot = slot<DoksysBrevbestilling>()

    @BeforeEach
    fun setUp() {
        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling {
                id = BEHANDLING_ID
                fagsak {
                    gsakSaksnummer = 123456789L
                }
                dokumentasjonSvarfristDato = Instant.now()
            }
        }

        sendAnmodningOmUnntak = SendAnmodningOmUnntak(
            eessiService, brevBestiller, behandlingService,
            behandlingsresultatService, anmodningsperiodeService
        )

        every { behandlingService.lagre(any()) } returns mockk()
        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns mockk()
        every { anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(any()) } returns Unit
        every { eessiService.opprettOgSendSed(any<Long>(), any(), any(), any(), any()) } returns Unit
        every { eessiService.lagEessiVedlegg(any(), any()) } returns emptySet()
        every { brevBestiller.bestill(any()) } returns Unit
    }

    @Test
    fun `utfør artikkel16 skal sende sed med vedlegg`() {
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, listOf(MOTTAKER_INSTITSJON))
        prosessinstans.setData(ProsessDataKey.VEDLEGG_SED, setOf(DokumentReferanse("", "")))
        val behandlingsresultat = hentBehandlingsresultat()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        val forventetVedlegg = Vedlegg(byteArrayOf(), "tittel")
        every { eessiService.lagEessiVedlegg(any(), any()) } returns setOf(forventetVedlegg)


        sendAnmodningOmUnntak.utfør(prosessinstans)


        verify {
            eessiService.opprettOgSendSed(
                BEHANDLING_ID,
                listOf(MOTTAKER_INSTITSJON),
                BucType.LA_BUC_01,
                match { it.contains(forventetVedlegg) },
                null
            )
        }
        verify { anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(BEHANDLING_ID) }
    }

    @Test
    fun `utfør artikkel18_1 skal sende sed`() {
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, listOf(MOTTAKER_INSTITSJON))
        val behandlingsresultat = hentBehandlingsresultat()
        behandlingsresultat.anmodningsperioder.forEach { periode ->
            periode.bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        sendAnmodningOmUnntak.utfør(prosessinstans)


        verify {
            eessiService.opprettOgSendSed(
                BEHANDLING_ID,
                listOf(MOTTAKER_INSTITSJON),
                BucType.LA_BUC_01,
                any(),
                null
            )
        }
        verify { anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(BEHANDLING_ID) }
    }

    @Test
    fun `utfør ingen institusjon eessi klar skal sende brev`() {
        val behandlingsresultat = hentBehandlingsresultat()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        prosessinstans.setData(ProsessDataKey.YTTERLIGERE_INFO_SED, "Mer info")


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
        val behandlingsresultat = hentBehandlingsresultat()
        behandlingsresultat.anmodningsperioder = mutableSetOf(Anmodningsperiode())
        every { behandlingsresultatService.hentBehandlingsresultat(2L) } returns behandlingsresultat
        prosessinstans.hentBehandling.id = 2L
        val nå = prosessinstans.hentBehandling.dokumentasjonSvarfristDato
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, listOf(MOTTAKER_INSTITSJON))
        prosessinstans.setData(ProsessDataKey.YTTERLIGERE_INFO_SED, "fritekst")


        sendAnmodningOmUnntak.utfør(prosessinstans)


        prosessinstans.hentBehandling.dokumentasjonSvarfristDato!!.isAfter(nå!!) shouldBe true
        verify(exactly = 0) {
            eessiService.opprettOgSendSed(any<Long>(), any(), BucType.LA_BUC_01, null, "fritekst")
        }
        verify { anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(prosessinstans.hentBehandling.id) }
    }

    private fun hentBehandlingsresultat() = Behandlingsresultat().apply {
        id = BEHANDLING_ID
        behandling = Behandling.forTest {
            id = BEHANDLING_ID
            fagsak {
                gsakSaksnummer = 123456789L
            }
            dokumentasjonSvarfristDato = Instant.now()
        }
        anmodningsperioder = mutableSetOf(
            Anmodningsperiode(
                LocalDate.now(), LocalDate.now(), Land_iso2.NO,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2, Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5,
                Land_iso2.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, Trygdedekninger.FULL_DEKNING_EOSFO
            )
        )
        type = Behandlingsresultattyper.ANMODNING_OM_UNNTAK
    }

    companion object {
        private const val BEHANDLING_ID = 1L
        private const val MOTTAKER_INSTITSJON = "SE:123"
    }
}
