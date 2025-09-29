package no.nav.melosys.itest

import no.nav.melosys.domain.arkiv.*
import no.nav.melosys.domain.eessi.melding.Avsender
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.integrasjon.joark.JoarkFasade
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class EessiMeldingTestDataFactory(
    @Autowired private val joarkFasade: JoarkFasade,
) {
    fun melosysEessiMelding(block: MelosysEessiMelding.() -> Unit): MelosysEessiMelding = MelosysEessiMelding().apply {
        aktoerId = "1111111111111"
        anmodningUnntak = null
        arbeidssteder = emptyList()
        arbeidsland = emptyList()
        gsakSaksnummer = null
        avsender = Avsender("SE:123", "SE")
        dokumentId = null
        statsborgerskap = emptyList()
        sedVersjon = "1"
        lovvalgsland = "SE"
        x006NavErFjernet = false
        block()
        if (sedId == null) sedId = sedType
        if (journalpostId == null) journalpostId = opprettEessiJournalpost(hentSedType())
    }

    fun opprettEessiJournalpost(sedType: String): String {
        val request = OpprettJournalpost().apply {
            hoveddokument = FysiskDokument().apply {
                dokumentKategori = "SED"
                tittel = "$sedType-tittel"
                brevkode = sedType
                dokumentVarianter = listOf(
                    DokumentVariant.lagDokumentVariant(
                        ByteArray(0)
                    )
                )
            }
            brukerId = "123123123"
            brukerIdType = BrukerIdType.FOLKEREGISTERIDENT
            journalposttype = Journalposttype.INN
            journalførendeEnhet = "4530"
            tema = "UFM"
            korrespondansepartId = "SE:123"
            korrespondansepartNavn = "Sverige"
            korrespondansepartLand = "SE"
            setKorrespondansepartIdType(OpprettJournalpost.KorrespondansepartIdType.UTENLANDSK_ORGANISASJON)
            mottaksKanal = "EESSI"
            journalposttype = Journalposttype.INN
            innhold = "$sedType-tittel"
        }
        return joarkFasade.opprettJournalpost(request, false)
    }
}
