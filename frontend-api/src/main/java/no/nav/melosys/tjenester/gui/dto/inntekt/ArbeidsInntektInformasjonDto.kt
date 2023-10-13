package no.nav.melosys.tjenester.gui.dto.inntekt

import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon
import no.nav.melosys.domain.dokument.inntekt.Inntekt

class ArbeidsInntektInformasjonDto(arbeidsInntektInformasjon: ArbeidsInntektInformasjon) {
    var inntektListe: List<Inntekt>

    init {
        inntektListe = arbeidsInntektInformasjon.getMutableInntektListe()
    }
}
