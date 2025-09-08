package no.nav.melosys.integrasjon.joark.journalpostapi.dto

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.arkiv.BrukerIdType
import no.nav.melosys.domain.arkiv.FysiskDokument
import no.nav.melosys.domain.arkiv.OpprettJournalpost
import org.junit.jupiter.api.Test

class OpprettJournalpostRequestTest {

    @Test
    fun `av med gyldig OpprettJournalpost forvent objekt`() {
        val hoveddokument = FysiskDokument().apply {
            tittel = "tittel"
            brevkode = "brevkode"
            dokumentKategori = "kategori"
            dokumentVarianter = listOf(no.nav.melosys.domain.arkiv.DokumentVariant.lagDokumentVariant("pdf".toByteArray()))
        }

        val opprettJournalpost = OpprettJournalpost().apply {
            setHoveddokument(hoveddokument)
            mottaksKanal = "S"
            tema = "MED"
            brukerId = "12345678901"
            brukerIdType = BrukerIdType.FOLKEREGISTERIDENT
            korrespondansepartNavn = "Trygdemyndighet"
            korrespondansepartId = "id123"
            setKorrespondansepartIdType(OpprettJournalpost.KorrespondansepartIdType.UTENLANDSK_ORGANISASJON)
            saksnummer = "MEL-1231"
            innhold = "Tittel som beskriver innholdet"
            journalførendeEnhet = "MEDLEMSKAP_OG_AVGIFT"
            journalposttype = no.nav.melosys.domain.arkiv.Journalposttype.UT
        }

        val request = OpprettJournalpostRequest.av(opprettJournalpost)

        request.shouldNotBeNull().run {
            tittel shouldBe "Tittel som beskriver innholdet"
            dokumenter shouldHaveSize 1
            kanal shouldBe "S"
        }
    }
}
