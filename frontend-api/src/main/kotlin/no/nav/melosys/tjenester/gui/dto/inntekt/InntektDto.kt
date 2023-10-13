package no.nav.melosys.tjenester.gui.dto.inntekt

import no.nav.melosys.domain.dokument.inntekt.InntektDokument

class InntektDto {
    var arbeidsInntektMaanedListe: MutableList<ArbeidsInntektMaanedDto> = ArrayList()
    var frilansInntektMaanedListe: MutableList<FrilansInntektMaanedDto> = ArrayList()

    constructor()
    constructor(inntektDokument: InntektDokument?) {
        if (inntektDokument != null && !inntektDokument.arbeidsInntektMaanedListe.isEmpty()) {
            for (arbeidsInntektMaaned in inntektDokument.arbeidsInntektMaanedListe) {
                if (arbeidsInntektMaaned.arbeidsInntektInformasjon != null) {
                    if (!arbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe.isEmpty()) {
                        arbeidsInntektMaanedListe.add(ArbeidsInntektMaanedDto(arbeidsInntektMaaned))
                    }
                    if (!arbeidsInntektMaaned.arbeidsInntektInformasjon.arbeidsforholdListe!!.isEmpty()) {
                        frilansInntektMaanedListe.add(FrilansInntektMaanedDto(arbeidsInntektMaaned))
                    }
                }
            }
        }
    }
}
