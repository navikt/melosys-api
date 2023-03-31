package no.nav.melosys.itest

import no.nav.melosys.domain.arkiv.*
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.Periode
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.Avsender
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.integrasjon.joark.JoarkFasade
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class EessiMeldingTestDataFactory(
    @Autowired private val joarkFasade: JoarkFasade,
) {
    fun melosysEessiMelding(
        bucType: BucType,
        rinaSaksnummer: String?,
        sedType: SedType,
        periode: Periode?,
        artikkel: String?,
        lovvalgsland: String = "SE",
        isX006NavErFjernet: Boolean = false,
    ): MelosysEessiMelding {
        return melosysEessiMelding(
            bucType = bucType,
            rinaSaksnummer = rinaSaksnummer,
            sedType = sedType,
            periode = periode,
            artikkel = artikkel,
            journalpostID = opprettEessiJournalpost(sedType),
            lovvalgsland = lovvalgsland,
            isX006NavErFjernet = isX006NavErFjernet
        )
    }

    fun melosysEessiMelding(
        bucType: BucType,
        rinaSaksnummer: String?,
        sedType: SedType,
        periode: Periode?,
        artikkel: String?,
        journalpostID: String,
        lovvalgsland: String = "SE",
        isX006NavErFjernet: Boolean = false,
    ): MelosysEessiMelding {
        val eessiMelding = MelosysEessiMelding()
        eessiMelding.aktoerId = "1111111111111"
        eessiMelding.anmodningUnntak = null
        eessiMelding.arbeidssteder = emptyList()
        eessiMelding.bucType = bucType.name
        eessiMelding.gsakSaksnummer = null
        eessiMelding.artikkel = artikkel
        eessiMelding.avsender = Avsender("SE:123", "SE")
        eessiMelding.dokumentId = null
        eessiMelding.journalpostId = journalpostID
        eessiMelding.lovvalgsland = lovvalgsland
        eessiMelding.periode = periode
        eessiMelding.sedType = sedType.name
        eessiMelding.sedId = sedType.name
        eessiMelding.rinaSaksnummer = rinaSaksnummer
        eessiMelding.statsborgerskap = emptyList()
        eessiMelding.sedVersjon = "1"
        eessiMelding.isX006NavErFjernet = isX006NavErFjernet
        return eessiMelding
    }

    fun opprettEessiJournalpost(sedType: SedType): String {
        val request = OpprettJournalpost()
        val hovedDokument = FysiskDokument()
        hovedDokument.dokumentKategori = "SED"
        hovedDokument.tittel = "$sedType-tittel"
        hovedDokument.brevkode = sedType.name
        hovedDokument.dokumentVarianter = listOf(
            DokumentVariant.lagDokumentVariant(
                ByteArray(0)
            )
        )
        request.setHoveddokument(hovedDokument)
        request.brukerId = "123123123"
        request.brukerIdType = BrukerIdType.FOLKEREGISTERIDENT
        request.journalposttype = Journalposttype.INN
        request.journalførendeEnhet = "4530"
        request.tema = "UFM"
        request.korrespondansepartId = "SE:123"
        request.korrespondansepartNavn = "Sverige"
        request.korrespondansepartLand = "SE"
        request.setKorrespondansepartIdType(OpprettJournalpost.KorrespondansepartIdType.UTENLANDSK_ORGANISASJON)
        request.mottaksKanal = "EESSI"
        request.journalposttype = Journalposttype.INN
        request.innhold = "$sedType-tittel"
        return joarkFasade.opprettJournalpost(request, false)
    }
}
