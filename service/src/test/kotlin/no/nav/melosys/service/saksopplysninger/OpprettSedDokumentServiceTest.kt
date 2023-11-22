package no.nav.melosys.service.saksopplysninger

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.dokument.DokumentFactory
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.Periode
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.AnmodningUnntak
import no.nav.melosys.domain.eessi.melding.Avsender
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.eessi.melding.Statsborgerskap
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.repository.SaksopplysningRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class OpprettSedDokumentServiceTest {
    @Mock
    private val dokumentFactory: DokumentFactory? = null

    @Mock
    private val saksopplysningRepository: SaksopplysningRepository? = null
    private var opprettSedDokumentService: OpprettSedDokumentService? = null
    @BeforeEach
    fun setup() {
        opprettSedDokumentService = OpprettSedDokumentService(dokumentFactory!!, saksopplysningRepository!!)
    }

    @Test
    fun opprettSedSaksopplysning() {
        val sedDokument = SedDokument()
        sedDokument.lovvalgBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
        sedDokument.bucType = BucType.LA_BUC_04
        sedDokument.sedType = SedType.A009
        val behandling = Behandling()
        opprettSedDokumentService!!.opprettSedSaksopplysning(hentMelosysEessiMelding(), behandling)
        Mockito.verify(dokumentFactory)!!.lagForenkletXml(
            ArgumentMatchers.any(
                Saksopplysning::class.java
            )
        )
        Mockito.verify(saksopplysningRepository)!!.save(
            ArgumentMatchers.any(
                Saksopplysning::class.java
            )
        )
    }

    private fun hentMelosysEessiMelding(): MelosysEessiMelding {
        val melding = MelosysEessiMelding()
        melding.aktoerId = "123"
        melding.artikkel = "12_1"
        melding.avsender = Avsender("GB:aopjfsa", "GB")
        melding.dokumentId = "123321"
        melding.gsakSaksnummer = 432432L
        melding.journalpostId = "j123"
        melding.lovvalgsland = "SE"
        val periode = Periode()
        periode.fom = LocalDate.of(2012, 12, 12)
        periode.tom = LocalDate.of(2012, 12, 13)
        melding.periode = periode
        val statsborgerskap = Statsborgerskap()
        statsborgerskap.landkode = "SE"
        melding.rinaSaksnummer = "r123"
        melding.sedId = "s123"
        melding.statsborgerskap =
            listOf(statsborgerskap)
        melding.sedType = "A009"
        melding.bucType = "LA_BUC_04"
        val anmodningUnntak = AnmodningUnntak()
        anmodningUnntak.unntakFraLovvalgsland = "NO"
        anmodningUnntak.unntakFraLovvalgsbestemmelse = "16_1"
        melding.anmodningUnntak = anmodningUnntak
        return melding
    }
}
