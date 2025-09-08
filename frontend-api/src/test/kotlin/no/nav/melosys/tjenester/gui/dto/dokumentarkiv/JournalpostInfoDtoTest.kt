package no.nav.melosys.tjenester.gui.dto.dokumentarkiv

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.melosys.domain.arkiv.ArkivDokument
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.arkiv.Journalposttype
import no.nav.melosys.domain.kodeverk.Mottaksretning
import org.junit.jupiter.api.Test
import java.time.Instant

class JournalpostInfoDtoTest {

    @Test
    fun `skal konvertere journalpost til journalpostInfoDto`() {
        val journalpostID = "journalpostID"
        val hovedTittel = "tittel"
        val partNavn = "part navn"
        val vedleggsTittel = "tittelV"
        val journalpost = Journalpost(journalpostID).apply {
            journalposttype = Journalposttype.INN
            val nå = Instant.now()
            forsendelseMottatt = nå
            forsendelseJournalfoert = nå
            hoveddokument = ArkivDokument().apply {
                tittel = hovedTittel
                dokumentId = "1"
            }
            korrespondansepartNavn = partNavn
            vedleggListe.add(ArkivDokument().apply {
                tittel = vedleggsTittel
                dokumentId = "2"
            })
        }


        val dto = JournalpostInfoDto.av(journalpost)


        dto.run {
            this.journalpostID shouldBe journalpostID
            mottaksretning shouldBe Mottaksretning.INN
            mottattDato shouldNotBe null
            journalforingDato shouldNotBe null
            avsenderEllerMottaker shouldBe partNavn
            hoveddokument.run {
                dokumentID shouldBe "1"
                tittel shouldBe hovedTittel
            }
            vedlegg[0].run {
                dokumentID shouldBe "2"
                tittel shouldBe vedleggsTittel
            }
        }
    }
}