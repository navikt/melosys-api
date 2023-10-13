package no.nav.melosys.tjenester.gui.dto.inntekt;

import java.time.YearMonth;

import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;

public class ArbeidsInntektMaanedDto {

    public YearMonth aarMaaned;

    public ArbeidsInntektInformasjonDto arbeidsInntektInformasjon;

    public ArbeidsInntektMaanedDto(ArbeidsInntektMaaned arbeidsInntektMaaned) {
        aarMaaned = arbeidsInntektMaaned.aarMaaned;
        arbeidsInntektInformasjon = new ArbeidsInntektInformasjonDto(arbeidsInntektMaaned.arbeidsInntektInformasjon);
    }
}
