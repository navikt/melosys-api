package no.nav.melosys.service.saksopplysninger

import io.getunleash.FakeUnleash
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningKildesystem
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.Periode
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.AnmodningUnntak
import no.nav.melosys.domain.eessi.melding.Avsender
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.eessi.melding.Statsborgerskap
import no.nav.melosys.repository.SaksopplysningRepository
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpprettSedDokumentServiceTest {

    private val saksopplysningRepository: SaksopplysningRepository = mockk()
    private val fakeUnleash = FakeUnleash()
    private val opprettSedDokumentService = OpprettSedDokumentService(saksopplysningRepository, fakeUnleash)

    @Test
    fun opprettSedSaksopplysning() {
        val saksopplysningSlot = slot<Saksopplysning>()
        val behandling = Behandling()
        val melosysEessiMelding = hentMelosysEessiMelding()
        every { saksopplysningRepository.save(any<Saksopplysning>()) } returns mockk()


        opprettSedDokumentService.opprettSedSaksopplysning(melosysEessiMelding, behandling)


        verify { saksopplysningRepository.save(capture(saksopplysningSlot)) }
        saksopplysningSlot.captured.let { saksopplysning ->
            saksopplysning.type.shouldBe(SaksopplysningType.SEDOPPL)
            saksopplysning.dokument.shouldBeInstanceOf<SedDokument>().run {
                sedType.shouldBe(SedType.A009)
                rinaDokumentID.shouldBe(melosysEessiMelding.sedId)
                bucType.shouldBe(BucType.LA_BUC_04)
            }
            saksopplysning.kilder.single().run {
                kilde.shouldBe(SaksopplysningKildesystem.EESSI)
                mottattDokument.shouldEqualJson(SED_DOKUMENT_KILDE_SOM_JSON)
            }
        }
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
            statsborgerskap = listOf(Statsborgerskap("SE"))
            rinaSaksnummer = "r123"
            sedId = "s123"
            sedType = "A009"
            bucType = "LA_BUC_04"
            anmodningUnntak = AnmodningUnntak("NO", "16_1")
        }
    }

    companion object {
        const val SED_DOKUMENT_KILDE_SOM_JSON = """
            {
              "rinaSaksnummer": "r123",
              "rinaDokumentID": "s123",
              "avsenderLandkode": "GB",
              "fnr": null,
              "lovvalgsperiode": {
                "fom": [
                  2012,
                  12,
                  12
                ],
                "tom": [
                  2012,
                  12,
                  13
                ]
              },
              "lovvalgBestemmelse": "FO_883_2004_ART12_1",
              "lovvalgslandKode": "SE",
              "unntakFraLovvalgBestemmelse": "FO_883_2004_ART16_1",
              "unntakFraLovvalgslandKode": "NO",
              "erEndring": false,
              "sedType": "A009",
              "bucType": "LA_BUC_04",
              "statsborgerskapKoder": [
                "SE"
              ],
              "arbeidssteder": null,
              "arbeidsland": null
            }
        """
    }
}
