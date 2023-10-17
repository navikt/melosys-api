package no.nav.melosys.tjenester.gui.dto.inntekt

import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon
import no.nav.melosys.domain.dokument.inntekt.ArbeidsforholdFrilanser

class FrilansInntektInformasjonDto(arbeidsInntektInformasjon: ArbeidsInntektInformasjon) {
    var arbeidsforholdFrilanserListe: List<ArbeidsforholdFrilanser>? = arbeidsInntektInformasjon.arbeidsforholdListe
}
