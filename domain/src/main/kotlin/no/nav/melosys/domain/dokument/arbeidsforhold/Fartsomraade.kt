package no.nav.melosys.domain.dokument.arbeidsforhold

import no.nav.melosys.domain.kodeverk.Kodeverk


enum class Fartsomraade(private val kode: String, private val beskrivelse: String) : Kodeverk {
    INNENRIKS("innenriks", "Innenriks"),
    UTENRIKS("utenriks", "Utenriks");

    override fun getKode(): String = kode

    override fun getBeskrivelse(): String = beskrivelse
}
