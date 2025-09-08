package no.nav.melosys.saksflyt.steg.sed

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.arkiv.ArkivDokument
import no.nav.melosys.domain.arkiv.DokumentReferanse
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.arkiv.Vedlegg
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.brev.SedSomBrevService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
internal class VideresendSoknadTest {

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK(relaxUnitFun = true)
    private lateinit var eessiService: EessiService

    @MockK
    private lateinit var joarkFasade: JoarkFasade

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var sedSomBrevService: SedSomBrevService

    private lateinit var videresendSoknad: VideresendSoknad
    private lateinit var behandling: Behandling
    private val journalpost = Journalpost("123")

    @BeforeEach
    fun setup() {
        videresendSoknad = VideresendSoknad(
            eessiService, behandlingsresultatService,
            joarkFasade, fagsakService, sedSomBrevService
        )

        behandling = Behandling.forTest {
            id = 1L
            initierendeJournalpostId = "123"
        }
        journalpost.hoveddokument = ArkivDokument().apply {
            tittel = "tittel på deg"
            dokumentId = "44444"
        }
    }

    @Test
    fun `utfør når vedlegg finnes ikke skal kaste FunksjonellException`() {
        val prosessinstans = opprettProsessinstans().apply {
            hentBehandling.initierendeJournalpostId = null
        }


        val exception = shouldThrow<FunksjonellException> {
            videresendSoknad.utfør(prosessinstans)
        }


        exception.message shouldContain "Kan ikke videresende søknad uten vedlegg"
    }

    @Test
    fun `utfør når skal sendes utland er eessi klar skal sende sed i buc3`() {
        var prosessinstans = opprettProsessinstans().toBuilder()
            .medData(ProsessDataKey.EESSI_MOTTAKERE, listOf("SE:123"))
            .build()

        val behandling = prosessinstans.behandling!!
        val behandlingID = 1L
        val behandlingsresultat = Behandlingsresultat().apply {
            id = behandlingID
            this.behandling = behandling
        }

        val vedlegg = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val dokumentReferanse = DokumentReferanse(
            behandling.initierendeJournalpostId!!,
            journalpost.hoveddokument.dokumentId!!
        )
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.VEDLEGG_SED, setOf(dokumentReferanse))
            .build()
        val forventetVedlegg = Vedlegg(vedlegg, "tittel")

        every { eessiService.lagEessiVedlegg(any(), any()) } returns setOf(forventetVedlegg)
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat


        videresendSoknad.utfør(prosessinstans)


        verify {
            eessiService.opprettOgSendSed(
                behandlingID, listOf(MOTTAKER_INSTITUSJON), BucType.LA_BUC_03,
                match { collection -> collection.contains(forventetVedlegg) }, null
            )
        }
        prosessinstans.getData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID).shouldBeNull()
    }

    @Test
    fun `utfør når skal sendes utland er ikke eessi klar skal sende a008 som brev`() {
        val prosessinstansUuid = UUID.randomUUID()
        var prosessinstans = opprettProsessinstans().toBuilder()
            .medId(prosessinstansUuid)
            .build()
        val behandling = prosessinstans.hentBehandling
        val opprettetJournalpostID = "532523"

        val vedlegg = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        prosessinstans = prosessinstans.toBuilder()
            .medData(
                ProsessDataKey.VEDLEGG_SED,
                setOf(DokumentReferanse(behandling.initierendeJournalpostId!!, journalpost.hoveddokument.dokumentId!!))
            )
            .build()

        val behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            this.behandling = behandling
        }

        every { joarkFasade.hentJournalpost(behandling.initierendeJournalpostId!!) } returns journalpost
        every { joarkFasade.hentDokument(behandling.initierendeJournalpostId!!, journalpost.hoveddokument.dokumentId) } returns vedlegg
        every { eessiService.lagEessiVedlegg(any(), any()) } returns emptySet()
        every {
            sedSomBrevService.lagJournalpostForSendingAvSedSomBrev(
                any(),
                any(),
                any(),
                any(),
                prosessinstansUuid.toString()
            )
        } returns opprettetJournalpostID
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        every { fagsakService.hentFagsak(any()) } returns lagFagsak()


        videresendSoknad.utfør(prosessinstans)


        verify {
            sedSomBrevService.lagJournalpostForSendingAvSedSomBrev(
                SedType.A008,
                any<Land_iso2>(),
                behandling,
                any(),
                prosessinstansUuid.toString()
            )
        }
        prosessinstans.getData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID) shouldBe opprettetJournalpostID
        prosessinstans.getData(ProsessDataKey.DISTRIBUER_MOTTAKER_LAND, Landkoder::class.java) shouldBe Landkoder.SE
    }

    private fun opprettProsessinstans() = Prosessinstans.forTest {
        behandling {
            id = 1L
            initierendeJournalpostId = "123"
            fagsak = lagFagsak()
        }
    }

    companion object {
        private const val MOTTAKER_INSTITUSJON = "SE:123"

        private fun lagFagsak(): Fagsak = Fagsak.forTest {
            medGsakSaksnummer()
            medTrygdemyndighet()
            medBruker()
        }
    }
}
