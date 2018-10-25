package no.nav.melosys.tjenester.gui.dto.inntekt;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;

public class ArbeidsInntektInformasjonDto {

    public List<Inntekt> inntektListe = new ArrayList<>();

    public ArbeidsInntektInformasjonDto(ArbeidsInntektInformasjon arbeidsInntektInformasjon) {
        inntektListe = arbeidsInntektInformasjon.inntektListe;
    }
}
