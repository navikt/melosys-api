package no.nav.melosys.domain.eessi.melding

import no.nav.melosys.domain.eessi.Periode
import no.nav.melosys.domain.eessi.SvarAnmodningUnntak
import org.apache.commons.lang3.StringUtils

/**
 * Melding fra melosys-esssi om en SED
 */
data class MelosysEessiMelding(
    var sedId: String? = null,
    var sequenceId: Int? = null,
    var rinaSaksnummer: String? = null,
    var avsender: Avsender? = null,
    var journalpostId: String? = null,
    var dokumentId: String? = null,
    var gsakSaksnummer: Long? = null,
    var aktoerId: String? = null,
    var statsborgerskap: List<Statsborgerskap>? = null,
    var arbeidssteder: List<Arbeidssted>? = null,
    var arbeidsland: List<Arbeidsland>? = null,
    var periode: Periode? = null,
    var lovvalgsland: String? = null,
    var artikkel: String? = null,
    var erEndring: Boolean = false,
    var midlertidigBestemmelse: Boolean = false,
    var x006NavErFjernet: Boolean = false,
    var ytterligereInformasjon: String? = null,
    var bucType: String? = null,
    var sedType: String? = null,
    var sedVersjon: String? = null,
    var svarAnmodningUnntak: SvarAnmodningUnntak? = null,
    var anmodningUnntak: AnmodningUnntak? = null
) {
    fun inneholderYtterligereInformasjon(): Boolean =
        StringUtils.isNotEmpty(ytterligereInformasjon)

    fun lagUnikIdentifikator(): String =
        "${hentRinaSaksnummer()}_${hentSedId()}_${hentSedVersjon()}"

    fun hentRinaSaksnummer() = rinaSaksnummer ?: error("rinaSaksnummer er påkrevd for ${this::class.simpleName}")

    fun hentSedId() = sedId ?: error("sedId er påkrevd for ${this::class.simpleName}")

    fun hentSedVersjon() = sedVersjon ?: error("sedVersjon er påkrevd for ${this::class.simpleName}")

    fun hentJournalpostId() = journalpostId ?: error("journalpostId er påkrevd for ${this::class.simpleName}")

    fun hentAktoerId() = aktoerId ?: error("aktoerId er påkrevd for ${this::class.simpleName}")

    fun hentAvsenderLandkode() = avsender?.landkode ?: error("avsender.landkode er påkrevd for ${this::class.simpleName}")

    fun hentLovvalgsland() = lovvalgsland ?: error("lovvalgsland er påkrevd for ${this::class.simpleName}")

    fun hentPeriode() = periode ?: error("periode er påkrevd for ${this::class.simpleName}")

    fun hentSedType() = sedType ?: error("sedType er påkrevd for ${this::class.simpleName}")

    fun hentBucType() = bucType ?: error("bucType er påkrevd for ${this::class.simpleName}")

    fun hentStatsborgerskap() = statsborgerskap ?: error("statsborgerskap er påkrevd for ${this::class.simpleName}")
}
