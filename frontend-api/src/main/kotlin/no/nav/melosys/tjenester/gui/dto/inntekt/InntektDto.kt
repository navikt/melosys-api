package no.nav.melosys.tjenester.gui.dto.inntekt

import no.nav.melosys.domain.dokument.inntekt.InntektDokument

class InntektDto(inntektDokument: InntektDokument? = null) {
    var arbeidsInntektMaanedListe: List<ArbeidsInntektMaanedDto> = inntektDokument?.arbeidsInntektMaanedListe
        ?.map { ArbeidsInntektMaanedDto(it) } ?: emptyList()

    var frilansInntektMaanedListe: List<FrilansInntektMaanedDto> = inntektDokument?.arbeidsInntektMaanedListe
        ?.map { arbeidsInntektMaaned ->
            arbeidsInntektMaaned.arbeidsInntektInformasjon.arbeidsforholdListe?.map {
                FrilansInntektMaanedDto(arbeidsInntektMaaned)
            } ?: emptyList()
        }?.flatten() ?: emptyList()
}
