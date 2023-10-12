package no.nav.melosys.domain.dokument.inntekt

import no.nav.melosys.domain.dokument.SaksopplysningDokument

class InntektDokument(var arbeidsInntektMaanedListe: List<ArbeidsInntektMaaned> = emptyList()) : SaksopplysningDokument {

    fun hentOrgnumre(): Set<String> {
        return arbeidsInntektMaanedListe
            .map { it.arbeidsInntektInformasjon.inntektListe }
            .flatten()
            .mapNotNull { it.virksomhetID }
            .toSet()
    }
}
