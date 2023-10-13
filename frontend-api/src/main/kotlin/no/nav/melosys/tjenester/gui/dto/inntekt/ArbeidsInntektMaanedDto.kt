package no.nav.melosys.tjenester.gui.dto.inntekt

import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned
import java.time.YearMonth

class ArbeidsInntektMaanedDto(arbeidsInntektMaaned: ArbeidsInntektMaaned) {
    var aarMaaned: YearMonth? = arbeidsInntektMaaned.aarMaaned
    var arbeidsInntektInformasjon: ArbeidsInntektInformasjonDto = ArbeidsInntektInformasjonDto(arbeidsInntektMaaned.arbeidsInntektInformasjon)
}
