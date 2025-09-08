package no.nav.melosys.saksflyt.steg.sed

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.eessi.Periode
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.eessi.melding.Statsborgerskap
import no.nav.melosys.domain.forTest
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.saksopplysninger.OpprettSedDokumentService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OpprettSedDokumentTest {

    @MockK
    private lateinit var opprettSedDokumentService: OpprettSedDokumentService

    private lateinit var opprettSedDokument: OpprettSedDokument

    @BeforeEach
    fun setup() {
        opprettSedDokument = OpprettSedDokument(opprettSedDokumentService)
    }

    @Test
    fun utfoerSteg() {
        every { opprettSedDokumentService.opprettSedSaksopplysning(any<MelosysEessiMelding>(), any<Behandling>()) } returns mockk()

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding())
        }
        prosessinstans.behandling = Behandling.forTest { }


        opprettSedDokument.utfør(prosessinstans)


        verify { opprettSedDokumentService.opprettSedSaksopplysning(any<MelosysEessiMelding>(), any<Behandling>()) }
    }

    private fun hentMelosysEessiMelding() = MelosysEessiMelding().apply {
        aktoerId = "123"
        artikkel = "12_1"
        dokumentId = "123321"
        gsakSaksnummer = 432432L
        journalpostId = "j123"
        lovvalgsland = "SE"
        periode = Periode().apply {
            fom = LocalDate.of(2012, 12, 12)
            tom = LocalDate.of(2012, 12, 13)
        }
        rinaSaksnummer = "r123"
        sedId = "s123"
        statsborgerskap = listOf(Statsborgerskap("SE"))
        sedType = "A009"
        bucType = "LA_BUC_04"
    }
}
