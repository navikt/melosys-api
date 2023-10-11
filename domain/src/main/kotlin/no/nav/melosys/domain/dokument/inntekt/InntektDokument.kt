package no.nav.melosys.domain.dokument.inntekt

import no.nav.melosys.domain.dokument.SaksopplysningDokument
import javax.validation.constraints.NotNull

class InntektDokument(@JvmField var arbeidsInntektMaanedListe: @NotNull MutableList<ArbeidsInntektMaaned> = mutableListOf()) :
    SaksopplysningDokument {

    fun getArbeidsInntektMaanedListe(): List<ArbeidsInntektMaaned> = arbeidsInntektMaanedListe

    fun hentOrgnumre(): Set<String> {
        return arbeidsInntektMaanedListe
            .mapNotNull { it.arbeidsInntektInformasjon.inntektListe }
            .flatten()
            .mapNotNull { it.virksomhetID }
            .toSet()
    }
}
