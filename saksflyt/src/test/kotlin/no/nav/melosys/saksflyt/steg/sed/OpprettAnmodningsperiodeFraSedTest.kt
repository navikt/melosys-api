package no.nav.melosys.saksflyt.steg.sed

import java.time.LocalDate
import java.util.Collections

import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import no.nav.melosys.domain.forTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.kotest.matchers.collections.shouldContain
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest

@ExtendWith(MockKExtension::class)
class OpprettAnmodningsperiodeFraSedTest {
    @MockK
    private lateinit var anmodningsperiodeService: AnmodningsperiodeService

    @MockK
    private lateinit var behandlingService: BehandlingService

    private lateinit var opprettAnmodningsperiodeFraSed: OpprettAnmodningsperiodeFraSed

    private val anmodningsperiodeSlot = slot<Collection<Anmodningsperiode>>()

    @BeforeEach
    fun setup() {
        opprettAnmodningsperiodeFraSed = OpprettAnmodningsperiodeFraSed(
            anmodningsperiodeService,
            behandlingService
        )
    }

    @Test
    fun `utfør skal lagre anmodningsperiode med full dekning når lovvalgsland er Norge`() {
        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = lagSedDokument(Landkoder.NO)
        }
        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = 1L
                saksopplysninger = mutableSetOf(saksopplysning)
            }
        }

        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns prosessinstans.behandling
        every { anmodningsperiodeService.lagreAnmodningsperioder(any(), capture(anmodningsperiodeSlot)) } returns emptyList()


        opprettAnmodningsperiodeFraSed.utfør(prosessinstans)


        verify { anmodningsperiodeService.lagreAnmodningsperioder(1L, any()) }
        val lagredePerioder = anmodningsperiodeSlot.captured
        lagredePerioder shouldContain lagForventetAnmodningsperiode(saksopplysning.dokument as SedDokument, Trygdedekninger.FULL_DEKNING_EOSFO)
    }

    @Test
    fun `utfør skal lagre anmodningsperiode uten dekning når lovvalgsland ikke er Norge`() {
        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = lagSedDokument(Landkoder.DE)
        }
        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = 1L
                saksopplysninger = mutableSetOf(saksopplysning)
            }
        }

        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns prosessinstans.behandling
        every { anmodningsperiodeService.lagreAnmodningsperioder(any(), capture(anmodningsperiodeSlot)) } returns emptyList()


        opprettAnmodningsperiodeFraSed.utfør(prosessinstans)


        verify { anmodningsperiodeService.lagreAnmodningsperioder(1L, any()) }
        val lagredePerioder = anmodningsperiodeSlot.captured
        lagredePerioder shouldContain lagForventetAnmodningsperiode(saksopplysning.dokument as SedDokument, Trygdedekninger.UTEN_DEKNING)
    }

    private fun lagSedDokument(lovvalgslandKode: Landkoder) = SedDokument().apply {
        lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
        lovvalgBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
        this.lovvalgslandKode = lovvalgslandKode
        unntakFraLovvalgBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
        unntakFraLovvalgslandKode = if (lovvalgslandKode == Landkoder.NO) Landkoder.SE else Landkoder.NO
    }

    private fun lagForventetAnmodningsperiode(
        sedDokument: SedDokument,
        trygdedekning: Trygdedekninger
    ) = Anmodningsperiode(
        sedDokument.hentLovvalgsperiode().fom,
        sedDokument.hentLovvalgsperiode().tom,
        Land_iso2.valueOf(sedDokument.lovvalgslandKode!!.name),
        sedDokument.lovvalgBestemmelse,
        null,
        Land_iso2.valueOf(sedDokument.unntakFraLovvalgslandKode!!.name),
        sedDokument.unntakFraLovvalgBestemmelse,
        trygdedekning
    )
}
