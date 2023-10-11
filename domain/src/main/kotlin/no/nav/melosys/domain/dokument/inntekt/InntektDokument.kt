package no.nav.melosys.domain.dokument.inntekt

import no.nav.melosys.domain.dokument.SaksopplysningDokument
import javax.validation.constraints.NotNull

class InntektDokument(var arbeidsInntektMaanedListe: MutableList<ArbeidsInntektMaaned> = mutableListOf()) :
    SaksopplysningDokument {

    fun hentOrgnumre(): Set<String> {
        return arbeidsInntektMaanedListe
            .mapNotNull { it.arbeidsInntektInformasjon.inntektListe }
            .flatten()
            .mapNotNull { it.virksomhetID }
            .toSet()
    }
}
