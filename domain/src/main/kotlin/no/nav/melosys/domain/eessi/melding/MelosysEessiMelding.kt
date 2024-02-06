package no.nav.melosys.domain.eessi.melding

import no.nav.melosys.domain.eessi.Periode
import no.nav.melosys.domain.eessi.SvarAnmodningUnntak

data class MelosysEessiMelding(
    val sedId: String,
    val sequenceId: Int?,
    val rinaSaksnummer: String?,
    val avsender: Avsender?,
    val journalpostId: String?,
    val dokumentId: String?,
    val gsakSaksnummer: Long?,
    val aktoerId: String?,
    val statsborgerskap: List<Statsborgerskap>?,
    val arbeidssteder: List<Arbeidssted>?,
    val periode: Periode?,
    val lovvalgsland: String?,
    val artikkel: String?,
    val erEndring: Boolean,
    val midlertidigBestemmelse: Boolean,
    val x006NavErFjernet: Boolean,
    val ytterligereInformasjon: String?,
    val bucType: String?,
    val sedType: String?,
    val sedVersjon: String?,
    val svarAnmodningUnntak: SvarAnmodningUnntak?,
    val anmodningUnntak: AnmodningUnntak?
) {
    fun inneholderYtterligereInformasjon(): Boolean {
        return !ytterligereInformasjon.isNullOrBlank()
    }

    fun lagUnikIdentifikator(): String {
        return "${rinaSaksnummer}_${sedId}_${sedVersjon}"
    }
}
