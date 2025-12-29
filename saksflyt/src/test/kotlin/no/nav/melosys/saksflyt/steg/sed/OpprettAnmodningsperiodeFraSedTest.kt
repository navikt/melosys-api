package no.nav.melosys.saksflyt.steg.sed

import java.time.LocalDate

import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.unntak.AnmodningsperiodeService
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
    fun `utfor skal lagre anmodningsperiode med full dekning naar lovvalgsland er Norge`() {
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusYears(1)
        val prosessinstans = lagProsessinstans(
            lovvalgslandKode = Landkoder.NO,
            unntakFraLovvalgslandKode = Landkoder.SE,
            fom = fom,
            tom = tom
        )

        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns prosessinstans.behandling
        every { anmodningsperiodeService.lagreAnmodningsperioder(any(), capture(anmodningsperiodeSlot)) } returns emptyList()


        opprettAnmodningsperiodeFraSed.utfør(prosessinstans)


        verify { anmodningsperiodeService.lagreAnmodningsperioder(1L, any()) }
        val lagredePerioder = anmodningsperiodeSlot.captured
        lagredePerioder shouldContain Anmodningsperiode(
            fom,
            tom,
            Land_iso2.NO,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
            null,
            Land_iso2.SE,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
            Trygdedekninger.FULL_DEKNING_EOSFO
        )
    }

    @Test
    fun `utfor skal lagre anmodningsperiode uten dekning naar lovvalgsland ikke er Norge`() {
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusYears(1)
        val prosessinstans = lagProsessinstans(
            lovvalgslandKode = Landkoder.DE,
            unntakFraLovvalgslandKode = Landkoder.NO,
            fom = fom,
            tom = tom
        )

        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns prosessinstans.behandling
        every { anmodningsperiodeService.lagreAnmodningsperioder(any(), capture(anmodningsperiodeSlot)) } returns emptyList()


        opprettAnmodningsperiodeFraSed.utfør(prosessinstans)


        verify { anmodningsperiodeService.lagreAnmodningsperioder(1L, any()) }
        val lagredePerioder = anmodningsperiodeSlot.captured
        lagredePerioder shouldContain Anmodningsperiode(
            fom,
            tom,
            Land_iso2.DE,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
            null,
            Land_iso2.NO,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
            Trygdedekninger.UTEN_DEKNING
        )
    }

    private fun lagProsessinstans(
        lovvalgslandKode: Landkoder,
        unntakFraLovvalgslandKode: Landkoder,
        fom: LocalDate,
        tom: LocalDate
    ) = Prosessinstans.forTest {
        behandling {
            id = 1L
            saksopplysning {
                type = SaksopplysningType.SEDOPPL
                sedDokument {
                    this@sedDokument.lovvalgsperiode(fom, tom)
                    this@sedDokument.lovvalgBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
                    this@sedDokument.lovvalgslandKode = lovvalgslandKode
                    this@sedDokument.unntakFraLovvalgBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
                    this@sedDokument.unntakFraLovvalgslandKode = unntakFraLovvalgslandKode
                }
            }
        }
    }
}
