package no.nav.melosys.tjenester.gui.dto.inntekt;

import java.time.YearMonth;

import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;

public class FrilansInntektMaanedDto {

    public YearMonth aarMaaned;

    public FrilansInntektInformasjonDto frilansInntektInformasjon;

    public FrilansInntektMaanedDto(ArbeidsInntektMaaned arbeidsInntektMaaned) {
        aarMaaned = arbeidsInntektMaaned.aarMaaned;
        frilansInntektInformasjon = new FrilansInntektInformasjonDto(arbeidsInntektMaaned.arbeidsInntektInformasjon);
    }
}
