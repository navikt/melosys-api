package no.nav.melosys.service.eessi.jobb

import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.saksflyt.ProsessDataKey
import no.nav.melosys.domain.saksflyt.Prosessinstans
import no.nav.melosys.repository.ProsessinstansRepository
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.journalforing.JournalfoeringService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class JournalfoerX100JournalposterJobbTest {
    @Mock
    private val prosessinstansRepository: ProsessinstansRepository? = null

    @Mock
    private val journalfoeringService: JournalfoeringService? = null

    private lateinit var journalfoerX100JournalposterJobb: JournalfoerX100JournalposterJobb

    @BeforeEach
    fun setup() {
        journalfoerX100JournalposterJobb = JournalfoerX100JournalposterJobb(
            prosessinstansRepository!!, journalfoeringService!!
        )
    }

    @Test
    fun journalfoerX100Journalposter() {
        val prosessMedX100 = lagProsessinstansMedEessiMelding()
        Mockito.`when`(prosessinstansRepository!!.findAllWithSedX100()).thenReturn(setOf(prosessMedX100))
        Mockito.`when`(journalfoeringService!!.hentJournalpost(JOURNALPOST_ID_X_100)).thenReturn(lagJournalpost())

        journalfoerX100JournalposterJobb.journalfoerX100Journalposter()

        Mockito.verify(journalfoeringService)!!.journalførOgKnyttTilEksisterendeSak(anyList())
    }

    companion object {
        private const val JOURNALPOST_ID_X_100 = "journalpostID-X100"

        private fun lagProsessinstansMedEessiMelding(): Prosessinstans {
            val prosessMedX100 = Prosessinstans()
            prosessMedX100.setData(ProsessDataKey.JOURNALPOST_ID, JOURNALPOST_ID_X_100)
            prosessMedX100.setData(ProsessDataKey.EESSI_MELDING, lagEessiMeldingMedX100())
            prosessMedX100.behandling = SaksbehandlingDataFactory.lagBehandling(
                SaksbehandlingDataFactory.lagFagsak("MEL-1")
            )
            return prosessMedX100
        }

        private fun lagEessiMeldingMedX100(): String {
            return """
            {"sedId"\\:"sedId","rinaSaksnummer"\\:"rinaSaksnummer",
            "avsender"\\:{"avsenderID"\\:"AT\\:3100","landkode"\\:"AT"},"journalpostId"\\:"jpID","dokumentId"\\:null,
            "gsakSaksnummer"\\:gsakSaksnummer,"aktoerId"\\:"aktoerId","statsborgerskap"\\:[],"arbeidssteder"\\:[],
            "periode"\\:null,"lovvalgsland"\\:null,"artikkel"\\:null,"erEndring"\\:false,"midlertidigBestemmelse"\\:false,
            "x006NavErFjernet"\\:false,"ytterligereInformasjon"\\:null,"bucType"\\:"LA_BUC_02","sedType"\\:"X100",
            "sedVersjon"\\:"2","svarAnmodningUnntak"\\:null,"anmodningUnntak"\\:null}
            """
        }

        private fun lagJournalpost() = Journalpost(JOURNALPOST_ID_X_100).apply { isErFerdigstilt = false }
    }
}
