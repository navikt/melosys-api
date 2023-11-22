package no.nav.melosys.service.saksopplysninger

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.dokument.DokumentFactory
import no.nav.melosys.domain.eessi.Periode
import no.nav.melosys.domain.eessi.melding.AnmodningUnntak
import no.nav.melosys.domain.eessi.melding.Avsender
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.eessi.melding.Statsborgerskap
import no.nav.melosys.repository.SaksopplysningRepository
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpprettSedDokumentServiceTest {

    private val dokumentFactory: DokumentFactory = mockk()
    private val saksopplysningRepository: SaksopplysningRepository = mockk()

    private val opprettSedDokumentService = OpprettSedDokumentService(dokumentFactory, saksopplysningRepository)

    @Test
    fun opprettSedSaksopplysning() {
        val behandling = Behandling()
        val melosysEessiMelding = hentMelosysEessiMelding()
        every { dokumentFactory.lagForenkletXml(any()) } returns "<xml>"
        every { saksopplysningRepository.save(any()) } returns mockk()


        opprettSedDokumentService.opprettSedSaksopplysning(melosysEessiMelding, behandling)


        verify { dokumentFactory.lagForenkletXml(any()) }
        verify { saksopplysningRepository.save(any()) }
    }

    private fun hentMelosysEessiMelding(): MelosysEessiMelding {
        return MelosysEessiMelding().apply {
            aktoerId = "123"
            artikkel = "12_1"
            avsender = Avsender("GB:aopjfsa", "GB")
            dokumentId = "123321"
            gsakSaksnummer = 432432L
            journalpostId = "j123"
            lovvalgsland = "SE"
            periode = Periode(LocalDate.of(2012, 12, 12), LocalDate.of(2012, 12, 13))
            statsborgerskap = listOf(Statsborgerskap().apply { landkode = "SE" })
            rinaSaksnummer = "r123"
            sedId = "s123"
            sedType = "A009"
            bucType = "LA_BUC_04"
            anmodningUnntak = AnmodningUnntak().apply {
                unntakFraLovvalgsland = "NO"
                unntakFraLovvalgsbestemmelse = "16_1"
            }
        }
    }
}
