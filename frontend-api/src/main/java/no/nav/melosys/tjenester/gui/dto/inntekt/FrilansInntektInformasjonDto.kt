package no.nav.melosys.tjenester.gui.dto.inntekt;

import java.util.List;

import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsforholdFrilanser;

public class FrilansInntektInformasjonDto {
    public List<ArbeidsforholdFrilanser> arbeidsforholdFrilanserListe;

    public FrilansInntektInformasjonDto(ArbeidsInntektInformasjon arbeidsInntektInformasjon) {
        arbeidsforholdFrilanserListe = arbeidsInntektInformasjon.getMutableArbeidsforholdListe();
    }
}
