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
    fun melosysEessiMelding(block: MelosysEessiMelding.() -> Unit): MelosysEessiMelding = MelosysEessiMelding().apply {
        aktoerId = "1111111111111"
        anmodningUnntak = null
        arbeidssteder = emptyList()
        gsakSaksnummer = null
        avsender = Avsender("SE:123", "SE")
        dokumentId = null
        statsborgerskap = emptyList()
        sedVersjon = "1"
        lovvalgsland = "SE"
        isX006NavErFjernet = false
        block()
        if (sedId == null) sedId = sedType
        if (journalpostId == null) journalpostId = opprettEessiJournalpost(sedType)
    }

    fun melosysEessiMelding(
        bucType: BucType,
        rinaSaksnummer: String?,
        sedType: SedType,
        periode: Periode?,
        artikkel: String?,
        lovvalgsland: String = "SE",
        isX006NavErFjernet: Boolean = false,
    ): MelosysEessiMelding = MelosysEessiMelding().apply {
        this.aktoerId = "1111111111111"
        this.anmodningUnntak = null
        this.arbeidssteder = emptyList()
        this.bucType = bucType.name
        this.gsakSaksnummer = null
        this.artikkel = artikkel
        this.avsender = Avsender("SE:123", "SE")
        this.dokumentId = null
        this.journalpostId = opprettEessiJournalpost(sedType.name)
        this.lovvalgsland = lovvalgsland
        this.periode = periode
        this.sedType = sedType.name
        this.sedId = sedType.name
        this.rinaSaksnummer = rinaSaksnummer
        this.statsborgerskap = emptyList()
        this.sedVersjon = "1"
        this.isX006NavErFjernet = isX006NavErFjernet
    }

    fun opprettEessiJournalpost(sedType: String): String {
        val request = OpprettJournalpost().apply {
            setHoveddokument(FysiskDokument().apply {
                dokumentKategori = "SED"
                tittel = "$sedType-tittel"
                brevkode = sedType
                dokumentVarianter = listOf(
                    DokumentVariant.lagDokumentVariant(
                        ByteArray(0)
                    )
                )
            })
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
