package no.nav.melosys.tjenester.gui.dto.inntekt;

import java.util.List;

import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;

public class ArbeidsInntektInformasjonDto {
    public List<Inntekt> inntektListe;

    public ArbeidsInntektInformasjonDto(ArbeidsInntektInformasjon arbeidsInntektInformasjon) {
        inntektListe = arbeidsInntektInformasjon.getMutableInntektListe();
    }
}
